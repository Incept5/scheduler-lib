package org.incept5.example.tasks

import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.std.FrequencyConfigData
import org.incept5.scheduler.model.NamedScheduledTask
import jakarta.transaction.Transactional
import java.time.Duration
import java.util.*

/**
 * Test that we can add extra tasks to the scheduler
 * via a bean factory returning a list of tasks
 */

class ExtraTask (private val name: String) : NamedScheduledTask, NamedTaskConfig {

    companion object {
        var count = 0
    }

    @Transactional
    override fun run() {
        count++
    }

    override fun getName(): String {
        return name
    }

    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.of(FrequencyConfigData(recurs = Duration.ofSeconds(2)))
    }

    override fun onFailure(): Optional<RetryConfig> {
        return Optional.empty()
    }

    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.empty()
    }

}