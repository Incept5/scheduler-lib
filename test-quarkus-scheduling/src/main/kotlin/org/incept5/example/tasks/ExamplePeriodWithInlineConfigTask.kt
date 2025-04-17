package org.incept5.example.tasks

import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.std.FrequencyConfigData
import org.incept5.scheduler.model.NamedScheduledTask
import jakarta.enterprise.context.ApplicationScoped
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
class ExamplePeriodWithInlineConfigTask : NamedScheduledTask, NamedTaskConfig {

    companion object {
        var count = AtomicInteger(0)
    }

    override fun run() {
        count.incrementAndGet()
        println("ExampleRecurringTask count is now: ${count.get()}")
    }

    override fun getName(): String {
        return "example-periodic-with-inline-config-task"
    }

    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.of(FrequencyConfigData(cron = "0 0 2 1 * ?"))
    }

    override fun onFailure(): Optional<RetryConfig> {
        return Optional.empty()
    }

    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.empty()
    }
}