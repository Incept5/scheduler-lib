package org.incept5.scheduler.config.std

import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.SchedulerConfig
import java.time.Duration
import java.util.Optional

data class SchedulerConfigData(
    val tasks: Map<String, NamedTaskConfig>,
    val pollingInterval: Duration? = null,
    val schema: String? = null,
    val threadCount: Int? = null,
    val heartbeatInterval: Duration? = null,
    val unresolvedTimeout: Duration? = null,
    val shutdownMaxWait: Duration? = null,
    val lenientConfig: Boolean = false
) : SchedulerConfig {
    override fun tasks(): Map<String, NamedTaskConfig> {
        return tasks
    }

    override fun schema(): Optional<String> {
        return Optional.ofNullable(schema)
    }

    override fun threadCount(): Optional<Int> {
        return Optional.ofNullable(threadCount)
    }

    override fun pollingInterval(): Optional<Duration> {
        return Optional.ofNullable(pollingInterval)
    }

    override fun heartbeatInterval(): Optional<Duration> {
        return Optional.ofNullable(heartbeatInterval)
    }

    override fun shutdownMaxWait(): Optional<Duration> {
        return Optional.ofNullable(shutdownMaxWait)
    }

    override fun unresolvedTimeout(): Optional<Duration> {
        return Optional.ofNullable(unresolvedTimeout)
    }

    override fun lenientConfig(): Boolean {
        return lenientConfig
    }
}
