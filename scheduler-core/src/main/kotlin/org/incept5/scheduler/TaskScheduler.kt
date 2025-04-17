package org.incept5.scheduler

import java.time.Instant

/**
 * This is the core interface for scheduling tasks.
 *
 * Each task name is expected to match a bean that implements the NamedTask interface.
 *
 * How those beans are configured is up to the service etc
 *
 * We have an implementation that uses db-scheduler under details
 *
 */
interface TaskScheduler {

    /**
     * Schedule a named task to run at the given time (defaults to now)
     *
     * @param name the name of the task to run (must be registered via a NamedJobbingTask)
     * @param payload the payload to pass to the task
     * @param executeAt the time to execute the task - defaults to now
     *
     * @TODO correlationId or traceId etc should be propagated
     */
    fun scheduleTask(name: String, payload: Any, executeAt: Instant = Instant.now()): String

    /**
     * Schedule a named recurring task to run at the given time (defaults to now)
     * This is used primarily by testware via the http interface to run recurring
     * tasks now instead of having to wait for the scheduled time. Also could be
     * useful in production to run a task now instead of waiting for the scheduled time.
     *
     * @param name the name of the task to run (must be registered via a NamedJobbingTask)
     * @param executeAt the time to execute the task - defaults to now
     *
     */
    fun scheduleRecurringTask(name: String, executeAt: Instant = Instant.now()): String
}