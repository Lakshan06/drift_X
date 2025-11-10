package com.driftdetector.app.core.error

import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import retrofit2.HttpException

/**
 * Retry policy configuration
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val factor: Double = 2.0,
    val shouldRetry: (Throwable) -> Boolean = { it.isRetryable() }
)

/**
 * Default retry configurations for different scenarios
 */
object RetryPolicies {
    val NETWORK = RetryConfig(
        maxRetries = 3,
        initialDelayMs = 1000,
        maxDelayMs = 10000,
        factor = 2.0
    )

    val SERVER_ERROR = RetryConfig(
        maxRetries = 5,
        initialDelayMs = 2000,
        maxDelayMs = 30000,
        factor = 2.0
    )

    val QUICK = RetryConfig(
        maxRetries = 2,
        initialDelayMs = 500,
        maxDelayMs = 5000,
        factor = 2.0
    )

    val PERSISTENT = RetryConfig(
        maxRetries = 10,
        initialDelayMs = 1000,
        maxDelayMs = 60000,
        factor = 1.5
    )
}

/**
 * Retry a suspending function with exponential backoff
 */
suspend fun <T> retryWithExponentialBackoff(
    config: RetryConfig = RetryPolicies.NETWORK,
    block: suspend () -> T
): Result<T> {
    var currentDelay = config.initialDelayMs
    var lastException: Throwable? = null

    repeat(config.maxRetries) { attempt ->
        try {
            Timber.d("🔄 Attempt ${attempt + 1}/${config.maxRetries}")
            val result = block()
            if (attempt > 0) {
                Timber.i("✅ Succeeded after ${attempt + 1} attempts")
            }
            return Result.success(result)
        } catch (e: Exception) {
            lastException = e

            // Check if we should retry this error
            if (!config.shouldRetry(e)) {
                Timber.w(e, "❌ Error is not retryable")
                return Result.failure(e)
            }

            // Don't delay after the last attempt
            if (attempt < config.maxRetries - 1) {
                val delayTime = currentDelay.coerceAtMost(config.maxDelayMs)
                Timber.w(e, "⏳ Attempt ${attempt + 1} failed, retrying in ${delayTime}ms...")
                delay(delayTime)
                currentDelay = (currentDelay * config.factor).toLong()
            } else {
                Timber.e(e, "❌ All ${config.maxRetries} attempts failed")
            }
        }
    }

    return Result.failure(lastException ?: Exception("All retry attempts failed"))
}

/**
 * Retry with custom retry condition
 */
suspend fun <T> retryWhen(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    factor: Double = 2.0,
    shouldRetry: (Throwable, Int) -> Boolean,
    block: suspend () -> T
): Result<T> {
    var currentDelay = initialDelayMs
    var lastException: Throwable? = null

    repeat(maxRetries) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Exception) {
            lastException = e

            if (!shouldRetry(e, attempt)) {
                Timber.w(e, "❌ Retry condition not met")
                return Result.failure(e)
            }

            if (attempt < maxRetries - 1) {
                Timber.w(e, "⏳ Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
    }

    return Result.failure(lastException ?: Exception("All retry attempts failed"))
}

/**
 * Retry with progress callback
 */
suspend fun <T> retryWithProgress(
    config: RetryConfig = RetryPolicies.NETWORK,
    onProgress: (attempt: Int, maxAttempts: Int, error: Throwable?) -> Unit,
    block: suspend () -> T
): Result<T> {
    var currentDelay = config.initialDelayMs
    var lastException: Throwable? = null

    repeat(config.maxRetries) { attempt ->
        onProgress(attempt + 1, config.maxRetries, lastException)

        try {
            val result = block()
            onProgress(config.maxRetries, config.maxRetries, null)
            return Result.success(result)
        } catch (e: Exception) {
            lastException = e

            if (!config.shouldRetry(e)) {
                return Result.failure(e)
            }

            if (attempt < config.maxRetries - 1) {
                delay(currentDelay.coerceAtMost(config.maxDelayMs))
                currentDelay = (currentDelay * config.factor).toLong()
            }
        }
    }

    return Result.failure(lastException ?: Exception("All retry attempts failed"))
}

/**
 * Extension function to determine if an error is retryable
 */
fun Throwable.isRetryable(): Boolean {
    return when (this) {
        is IOException -> true  // Network errors
        is HttpException -> this.code() in 408..599  // Timeout and server errors
        else -> false
    }
}

/**
 * Circuit breaker pattern for preventing repeated failures
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 60000  // 1 minute
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED

    enum class State {
        CLOSED,   // Normal operation
        OPEN,     // Circuit is open, reject all requests
        HALF_OPEN // Testing if service is back
    }

    fun isOpen(): Boolean {
        if (state == State.OPEN) {
            // Check if we should transition to half-open
            if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
                Timber.i("🔄 Circuit breaker transitioning to HALF_OPEN")
                state = State.HALF_OPEN
                return false
            }
            return true
        }
        return false
    }

    fun recordSuccess() {
        failureCount = 0
        if (state == State.HALF_OPEN) {
            Timber.i("✅ Circuit breaker transitioning to CLOSED")
            state = State.CLOSED
        }
    }

    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()

        if (failureCount >= failureThreshold) {
            Timber.w("⚠️ Circuit breaker transitioning to OPEN (failures: $failureCount)")
            state = State.OPEN
        }
    }

    fun reset() {
        failureCount = 0
        state = State.CLOSED
        Timber.i("🔄 Circuit breaker reset")
    }
}

/**
 * Retry with circuit breaker
 */
suspend fun <T> retryWithCircuitBreaker(
    config: RetryConfig = RetryPolicies.NETWORK,
    circuitBreaker: CircuitBreaker,
    block: suspend () -> T
): Result<T> {
    // Check circuit breaker state
    if (circuitBreaker.isOpen()) {
        Timber.w("⚠️ Circuit breaker is OPEN, rejecting request")
        return Result.failure(Exception("Circuit breaker is open"))
    }

    val result = retryWithExponentialBackoff(config, block)

    if (result.isSuccess) {
        circuitBreaker.recordSuccess()
    } else {
        circuitBreaker.recordFailure()
    }

    return result
}
