package org.incept5.scheduler.config.std

import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import java.util.*

data class NamedTaskConfigData(val frequency: FrequencyConfig? = null, val onFailure: RetryConfig? = null, val onIncomplete: RetryConfig? = null) : NamedTaskConfig {
    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.ofNullable(frequency)
    }

    override fun onFailure(): Optional<RetryConfig> {
        return Optional.ofNullable(onFailure)
    }

    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.ofNullable(onIncomplete)
    }

}
