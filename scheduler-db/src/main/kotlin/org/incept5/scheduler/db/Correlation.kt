package org.incept5.scheduler.db


import org.incept5.correlation.CorrelationId
import org.incept5.scheduler.model.NamedJobbingTask
import org.incept5.scheduler.model.TaskContext
import java.util.*
import java.util.function.Function

/**
 * A bunch of utility methods to support the use of the Correlation ID lib.
 */
object Correlation {

    /**
     * A delegate method to return the calling thread's correlation ID.
     */
    fun getCorrelationId(): String {
        return CorrelationId.getId()
    }

    /**
     * A delegate method to set a new correlation ID, returning any previous ID.
     */
    fun setCorrelationId(id: String): String {
        val prev: String = CorrelationId.getId()
        CorrelationId.setId(id)
        return prev
    }

    /**
     * A utility method to call the given Runnable function, setting the given correlation
     * ID for the duration of the call. Restores any previous correlation ID when the function
     * is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aRunnable the function to be called.
     */
    fun run(aCorrelationId: String, aRunnable: Runnable) {
        val prevId: String = setCorrelationId(aCorrelationId)
        try {
            aRunnable.run()
        } finally {
            setCorrelationId(prevId)
        }
    }

    /**
     * A utility method to call the given function with the given argument and return its
     * result, setting the given correlation ID for the duration of the call. Restores any
     * previous correlation ID when the function is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aFunction the function to be called.
     * @param aContext the function's argument value.
     * @param <T> the function's argument type.
     * @param <R> the function's return type.
     </R></T> */
    fun <T, R> call(aCorrelationId: String, aFunction: Function<T, R>, aArg: T): R {
        val prevId: String = setCorrelationId(aCorrelationId)
        return try {
            aFunction.apply(aArg)
        } finally {
            setCorrelationId(prevId)
        }
    }

    /**
     * Wraps the given Runnable function in a new Runnable that will set the current
     * correlation ID before calling the given function.
     */
    fun run(aRunnable: Runnable): Runnable {
        val correlationId = getCorrelationId()
        return Runnable { run(correlationId, aRunnable) }
    }

    /**
     * Calls the given NamedJobbing task passing the given context, and its payload for
     * processing. Sets the given correlation ID for the duration of the call. Restores
     * any previous correlation ID when the function is complete.
     *
     * @param aCorrelationId the correlation ID to be set for the duration of the call.
     * @param aTask the NamedJobbingTask to be called.
     * @param aContext the execution context in which the task will run; incl the payload.
     * @param <T> the function's argument type.
     */
    fun <T> run(aCorrelationId: String, aTask: NamedJobbingTask<T>, aContext: TaskContext<T>) {
        val prevId: String = setCorrelationId(aCorrelationId)
        try {
            aTask.apply(aContext)
        } finally {
            setCorrelationId(prevId)
        }
    }
}
