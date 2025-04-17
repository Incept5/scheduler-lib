package org.incept5.scheduler.model

import org.incept5.scheduler.TaskScheduler
import java.time.Instant
import java.util.function.Function
import org.slf4j.LoggerFactory


/**
 * A named instance of a named task that can execute jobs on an arbitrary frequency.
 * Extends Consumer, and jobs are queued (added) as payloads to be passed to the
 * accept() method when scheduled.
 *
 * @see org.incept5.scheduler.SchedulerFactory#queueJob(String, Any)
 */
open class NamedJobbingTask<T>() : Function<TaskContext<T>, TaskConclusion>, NamedTask {

    companion object {
        private val logger = LoggerFactory.getLogger(NamedJobbingTask::class.java)
    }

    constructor(name: String) : this() {
        this.name = name
    }

    private lateinit var name: String
    var scheduler: TaskScheduler? = null

    override fun getName(): String {
        return name
    }

    override fun taskScheduled(scheduler: TaskScheduler) {
        logger.debug ( "Task scheduled: taskName: $name")
        this.scheduler = scheduler
    }

    /**
     * Schedules a job of work for this Jobbing task to process the given payload.
     *
     * Allows the task to queue a job. This is equivalent to calling the scheduler
     * directly:
     * ```
     * scheduler.queueJob(task.getName(), jobPayload)
     * ```
     * @param payload the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    fun scheduleJob(payload: T, executeAt: Instant = Instant.now()): String {
        logger.debug ( "Scheduling job: taskName $name, executeAt  $executeAt")
        if (scheduler == null) {
            throw UnsupportedOperationException("Scheduler not initialised [task: ${this.getName()}]")
        }

        return scheduler!!.scheduleTask(this.getName(), payload as Any, executeAt)
    }

    /**
     * This is the method called on each run of the task. By default, it will simply
     * call the task's run method, passing its payload provided when the task was queued.
     * However, implementation may override this default implementation to inspect the
     * properties of the context.
     * <p>
     * Any Exception raised by this method will trigger a retry at the configured
     * interval.
     *
     * @param context the context in which the task is being executed. Includes the
     * payload given when the job was queued, and which the task will process.
     */
    override fun apply(context: TaskContext<T>): TaskConclusion {
        accept(context.payload)
        return TaskConclusion.COMPLETE
    }

    /**
     * This is a simplified method that will be called if the apply() method is not
     * overridden. It passes just the task's payload (rather than the entire context)
     * and assumes that the task's return value will always be COMPLETE.
     * <p>
     * Any Exception raised by this method will trigger a retry at the configured
     * interval.
     *
     * @param payload the payload provided when the job was queued.
     */
    open fun accept(payload: T) {
    }
}
