package org.incept5.scheduler.config

import java.time.Duration
import java.util.Optional

/**
 * Determines how the scheduler, and the NamedTasks, are configured.
 *
 * See the db-scheduler lib documentation for more information:
 * https://github.com/kagkarlsson/db-scheduler#configuration
 */
interface SchedulerConfig {
    companion object {
        val DEFAULT_THREAD_COUNT: Int = Integer.valueOf(10)
        val DEFAULT_POLLING_INTERVAL: Duration = Duration.ofSeconds(5)
        val DEFAULT_HEARTBEAT_INTERVAL: Duration = Duration.ofMinutes(5)
        val DEFAULT_SHUTDOWN_MAX_WAIT: Duration = Duration.ofMinutes(30)
        val DEFAULT_UNRESOLVED_TIMEOUT: Duration = Duration.ofDays(14)
    }

    /**
     * The name of the DB schema in which the scheduler tables are stored.
     */
    fun schema(): Optional<String>

    /**
     * The number of threads used to process work.
     */
    fun threadCount(): Optional<Int>

    /**
     * Determines the rate at which the scheduler will poll for work.
     */
    fun pollingInterval(): Optional<Duration>

    /**
     * How often to update the heartbeat timestamp for running executions.
     */
    fun heartbeatInterval(): Optional<Duration>

    /**
     * How long the scheduler will wait before interrupting executor-service threads.
     */
    fun shutdownMaxWait(): Optional<Duration>

    /**
     * The time after which executions with unknown tasks are automatically deleted.
     */
    fun unresolvedTimeout(): Optional<Duration>

    /**
     * A collection of NamedTask configuration, keyed on the name of the task to
     * which they relate.
     */
    fun tasks(): Map<String, NamedTaskConfig>

    /**
     * If set to true then the scheduler will just log to warn for missing config
     * The default is to throw an exception if a scheduled task is missing config
     */
    fun lenientConfig(): Boolean = false
}
