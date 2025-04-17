package org.incept5.scheduler.config

import java.util.*

/**
 * Configures a named task. Each entry is listed within a Map, keyed on the
 * name of the task to which it refers.
 * <p>
 * These configurations are optional for NamedJobbingTasks; for which only the
 * retry config applies.
 */
interface NamedTaskConfig {
    /**
     * Determines how a NamedScheduledTask is scheduled. This is not used by
     * NamedJobbingTasks, which process tasks in an ad-hoc manner.
     */
    fun frequency(): Optional<FrequencyConfig>

    /**
     * Determines how a task is to be retried should it fail with an exception.
     */
    fun onFailure(): Optional<RetryConfig>

    /**
     * Determines how a task is to be retried should it return an INCOMPLETE conclusion.
     */
    fun onIncomplete(): Optional<RetryConfig>
}
