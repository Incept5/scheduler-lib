package org.incept5.scheduler.config.std

import org.incept5.scheduler.config.FrequencyConfig
import java.time.Duration
import java.time.LocalTime
import java.util.*

data class FrequencyConfigData(var recurs: Duration? = null, var timeOfDay: LocalTime? = null, var cron: String? = null) : FrequencyConfig {
    override fun recurs(): Optional<Duration> {
        return Optional.ofNullable(recurs)
    }

    override fun timeOfDay(): Optional<String> {
        return Optional.ofNullable(timeOfDay?.toString())
    }

    override fun cron(): Optional<String> {
        return Optional.ofNullable(cron)
    }
}