package com.yozora.aichat.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val DORO_AUTO_MODEL_ID = "doro-auto"
const val DORO_AUTO_MODEL_LABEL = "DoroAutoMode"

const val DORO_AUTO_PRO_MODEL_ID = "doro-auto-pro"
const val DORO_AUTO_PRO_MODEL_LABEL = "DoroAutoMode PRO+"

val DORO_AUTO_FALLBACK_MODELS = listOf(
    "gemini-3.6-flash",
    "gemini-3.5-flash",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",
    "gemini-3-flash"
)

/** Highest capability first; every step down is a cheaper, more available model. */
val DORO_AUTO_PRO_FALLBACK_MODELS = listOf(
    "gemini-4-hydrogen",
    "gemini-3.8-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite"
)

private fun doroTierFor(model: String): List<String>? = when (model) {
    DORO_AUTO_MODEL_ID -> DORO_AUTO_FALLBACK_MODELS
    DORO_AUTO_PRO_MODEL_ID -> DORO_AUTO_PRO_FALLBACK_MODELS
    else -> null
}

fun isDoroAutoModel(model: String): Boolean = doroTierFor(model) != null

fun modelDisplayName(model: String): String = when (model) {
    DORO_AUTO_MODEL_ID -> DORO_AUTO_MODEL_LABEL
    DORO_AUTO_PRO_MODEL_ID -> DORO_AUTO_PRO_MODEL_LABEL
    else -> model
}

class DoroAutoExhaustedException(
    cause: ApiHttpException? = null,
    networkCause: IOException? = null
) : IOException(
    if (networkCause != null && cause == null) {
        "DoroAutoMode exhausted: every fallback model timed out or dropped the connection."
    } else {
        "DoroAutoMode exhausted: every fallback model is rate-limited, unavailable, or unreachable."
    },
    cause ?: networkCause
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
        val tier = doroTierFor(selectedModel) ?: return request(selectedModel)

        val quotaDay = dayProvider()
        val cacheKey = CacheKey(
            apiKeyFingerprint = apiKey.sha256Fingerprint(),
            quotaDay = quotaDay
        )
        val candidates = cacheMutex.withLock {
            discardOlderDaysLocked(quotaDay)
            val exhausted = dailyExhaustedModels[cacheKey].orEmpty()
            tier.filterNot(exhausted::contains)
        }

        if (candidates.isEmpty()) {
            throw DoroAutoExhaustedException()
        }

        var lastRetryable: ApiHttpException? = null
        var lastNetwork: IOException? = null
        for (candidate in candidates) {
            try {
                return request(candidate)
            } catch (error: ApiHttpException) {
                if (!isRetryableFailure(error)) {
                    throw error
                }
                lastRetryable = error
                // Only a per-day quota burns the model for the rest of the day.
                // 503 / high-demand is transient: keep the model eligible so the
                // next message can try it again instead of degrading for hours.
                if (error.statusCode == 429 && isDailyQuotaExceeded(error)) {
                    markDailyQuotaExhausted(cacheKey, candidate, quotaDay)
                }
            } catch (error: SocketTimeoutException) {
                // A model that stops responding is just as unusable as one that
                // returns 503; fall through to the next tier instead of failing
                // the whole turn on a single stalled endpoint.
                lastNetwork = error
            } catch (error: IOException) {
                if (!isRetryableNetworkFailure(error)) throw error
                lastNetwork = error
            }
        }

        throw DoroAutoExhaustedException(lastRetryable, lastNetwork)
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

/**
 * Whether this failure means "this model cannot serve right now, try the next one".
 *
 * Two families qualify:
 *  - 429 RESOURCE_EXHAUSTED — quota or per-minute rate limit.
 *  - 503 UNAVAILABLE — Google-side overload, reported either as a status code or
 *    as a "high demand" / "overloaded" message on a 429 body.
 *
 * Everything else (400 bad key, 404 unknown model, 403 permissions) is permanent
 * for the whole tier, so failing fast beats burning requests on the other models.
 */
internal fun isRetryableFailure(error: ApiHttpException): Boolean {
    if (error.statusCode == 429 || error.statusCode == 503) return true
    if (error.statusCode !in 500..599) return false
    val payload = buildString {
        append(error.apiMessage)
        append('\n')
        append(error.responseBody)
    }.lowercase(Locale.US)
    return HIGH_DEMAND_MARKERS.any(payload::contains)
}

/**
 * Network-level failures worth trying another model for. A DNS miss or a refused
 * connection is transport-wide, so retrying the tier cannot help; an SSL or
 * protocol failure is likewise not model-specific. Timeouts and resets are.
 */
internal fun isRetryableNetworkFailure(error: IOException): Boolean = when (error) {
    is SocketTimeoutException -> true
    is ConnectException -> true
    is UnknownHostException -> false
    else -> error.message.orEmpty().let { message ->
        val normalized = message.lowercase(Locale.US)
        normalized.contains("timed out") ||
            normalized.contains("timeout") ||
            normalized.contains("reset") ||
            normalized.contains("broken pipe") ||
            normalized.contains("eof")
    }
}

private val HIGH_DEMAND_MARKERS = listOf(
    "high demand",
    "overloaded",
    "over capacity",
    "capacity",
    "try again later",
    "temporarily unavailable",
    "unavailable",
    "resource exhausted"
)

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
