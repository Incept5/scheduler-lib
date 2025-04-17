package org.incept5.scheduler.model

import org.incept5.scheduler.TaskScheduler

/**
 * A named instance of a task.
 */
interface NamedTask {
    fun getName(): String

    fun taskScheduled(scheduler: TaskScheduler) {}
}
