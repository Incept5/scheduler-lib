package org.incept5.scheduler.model

/**
 * Passed to each run of a NamedJobbingTask, providing the task with access to the
 * context in which the task is being executed.
 *
 * @param payload The payload to be processed; passed when a task was queued.
 *
 */
data class TaskContext<T>(val payload: T) {
    /**
     * The number of consecutive retries due to error condition. For repeating
     * tasks this will be reset whenever a run passes without failure.
     */
    var failureCount: Int = 0

    /**
     * The number of times a repeating task has been executed without completion.
     */
    var repeatCount: Int = 0
}
