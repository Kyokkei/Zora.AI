package com.yozora.aichat.data.remote

import com.yozora.aichat.ui.chat.ApiVendor
import com.yozora.aichat.ui.chat.PersonaUiState
import com.yozora.aichat.ui.chat.normalizeStoredModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class DoroAutoModeTest {

    @Test
    fun fallbackModelsUseTheExactFreeTierOrder() {
        assertEquals(
            listOf(
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite",
                "gemini-3-flash"
            ),
            DORO_AUTO_FALLBACK_MODELS
        )
    }

    @Test
    fun googleDefaultsToDoroAutoMode() {
        assertEquals(DORO_AUTO_MODEL_ID, ApiVendor.Google.defaultModel)
    }

    @Test
    fun newPersonaDefaultsToDoroAutoMode() {
        assertEquals(DORO_AUTO_MODEL_ID, PersonaUiState().model)
    }

    @Test
    fun googleModelOptionsStartWithDoroAndContainEachFallbackOnce() {
        val options = ApiVendor.Google.modelOptions

        assertEquals(listOf(DORO_AUTO_MODEL_ID) + DORO_AUTO_FALLBACK_MODELS, options)
        assertEquals(options.size, options.toSet().size)
    }

    @Test
    fun concreteModelInvokesTheRequestOnce() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()

        val result = router.run("gemini-3.5-flash", "key-one") { model ->
            attempts += model
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf("gemini-3.5-flash"), attempts)
    }

    @Test
    fun doroStopsAfterTheFirstSuccessfulCandidate() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()

        val result = router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            attempts += model
            "first success"
        }

        assertEquals("first success", result)
        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun doroAdvancesAcrossTyped429sUntilSuccess() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()

        val result = router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            attempts += model
            if (model in DORO_AUTO_FALLBACK_MODELS.take(2)) throw generic429()
            "flash lite success"
        }

        assertEquals("flash lite success", result)
        assertEquals(DORO_AUTO_FALLBACK_MODELS.take(3), attempts)
    }

    @Test
    fun non429ClientFailureIsRethrownWithoutDowngrading() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()
        val expected = httpError(status = 400, message = "Bad request")

        val actual = assertFailsWithSuspend<ApiHttpException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
                attempts += model
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun typed503IsRethrownToTheOuterRetryLayerWithoutDowngrading() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()
        val expected = httpError(status = 503, message = "Service unavailable")

        val actual = assertFailsWithSuspend<ApiHttpException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
                attempts += model
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun fiveTyped429sExhaustTheChainWithoutRepeatingACandidate() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()

        val exhausted = assertFailsWithSuspend<DoroAutoExhaustedException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
                attempts += model
                throw generic429()
            }
        }

        assertTrue(exhausted.message.orEmpty().contains("HTTP 429"))
        assertTrue(exhausted.message.orEmpty().contains("quota", ignoreCase = true))
        assertEquals(DORO_AUTO_FALLBACK_MODELS, attempts)
        assertEquals(attempts.size, attempts.toSet().size)
    }

    @Test
    fun cancellationPropagatesWithoutTryingAnotherCandidate() = runBlocking {
        val router = router()
        val attempts = mutableListOf<String>()
        val cancellation = CancellationException("cancelled")

        val actual = assertFailsWithSuspend<CancellationException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
                attempts += model
                throw cancellation
            }
        }

        assertSame(cancellation, actual)
        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun dailyQuotaClassifierRecognizesRpdMarkersButRejectsGeneric429s() {
        assertTrue(
            isDailyQuotaExceeded(
                httpError(
                    status = 429,
                    responseBody = """{"error":{"details":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}}"""
                )
            )
        )
        assertTrue(
            isDailyQuotaExceeded(
                httpError(
                    status = 429,
                    message = "Exceeded quota metric: REQUESTS_PER_DAY"
                )
            )
        )
        assertTrue(isDailyQuotaExceeded(httpError(status = 429, message = "quota bucket RPD reached")))
        assertFalse(isDailyQuotaExceeded(httpError(status = 429, message = "Quota exceeded")))
        assertFalse(
            isDailyQuotaExceeded(
                httpError(status = 429, responseBody = """{"quotaMetric":"RequestsPerMinute"}""")
            )
        )
    }

    @Test
    fun confirmedDailyQuotaSkipsThatModelForTheSameKeyAndDay() = runBlocking {
        val router = router()
        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            if (model == "gemini-3.6-flash") throw daily429()
            "first request"
        }

        val nextAttempts = mutableListOf<String>()
        val result = router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            nextAttempts += model
            "second request"
        }

        assertEquals("second request", result)
        assertEquals(listOf("gemini-3.5-flash"), nextAttempts)
    }

    @Test
    fun generic429IsRetriedLocallyButProbedAgainOnTheNextRequest() = runBlocking {
        val router = router()
        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            if (model == "gemini-3.6-flash") throw generic429()
            "first request"
        }

        val nextAttempts = mutableListOf<String>()
        val result = router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            nextAttempts += model
            "second request"
        }

        assertEquals("second request", result)
        assertEquals(listOf("gemini-3.6-flash"), nextAttempts)
    }

    @Test
    fun aDifferentApiKeyDoesNotInheritDailyExhaustion() = runBlocking {
        val router = router()
        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            if (model == "gemini-3.6-flash") throw daily429()
            "first request"
        }

        val attempts = mutableListOf<String>()
        router.run(DORO_AUTO_MODEL_ID, "key-two") { model ->
            attempts += model
            "different key"
        }

        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun aNewPacificDayClearsEffectiveDailyExhaustion() = runBlocking {
        var day = "2026-07-22"
        val router = DoroAutoRouter(dayProvider = { day })
        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            if (model == "gemini-3.6-flash") throw daily429()
            "first request"
        }

        day = "2026-07-23"
        val attempts = mutableListOf<String>()
        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            attempts += model
            "new day"
        }

        assertEquals(listOf("gemini-3.6-flash"), attempts)
    }

    @Test
    fun concurrentDailyExhaustionUpdatesRetainEveryModel() = runBlocking {
        val router = router()
        val startingGate = CountDownLatch(DORO_AUTO_FALLBACK_MODELS.size)
        val executor = Executors.newFixedThreadPool(DORO_AUTO_FALLBACK_MODELS.size)
        val dispatcher = executor.asCoroutineDispatcher()

        try {
            coroutineScope {
                DORO_AUTO_FALLBACK_MODELS.mapIndexed { targetIndex, _ ->
                    async(dispatcher) {
                        runCatching {
                            router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
                                val candidateIndex = DORO_AUTO_FALLBACK_MODELS.indexOf(model)
                                if (candidateIndex == 0) {
                                    startingGate.countDown()
                                    startingGate.await()
                                }
                                if (candidateIndex <= targetIndex) throw daily429()
                                throw httpError(status = 503, message = "stop after target")
                            }
                        }
                    }
                }.awaitAll()
            }
        } finally {
            dispatcher.close()
            executor.shutdownNow()
        }

        var networkInvoked = false
        assertFailsWithSuspend<DoroAutoExhaustedException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") {
                networkInvoked = true
                "unexpected"
            }
        }
        assertFalse(networkInvoked)
    }

    @Test
    fun allConfirmedDailyExhaustionFailsFastWithoutANetworkCall() = runBlocking {
        val router = router()
        assertFailsWithSuspend<DoroAutoExhaustedException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") { throw daily429() }
        }

        var networkInvoked = false
        assertFailsWithSuspend<DoroAutoExhaustedException> {
            router.run(DORO_AUTO_MODEL_ID, "key-one") {
                networkInvoked = true
                "unexpected"
            }
        }
        assertFalse(networkInvoked)
    }

    @Test
    fun doroCallbackAlwaysReceivesAConcreteModel() = runBlocking {
        val router = router()
        var resolvedModel = ""

        router.run(DORO_AUTO_MODEL_ID, "key-one") { model ->
            resolvedModel = model
            "ok"
        }

        assertTrue(resolvedModel in DORO_AUTO_FALLBACK_MODELS)
        assertFalse(resolvedModel == DORO_AUTO_MODEL_ID)
    }

    @Test
    fun selectedDoroValueDoesNotChangeAfterALowerCandidateSucceeds() = runBlocking {
        val router = router()
        val selectedModel = DORO_AUTO_MODEL_ID

        val result = router.run(selectedModel, "key-one") { model ->
            if (model == "gemini-3.6-flash") throw generic429()
            "lower candidate"
        }

        assertEquals("lower candidate", result)
        assertEquals(DORO_AUTO_MODEL_ID, selectedModel)
    }

    @Test
    fun restoredGoogleProNormalizesToDoro() {
        assertEquals(
            DORO_AUTO_MODEL_ID,
            normalizeStoredModel(ApiVendor.Google, "gemini-3.1-pro")
        )
    }

    @Test
    fun restoredSupportedConcreteGoogleModelIsUnchanged() {
        assertEquals(
            "gemini-3.5-flash",
            normalizeStoredModel(ApiVendor.Google, "gemini-3.5-flash")
        )
    }

    @Test
    fun blankStoredModelUsesThatVendorsDefault() {
        assertEquals(ApiVendor.GPT.defaultModel, normalizeStoredModel(ApiVendor.GPT, "   "))
    }

    @Test
    fun targetedProMigrationDoesNotChangeANonGoogleModel() {
        assertEquals(
            "gemini-3.1-pro",
            normalizeStoredModel(ApiVendor.GPT, "gemini-3.1-pro")
        )
    }

    private fun router(): DoroAutoRouter = DoroAutoRouter(dayProvider = { "2026-07-22" })

    private fun generic429(): ApiHttpException = httpError(
        status = 429,
        message = "Resource exhausted"
    )

    private fun daily429(): ApiHttpException = httpError(
        status = 429,
        responseBody = """{"error":{"details":[{"quotaMetric":"GenerateContentRequestsPerDay"}]}}"""
    )

    private fun httpError(
        status: Int,
        message: String = "",
        responseBody: String = ""
    ): ApiHttpException = ApiHttpException(status, message, responseBody)

    private suspend inline fun <reified T : Throwable> assertFailsWithSuspend(
        crossinline block: suspend () -> Unit
    ): T {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return error
            throw error
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }
}
