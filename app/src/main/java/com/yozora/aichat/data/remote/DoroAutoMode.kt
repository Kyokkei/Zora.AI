package com.yozora.aichat.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DORO_AUTO_MODEL_ID = "doro-auto"
const val DORO_AUTO_MODEL_LABEL = "DoroAutoMode"

val DORO_AUTO_FALLBACK_MODELS = listOf(
    "gemini-3.6-flash",
    "gemini-3.5-flash",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",
    "gemini-3-flash"
)

fun isDoroAutoModel(model: String): Boolean = model == DORO_AUTO_MODEL_ID

fun modelDisplayName(model: String): String =
    if (isDoroAutoModel(model)) DORO_AUTO_MODEL_LABEL else model

class DoroAutoExhaustedException(
    cause: ApiHttpException? = null
) : IOException(
    "HTTP 429: DoroAutoMode quota exhausted for all Google fallback models.",
    cause
)

/**
 * Resolves the app-only Doro model selection to concrete Google model IDs.
 *
 * The daily-exhaustion cache is intentionally held by this instance rather than a process-global
 * singleton so each long-lived chat service owns its own runtime-only state.
 */
class DoroAutoRouter(
    private val dayProvider: () -> String = ::pacificQuotaDay
) {
    private data class CacheKey(
        val apiKeyFingerprint: String,
        val quotaDay: String
    )

    private val cacheMutex = Mutex()
    private val dailyExhaustedModels = mutableMapOf<CacheKey, MutableSet<String>>()

    suspend fun <T> run(
        selectedModel: String,
        apiKey: String,
        request: suspend (resolvedModel: String) -> T
    ): T {
        if (!isDoroAutoModel(selectedModel)) {
            return request(selectedModel)
        }

        val quotaDay = dayProvider()
        val cacheKey = CacheKey(
            apiKeyFingerprint = apiKey.sha256Fingerprint(),
            quotaDay = quotaDay
        )
        val candidates = cacheMutex.withLock {
            discardOlderDaysLocked(quotaDay)
            val exhausted = dailyExhaustedModels[cacheKey].orEmpty()
            DORO_AUTO_FALLBACK_MODELS.filterNot(exhausted::contains)
        }

        if (candidates.isEmpty()) {
            throw DoroAutoExhaustedException()
        }

        var lastRateLimit: ApiHttpException? = null
        for (candidate in candidates) {
            try {
                return request(candidate)
            } catch (error: ApiHttpException) {
                if (error.statusCode != 429) {
                    throw error
                }
                lastRateLimit = error
                if (isDailyQuotaExceeded(error)) {
                    markDailyQuotaExhausted(cacheKey, candidate, quotaDay)
                }
            }
        }

        throw DoroAutoExhaustedException(lastRateLimit)
    }

    private suspend fun markDailyQuotaExhausted(
        cacheKey: CacheKey,
        model: String,
        quotaDay: String
    ) {
        cacheMutex.withLock {
            discardOlderDaysLocked(quotaDay)
            dailyExhaustedModels.getOrPut(cacheKey) { linkedSetOf() }.add(model)
        }
    }

    private fun discardOlderDaysLocked(currentDay: String) {
        dailyExhaustedModels.keys.removeAll { cacheKey -> cacheKey.quotaDay < currentDay }
    }
}

internal fun isDailyQuotaExceeded(error: ApiHttpException): Boolean {
    val payload = buildString {
        append(error.apiMessage)
        append('\n')
        append(error.responseBody)
    }
    val normalized = payload.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
    return normalized.contains("perday") ||
        normalized.contains("dailyquota") ||
        normalized.contains("dailyrequest") ||
        (normalized.contains("quota") && normalized.contains("daily")) ||
        Regex("\\brpd\\b", RegexOption.IGNORE_CASE).containsMatchIn(payload)
}

internal fun pacificQuotaDay(now: Date = Date()): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/Los_Angeles")
    }.format(now)
}

private fun String.sha256Fingerprint(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
