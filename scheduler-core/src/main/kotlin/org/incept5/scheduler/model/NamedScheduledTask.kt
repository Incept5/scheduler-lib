package org.incept5.scheduler.model

/**
 * A named instance of a named task that can be scheduled to run at configured
 * intervals.
 */
interface NamedScheduledTask : Runnable, NamedTask {
    /**
     * This method will be invoked at each scheduled interval.
     * <p>
     * Any Exception raised by this method will trigger a retry at the configured
     * interval.
     */
    override fun run()
}
