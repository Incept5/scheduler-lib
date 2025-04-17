package org.incept5.example.tasks

import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.std.RetryConfigData
import org.incept5.scheduler.model.NamedJobbingTask
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Duration
import java.util.*

/**
 * This task provides it's own configuration rather than relying on scheduler config
 * It can still be overridden by scheduler config if desired.
 * This allows libraries to use the tasks mechanism without requiring specific configuration.
 *
 */
@ApplicationScoped
class ExampleInlineConfigTask(val failUntilCount: Int = 3) : NamedJobbingTask<String>("example-inline-config-task"), NamedTaskConfig {

    companion object {
        var count = 0
    }

    @Transactional
    override fun accept(payload: String) {
        count++
        println("ExampleInlineConfigTask count is now: $count and payload was: $payload")

        if (count < failUntilCount) {
            throw Exception("ExampleInlineConfigTask failed on count: $count")
        }

        println("ExampleInlineConfigTask succeeded on count: $count")
    }

    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.empty()
    }

    override fun onFailure(): Optional<RetryConfig> {
        return Optional.of(RetryConfigData(retryInterval = Duration.ofSeconds(1)))
    }

    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.empty()
    }
}