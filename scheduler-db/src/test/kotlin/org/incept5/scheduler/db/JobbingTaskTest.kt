package org.incept5.scheduler.db


import org.incept5.scheduler.db.helper.TestDataSource
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.std.NamedTaskConfigData
import org.incept5.scheduler.config.std.RetryConfigData
import org.incept5.scheduler.config.std.SchedulerConfigData
import org.incept5.scheduler.model.NamedJobbingTask
import org.incept5.scheduler.model.TaskConclusion
import org.incept5.scheduler.model.TaskContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Kotests for the DbTaskSchedulerService
 *
 * Checks the scheduling and retry/failure logic etc
 */
class JobbingTaskTest : ShouldSpec({

    val standardWait = eventuallyConfig {
        interval = 2.seconds
        duration = 25.seconds
    }

    val delayThenWait = eventuallyConfig {
        initialDelay = 3.seconds
        interval = 1.seconds
        duration = 3.seconds
    }

    /**
     * Simply tests that a task can be queued several times
     */
    should("testScheduledTasks") {
        val signal = AtomicInteger()
        val task: NamedJobbingTask<String> = object : NamedJobbingTask<String>("test-jobs") {
            override fun accept(payload: String) {
                logger.info { "Task $payload is running (${signal.incrementAndGet()})" }
            }
        }

        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            SchedulerConfigData(emptyMap()),
            mutableListOf(task)
        ).start()

        // queue some jobs
        fixture.scheduleTask(task.getName(), "one")
        fixture.scheduleTask(task.getName(), "two")
        fixture.scheduleTask(task.getName(), "three")

        eventually(standardWait) {
            signal.get() == 3
        }

        // queue some more jobs
        fixture.scheduleTask(task.getName(), "four")
        fixture.scheduleTask(task.getName(), "five")

        eventually(standardWait) {
            signal.get() == 5
        }

        fixture.stop()
    }

    /**
     * Tests that a task is retried when it throws an exception.
     */

    should("testOnFailureRetry") {
        val signal = AtomicInteger()
        val task: NamedJobbingTask<String> = object : NamedJobbingTask<String>("test-jobs") {
            // the task will run 3 times (first two will fail)
            override fun accept(payload: String) {
                val count = signal.incrementAndGet()
                logger.info { "${Instant.now()}: Task $payload is running ($count)" }

                // fail for first 2 runs
                if (count < 3) throw RuntimeException("test retry")
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                // retry on failure every 2 seconds, indefinitely
                RetryConfigData(Duration.ofSeconds(2), 1.0),
                null
            )
        )
        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue some jobs
        fixture.scheduleTask(task.getName(), "one")

        eventually(standardWait) {
            signal.get() == 3
        }

        // wait for jobs to complete
        fixture.stop()
    }

    /**
     * Tests that the task retries on failure for a configured max number of retries
     */
    should("testOnFailureMaxRetry") {
        val signal = AtomicInteger()
        val task: NamedJobbingTask<String> = object : NamedJobbingTask<String>("test-jobs") {
            // test the context data
            override fun apply(context: TaskContext<String>): TaskConclusion {
                signal.get() shouldBe context.failureCount

                // the task will fail on each run
                val count = signal.incrementAndGet()
                logger.info { "${Instant.now()}: Task ${context.payload} is running ($count)" }
                throw RuntimeException("test retry")
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                // retry on failure every 2 seconds, for max 3 times
                RetryConfigData(Duration.ofSeconds(2), 1.0, 3, null),
                null
            )
        )
        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue some jobs
        fixture.scheduleTask(task.getName(), "one")

        // wait for retries
        eventually(standardWait) {
            signal.get() == 4
        }

        // wait some more and check no more retries are made
        eventually(delayThenWait) {
            signal.get() == 4
        }

        fixture.stop()
    }

    /**
     * Tests that another task is executed when the initial task fails for the
     * configured max-retry count
     */
    should("testOnFailureOnMaxRetry") {
        val signal = AtomicInteger()
        val maxRetrySignal = AtomicBoolean()
        val payload = UUID.randomUUID().toString()

        val onMaxRetryTask = object : NamedJobbingTask<String>("on-max-retry-task") {
            // the task will run when max-retry is reached
            override fun apply(context: TaskContext<String>): TaskConclusion {
                logger.info { "Running on-max-retry task: ${context.payload}" }
                payload shouldBe context.payload
                maxRetrySignal.set(true)
                return TaskConclusion.COMPLETE
            }
        }

        val task = object : NamedJobbingTask<String>("initial-task") {
            // the task will fail on each run
            override fun apply(context: TaskContext<String>): TaskConclusion {
                signal.get() shouldBe context.failureCount
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count, payload: ${context.payload}])"
                }
                payload shouldBe context.payload
                throw java.lang.RuntimeException("test task failure")
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                // retry on failure every 2 seconds, for max 3 times, then run other task
                RetryConfigData(Duration.ofSeconds(1), 1.0, 2, onMaxRetryTask.getName()),
                null
            )
        )
        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task, onMaxRetryTask)
        ).start()

        // queue the initial task
        fixture.scheduleTask(task.getName(), payload)

        // wait for retries
        eventually(standardWait) {
            signal.get() == 3
        }

        // and no more retries
        eventually(delayThenWait) {
            signal.get() == 3
        }

        // then check on max retry task was run
        eventually(standardWait) {
            maxRetrySignal.get()
        }

        fixture.stop()
    }

    /**
     * Tests that a task will repeat until it returns COMPLETE
     */
    should("testOnIncompleteRepeat") {
        val signal = AtomicInteger()
        val complete = AtomicBoolean(false)

        val task = object : NamedJobbingTask<String>("repeating-task") {
            override fun apply(context: TaskContext<String>): TaskConclusion {
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count]"
                }
                complete.set(count >= 4)
                return if (complete.get()) TaskConclusion.COMPLETE else TaskConclusion.INCOMPLETE
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                null,
                // repeat until COMPLETE, up to 5 times
                RetryConfigData(Duration.ofSeconds(1), 1.0, 5, null)
            )
        )

        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue the job
        fixture.scheduleTask(task.getName(), "one")

        eventually(standardWait) {
            signal.get() == 4 && complete.get()
        }

        fixture.stop()
    }

    /**
     * Tests that an INCOMPLETE task will NOT repeat if no on-incomplete is configured
     */
    should("testOnIncompleteNoRepeat") {
        val signal = AtomicInteger()

        val task = object : NamedJobbingTask<String>("repeating-task") {
            override fun apply(context: TaskContext<String>): TaskConclusion {
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count]"
                }
                return TaskConclusion.INCOMPLETE
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                null,
                null // no in-complete is configured
            )
        )

        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue the job
        fixture.scheduleTask(task.getName(), "one")

        // wait for job to complete
        // no retries on incomplete applied
        eventually(standardWait) {
            signal.get() == 1
        }

        // no more retry attempts will be made
        // allow time for another retry (which shouldn't happen)
        eventually(delayThenWait) {
            signal.get() == 1
        }

        fixture.stop()
    }

    /**
     * Tests that an INCOMPLETE task will be repeated only a max number of times
     */
    should("testOnCompleteMaxRetry") {
        val signal = AtomicInteger()

        val task = object : NamedJobbingTask<String>("repeating-task") {
            override fun apply(context: TaskContext<String>): TaskConclusion {
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count]"
                }
                return TaskConclusion.INCOMPLETE
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                null,
                // on-complete repeat for a max 3 times
                RetryConfigData(Duration.ofSeconds(1), 1.0, 3, null)
            )
        )

        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue the job
        fixture.scheduleTask(task.getName(), "one")

        // wait for job to complete
        eventually(standardWait) {
            signal.get() == 4
        }

        // no more retry attempts will be made
        eventually(delayThenWait) {
            signal.get() == 4
        }

        fixture.stop()
    }

    /**
     * Tests that a task will be repeated when INCOMPLETE or throws an exception.
     * When no exception is thrown, but the task is INCOMPLETE, the failure count will be reset.
     */
    should("testOnIncompleteWithOnFailureMaxRetry") {
        val signal = AtomicInteger()
        val complete = AtomicBoolean(false)

        val task = object : NamedJobbingTask<String>("one") {
            override fun apply(context: TaskContext<String>): TaskConclusion {
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count]"
                }

                // failure every other run
                // will fail 3 times - but won't reach max-retry due to reset on next repeat
                // only 3 consecutive failures will cause failure max-retry to trigger
                if (count % 2 == 0) {
                    throw java.lang.RuntimeException("Mock failure")
                }

                // will complete after 7 repeats
                complete.set(count >= 7)
                return if (complete.get()) TaskConclusion.COMPLETE else TaskConclusion.INCOMPLETE
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                // retry on failure for a max of 3 consecutive failures - will not reach this max
                RetryConfigData(Duration.ofMillis(400), 1.0, 3, null),
                // repeat on incomplete indefinitely
                RetryConfigData(Duration.ofMillis(400), 1.0, null, null)
            )
        )

        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task)
        ).start()

        // queue the job
        fixture.scheduleTask(task.getName(), "one")

        // wait for job to complete
        eventually(standardWait) {
            signal.get() == 7 && complete.get()
        }

        // no more retry attempts will be made
        eventually(delayThenWait) {
            signal.get() == 7
        }

        fixture.stop()
    }

    /**
     * Tests that another task will be run if the task is INCOMPLETE after a configured
     * max number of repeats.
     */
    should("testOnIncompleteOnMaxRetry") {
        val signal = AtomicInteger()
        val maxRetrySignal = AtomicBoolean()
        val payload = UUID.randomUUID().toString()

        val onMaxRetryTask =
            object : NamedJobbingTask<String>("on-max-retry-task") {
                // the task will run when max-retry is reached
                override fun apply(context: TaskContext<String>): TaskConclusion {
                    logger.info { "Running on-max-retry task: ${context.payload}" }
                    payload shouldBe context.payload
                    maxRetrySignal.set(true)
                    return TaskConclusion.COMPLETE
                }
            }

        val task = object : NamedJobbingTask<String>("initial task") {
            override fun apply(context: TaskContext<String>): TaskConclusion {
                val count = signal.incrementAndGet()
                logger.info {
                    "Task ${context.payload} is running [" +
                        "failureCount: ${context.failureCount}, " +
                        "repeatCount: ${context.repeatCount}, " +
                        "signal: $count, payload: ${context.payload})"
                }
                payload shouldBe context.payload

                // never completes
                return TaskConclusion.INCOMPLETE
            }
        }

        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                null,
                null,
                // on incomplete repeat 2 times, then call on-max-retry task
                RetryConfigData(Duration.ofSeconds(1), 1.0, 2, onMaxRetryTask.getName())
            )
        )

        val config = SchedulerConfigData(taskConfigs, Duration.ofSeconds(1))
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            config,
            mutableListOf(task, onMaxRetryTask)
        ).start()

        // queue the job
        fixture.scheduleTask(task.getName(), payload)

        eventually(standardWait) {
            signal.get() == 3
        }

        eventually(delayThenWait) {
            signal.get() == 3
        }

        eventually(standardWait) {
            maxRetrySignal.get()
        }

        fixture.stop()
    }
})