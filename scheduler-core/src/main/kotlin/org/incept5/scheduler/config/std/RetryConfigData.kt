package org.incept5.scheduler.config.std

import org.incept5.scheduler.config.RetryConfig
import java.time.Duration
import java.util.*

data class RetryConfigData(var retryInterval: Duration?, var retryExponent: Double? = null, var maxRetry: Int? = null, var onMaxRetry: String? = null) : RetryConfig {
    override fun retryInterval(): Optional<Duration> {
        return Optional.ofNullable(retryInterval)
    }

    override fun retryExponent(): OptionalDouble {
        if (retryExponent == null) {
            return OptionalDouble.empty()
        } else {
            return OptionalDouble.of(retryExponent!!)
        }
    }

    override fun maxRetry(): OptionalInt {
        if (maxRetry == null) {
            return OptionalInt.empty()
        } else {
            return OptionalInt.of(maxRetry!!)
        }
    }

    override fun onMaxRetry(): Optional<String> {
        return Optional.ofNullable(onMaxRetry)
    }

}
