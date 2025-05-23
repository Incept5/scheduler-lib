package org.incept5.scheduler.config

import java.time.Duration
import java.util.*

interface RetryConfig {
    companion object {
        const val DEFAULT_RETRY_EXPONENT = 1.5
        val DEFAULT_RETRY_INTERVAL: Duration = Duration.ofMinutes(5)
    }

    /**
     * The initial interval before retrying a task. If required, a value of 1 minute is
     * suggested.
     *
     * Note: the scheduler's polling interval will affect the accuracy at which this
     * interval will be applied.
     *
     * By default, Recurring tasks are rescheduled according to their Schedule one-time
     * tasks are retried again in 5 minutes.
     * @see .retryExponent
     *
     * @see SchedulerConfig.pollingInterval
     */
    fun retryInterval(): Optional<Duration>

    /**
     * The exponential rate at which a task will be retried. The retryInterval will be
     * increased by this factor on each retry. This will only apply if retryInterval is
     * specified.
     * If retryInterval is specified, a default value of 1.5 is applied.
     */
    fun retryExponent(): OptionalDouble

    /**
     * The maximum number of times a task will be retried. If required, a value of 5 is
     * suggested.
     * When both on-failure and on-incomplete configures are supplied, the on-failure
     * max-retry only apply for consecutive failures. If the task returns an "incomplete"
     * result, the failure count will be reset. However, the on-incomplete max-retry will
     * apply regardless of whether the retries are consecutive or not.
     */
    fun maxRetry(): OptionalInt

    /**
     * The name of an optional jobbing task to be run when the max-retry is reached.
     * If no max-retry number is configured, this task will be queued after first
     * unsuccessful run of this task.
     *
     * The named jobbing task must accept the same payload as the task to which this
     * config belongs.
     */
    fun onMaxRetry(): Optional<String>
}
