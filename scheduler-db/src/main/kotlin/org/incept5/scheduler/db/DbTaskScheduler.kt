package org.incept5.scheduler.db

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerBuilder
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.*
import com.github.kagkarlsson.scheduler.task.CompletionHandler.OnCompleteRemove
import com.github.kagkarlsson.scheduler.task.CompletionHandler.OnCompleteReschedule
import com.github.kagkarlsson.scheduler.task.FailureHandler.ExponentialBackoffFailureHandler
import com.github.kagkarlsson.scheduler.task.FailureHandler.OnFailureRetryLater
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import org.incept5.scheduler.data.JobbingTaskData
import org.incept5.scheduler.data.TaskDataSerializer
import jakarta.annotation.PreDestroy
import org.incept5.scheduler.TaskScheduler
import org.incept5.scheduler.config.FrequencyConfig
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.RetryConfig
import org.incept5.scheduler.config.SchedulerConfig
import org.incept5.scheduler.model.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.*
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import java.util.function.Consumer
import javax.sql.DataSource
import kotlin.math.pow

/**
 * Creates a facade around the kagkarlsson db-scheduler library; with the aim of
 * reducing our dependency on that library. https://github.com/kagkarlsson/db-scheduler
 * <p>
 * The concept behind db-scheduler is; tasks (along with any data) are persisted
 * to the database and shared out to all service nodes in a cluster. To enable this,
 * all service nodes must initialise a scheduler (usually on start-up) to poll the
 * database for named tasks. The polled tasks are then passed to worker threads for
 * processing. These worker threads will remain running until the service is shutdown.
 * <p>
 * In this facade, the initialisation of the scheduler is governed by the configuration
 * (see {@link SchedulerConfig}), and a coll
 *
 * @param dataSource The datasource to which the scheduler jobs will be written.
 * @param configuration The scheduler's configuration properties, and that of the named tasks.
 * @param namedTasks The collection of NamedTasks to be scheduled. NamedScheduledTasks will
 * only be scheduled if a corresponding entry is found in the tasks of the configuration.
 */
open class DbTaskScheduler(
    private val dataSource: DataSource,
    configuration: SchedulerConfig,
    namedTasks: List<NamedTask>,
    configurer: DbSchedulerConfigurer? = null
) : TaskScheduler {

    companion object {
        private val logger = LoggerFactory.getLogger(DbTaskScheduler::class.java)
    }

    private val scheduler: Scheduler
    private val jobbingTasks: Map<String, Task<JobbingTaskData>>
    private val recurringTasks: List<RecurringTask<*>>
    private var started = false

    /**
     * Creates tasks, to process scheduled and ad-hoc work items, in accordance with
     * the given configuration.
     */
    init {
        val pollingInterval = configuration.pollingInterval()?.also { SchedulerConfig.DEFAULT_POLLING_INTERVAL }
        logger.info ( "Initialising scheduled tasks : pollingInterval $pollingInterval")

        // validate named tasks for duplicates
        val names: MutableSet<String> = HashSet()
        namedTasks.forEach { task ->
            if (names.contains(task.getName())) {
                throw IllegalArgumentException("Duplicate Task name - ${task.getName()}")
            }

            names.add(task.getName())
        }

        jobbingTasks = createJobbingTasks(namedTasks, configuration)
        recurringTasks = createScheduledTasks(namedTasks, configuration)
        logger.info ("Scheduled tasks : numJobbingTasks ${jobbingTasks.size}, numRecurringTasks ${recurringTasks.size}")
        scheduler = scheduleTasks(configuration, jobbingTasks.values, recurringTasks, configurer)

        // inform all tasks that they have been started
        namedTasks.forEach(Consumer { task: NamedTask -> task.taskScheduled(this) })
    }

    /**
     * You need to call this to start the scheduler after creation
     */
    fun start(): DbTaskScheduler {
        if (jobbingTasks.isNotEmpty() || recurringTasks.isNotEmpty()) {
            scheduler.start()
            started = true
        } else {
            logger.warn ("Not starting scheduler - no configured tasks" )
        }
        return this
    }

    /**
     * Can be used to stop the polling for work, and to cancel tasks currently
     * running.
     */
    @PreDestroy
    fun stop() {
        if (started) {
            logger.info ( "Stopping scheduler" )
            scheduler.stop()
        }
    }

    /**
     * Schedules a job of work for the named Jobbing task, to process the given payload.
     *
     * @param name the name of the Jobbing task to pass the payload to for processing.
     * @param payload the payload to be processed.
     * @return the unique identifier for the scheduled job.
     */
    override fun scheduleTask(name: String, payload: Any, executeAt: Instant): String {
        val task: Task<JobbingTaskData> = jobbingTasks[name]
            ?: throw IllegalArgumentException("No Jobbing Task found named \"$name\"")

        val id = UUID.randomUUID()
        logger.trace("Queuing jobbing task : name {}, id {}, executeAt {}", name, id, executeAt)

        // schedule the job's payload - with the caller's correlation ID
        val correlationId = Correlation.getCorrelationId()
        scheduler.schedule(task.instance(id.toString(), JobbingTaskData(correlationId, payload)), executeAt)
        return id.toString()
    }

    /**
     * Schedules a recurring task to run at the given time.
     * This is used primarily by testware via the http interface to run recurring
     * tasks now instead of having to wait for the scheduled time. Also could be
     * useful in production to run a task now instead of waiting for the scheduled time.
     */
    override fun scheduleRecurringTask(name: String, executeAt: Instant): String {
        val task = recurringTasks.find { it.taskName == name }
            ?: throw IllegalArgumentException("No Recurring Task found named \"$name\"")

        val id = UUID.randomUUID()
        logger.trace ( "Queuing recurring task : name {}, id {}, executeAt {}", name, id, executeAt)

        scheduler.schedule(task.instance(id.toString()), executeAt)
        return id.toString()
    }

    /**
     * Configures the recurring tasks from the given collection of NamedTasks. Only
     * those tasks for which a configuration is given will be created.
     *
     * @param namedTasks the collection of NamedTasks to be configured.
     * @param configuration the configurations to be matched to the NamedTasks.
     * @return the configured collection of recurring tasks.
     */
    private fun createScheduledTasks(
        namedTasks: Iterable<NamedTask>?,
        configuration: SchedulerConfig
    ): List<RecurringTask<*>> {
        if (namedTasks == null) {
            return emptyList()
        }
        val result: MutableList<RecurringTask<*>> = ArrayList<RecurringTask<*>>()
        namedTasks.forEach { task: NamedTask ->
            if (task is NamedScheduledTask) {
                 // find any configuration related to the named task
                var namedTaskConfig: NamedTaskConfig? = configuration.tasks()[task.getName()]
                if (namedTaskConfig == null && task is NamedTaskConfig) {
                    logger.debug ( "Inline scheduled task configuration : {}", task.getName())
                    // inline configuration of the task on itself
                    namedTaskConfig = task
                }
                if (namedTaskConfig == null) {
                    if (configuration.lenientConfig()) {
                        logger.warn ("No Scheduled Task Configuration Found : {}", task.getName())
                    } else {
                        throw IllegalArgumentException("No scheduled task configuration found [task: ${task.getName()}]")
                    }
                } else {
                    val schedule: Schedule = parseSchedule(task.getName(), namedTaskConfig)
                    logger.info ( "Scheduled task : name {}, schedule {}", task.getName(), schedule)

                    // create and configure recurring task
                    val builder = Tasks.recurring(task.getName(), schedule)

                    configureFailureHandler<Void>(namedTaskConfig)
                        .ifPresent { failureHandler: FailureHandler<Void>? ->
                            builder.onFailure(failureHandler)
                        }

                    // add task (and call-back) to the result
                    result.add(
                        builder.execute { _: TaskInstance<Void>, _: ExecutionContext ->
                            // call the task - using a new correlation ID
                            Correlation.run(UUID.randomUUID().toString(), task)
                        }
                    )
                }
            }
        }
        return result
    }

    /**
     * Configures jobbing tasks from the given collection NamedTasks.
     *
     * @param namedTasks the collection of NamedTasks to be configured.
     * @return the configured collection of jobbing tasks.
     */
    @Suppress("UNCHECKED_CAST")
    private fun createJobbingTasks(
        namedTasks: Iterable<NamedTask>?,
        configuration: SchedulerConfig
    ): Map<String, Task<JobbingTaskData>> {
        if (namedTasks == null) {
            return emptyMap()
        }

        val result = HashMap<String, Task<JobbingTaskData>>()
        namedTasks.forEach { task: NamedTask ->

            // find optional configuration related to the named task coming from the scheduler config
            var taskConfig = Optional.ofNullable(configuration.tasks()[task.getName()])
            if (taskConfig.isEmpty && task is NamedTaskConfig) {
                logger.debug("Inline jobbing task configuration found : {}", task.getName())
                // inline configuration of the task on itself
                taskConfig = Optional.ofNullable(task as NamedTaskConfig)
            }

            if (task is NamedJobbingTask<*>) {
                logger.info ( "Adding Jobbing Task : {}", task.getName())

                // create a custom task
                val builder = Tasks.custom(task.getName(), JobbingTaskData::class.java)

                // configure failure (exception) handling
                taskConfig.ifPresent { config ->
                    logger.debug ( "Jobbing Task configuration found : {}",  task.getName())
                    configureFailureHandler<JobbingTaskData>(config)
                        .ifPresent { builder.onFailure(it) }
                }

                // add task - with a call-back to execute
                result[task.getName()] = builder.execute { inst: TaskInstance<JobbingTaskData>, ctx: ExecutionContext ->
                    // construct the context for the task to run in - including the job payload
                    val taskContext: TaskContext<Any> = TaskContext(inst.data.getPayloadContent<Any>()!!).apply {
                        failureCount = ctx.execution.consecutiveFailures
                        repeatCount = inst.data.repeatCount
                    }

                    logger.debug("Running jobbing task : name {}, instance {}, repeatCount {}, failureCount {}", task.getName(), inst.taskAndInstance, taskContext.repeatCount, taskContext.failureCount)

                    // call the task using the correlation ID used when job was queued
                    val function = task as NamedJobbingTask<Any>
                    val conclusion = Correlation.call(inst.data.correlationId!!, function, taskContext)

                    // if task has completed
                    if (conclusion === TaskConclusion.COMPLETE) {
                        logger.debug ( "Task completed : instance {}, repeatCount {}",  inst.taskAndInstance, taskContext.repeatCount)
                        OnCompleteRemove()
                    }

                    // task has not reached a conclusion
                    else {
                        // reset the consecutive failure count - as it didn't fail this time
                        ctx.execution.consecutiveFailures = 0

                        // increment the repeat count
                        inst.data.repeatCount++

                        // determine if, and when, the incomplete task should be repeated
                        val repeatInterval = calcRepeatInterval(taskConfig, inst.data.repeatCount)
                        if (repeatInterval.isPresent) {
                            logger.debug ("Re-scheduling incomplete task : name {}, instance {}, repeatCount {}, interval {}", task.getName(), inst.taskAndInstance, taskContext.repeatCount,  repeatInterval.get())
                            OnCompleteReschedule(Schedules.fixedDelay(repeatInterval.get()), inst.data)
                        } else {
                            // max-repeats reached - fail the task
                            logger.error ("Task failed to complete after retrying - cancelling execution : name {}, instance {}, onSuccessFailure {}", task.getName(), inst.taskAndInstance,  taskContext.repeatCount)

                            // if an on-max-retry task was named in the config
                            taskConfig
                                .flatMap(NamedTaskConfig::onIncomplete)
                                .flatMap(RetryConfig::onMaxRetry)
                                .ifPresent { onMaxRetryTaskName: String ->
                                    try {
                                        logger.debug ( "Queuing on-max-retry task : {}", onMaxRetryTaskName)
                                        Correlation.run(inst.data.correlationId!!) {
                                            scheduleTask(onMaxRetryTaskName, inst.data.getPayloadContent<Any>()!!)
                                        }
                                    } catch (e: java.lang.Exception) {
                                        logger.error( "Failed to queue on-max-retry task", e)
                                    }
                                }

                            // end this task
                            OnCompleteRemove()
                        }
                    }
                }
            }
        }
        return result
    }

    /**
     * Creates a new Scheduler of the given configuration. The given collections of
     * jobbing and recurring tasks are started.
     *
     * @param configuration the scheduler configuration.
     * @param jobbingTasks the collection of Jobbing tasks to be started.
     * @param recurringTasks the collection of recurring tasks to be started.
     * @return the new scheduler. Will be null if no tasks are given.
     */
    private fun scheduleTasks(
        configuration: SchedulerConfig,
        jobbingTasks: Collection<Task<*>>,
        recurringTasks: Collection<RecurringTask<*>>,
        configurer: DbSchedulerConfigurer? = null
    ): Scheduler {
        logger.info ( "Scheduling named tasks : numJobbingTasks {}, numRecurringTasks {}", jobbingTasks.size, recurringTasks.size)

        val schemaName = configuration.schema()
        logger.debug ( "Creating scheduler : {}", schemaName.orElse(null))
        val tableName = configuration.schema().map { "$it.scheduled_tasks" }.orElse("scheduled_tasks")
        val builder: SchedulerBuilder = Scheduler.create(dataSource, ArrayList(jobbingTasks))
            .tableName(tableName)
            .threads(configuration.threadCount().orElse(SchedulerConfig.DEFAULT_THREAD_COUNT))
            .pollingInterval(configuration.pollingInterval().orElse(SchedulerConfig.DEFAULT_POLLING_INTERVAL))
            .heartbeatInterval(configuration.heartbeatInterval().orElse(SchedulerConfig.DEFAULT_HEARTBEAT_INTERVAL))
            .shutdownMaxWait(configuration.shutdownMaxWait().orElse(SchedulerConfig.DEFAULT_SHUTDOWN_MAX_WAIT))
            .deleteUnresolvedAfter(configuration.unresolvedTimeout().orElse(SchedulerConfig.DEFAULT_UNRESOLVED_TIMEOUT))
            .startTasks(ArrayList(recurringTasks))
            .serializer(TaskDataSerializer()) // this is our custom serializer that uses Jackson to serialize the payload
            .registerShutdownHook()

        // use the given configurer to configure the builder (if provided)
        configurer?.configureBuilder(builder)

        return builder.build()
    }

    private fun parseSchedule(
        taskName: String,
        config: NamedTaskConfig
    ): Schedule {
        var result: Schedule? = null

        val frequencyConfig: FrequencyConfig? = config.frequency().orElse(null)
        if (frequencyConfig != null) {
            result = frequencyConfig.recurs()
                .map { delay: Duration -> Schedules.fixedDelay(delay) }
                .orElse(null)

            if (result == null) {
                result = frequencyConfig.timeOfDay()
                    .map(LocalTime::parse)
                    .map(Schedules::daily)
                    .orElse(null)
            }

            if (result == null) {
                result = frequencyConfig.cron()
                    .map(Schedules::cron)
                    .orElse(null)
            }
        }

        if (result == null) {
            throw IllegalArgumentException("Schedule period may not be null - taskName: $taskName")
        }
        return result
    }

    /**
     * Returns the underlying scheduler
     */
    fun schedulerClient(): SchedulerClient {
        return scheduler
    }

    /**
     * Constructs a FailureHandler from the given NamedTaskConfig. The handler may consist of
     * composite handlers.
     *
     * @param namedTaskConfig the configuration from which to create the handler.
     * @return the configured FailureHandler.
     */
    private fun <T> configureFailureHandler(namedTaskConfig: NamedTaskConfig): Optional<FailureHandler<T>> {
        return namedTaskConfig.onFailure().map { retryConfig ->
            var result: FailureHandler<T>? = null
            // if a retry config is given
            if (retryConfig.retryInterval().isPresent) {
                // create a back-off failure handler
                result = ExponentialBackoffFailureHandler(
                    retryConfig.retryInterval().get(),
                    retryConfig.retryExponent().orElse(RetryConfig.DEFAULT_RETRY_EXPONENT)
                )
            }

            // if a max-retry config is given
            if (retryConfig.maxRetry().isPresent) {
                // if no retry config was given, use a default
                if (result == null) {
                    result = OnFailureRetryLater(RetryConfig.DEFAULT_RETRY_INTERVAL)
                }

                // create max-retry failure handler - wrapping the retry handler
                result = MaxRetriesWithAbortHandler(
                    this,
                    retryConfig.maxRetry().asInt,
                    retryConfig.onMaxRetry().orElse(null),
                    result
                )
            }
            result
        }
    }

    /**
     * Calculates the delay before an INCOMPLETE NamedJobbingTask will be repeated.
     * If no repeat configuration is given, or the max-retries has been reached,
     * the return value will be empty.
     *
     * @param config the NamedJobbingTask's configuration.
     * @param repeatCount the count of the task's repeats
     * @return the delay after which the task will be run.
     */
    private fun calcRepeatInterval(config: Optional<NamedTaskConfig>, repeatCount: Int): Optional<Duration> {
        return config
            .flatMap(NamedTaskConfig::onIncomplete)
            .filter { c -> c.maxRetry().isEmpty || repeatCount <= c.maxRetry().asInt }
            .map { c: RetryConfig ->
                val interval: Long = c.retryInterval()
                    .map { interval: Duration -> interval.toMillis() }
                    .orElse(RetryConfig.DEFAULT_RETRY_INTERVAL.toMillis())

                val exponent: Double = c.retryExponent().orElse(RetryConfig.DEFAULT_RETRY_EXPONENT)
                Duration.ofMillis(
                    if (c.retryExponent().isPresent) {
                        Math.round(interval * exponent.pow(repeatCount.toDouble()))
                    } else {
                        interval
                    }
                )
            }
    }

    class MaxRetriesWithAbortHandler<T>(
        private val scheduler: TaskScheduler,
        private val maxRetries: Int,
        private val onMaxRetryTaskName: String?,
        private val failureHandler: FailureHandler<T>
    ) : FailureHandler<T> {
        override fun onFailure(executionComplete: ExecutionComplete, executionOperations: ExecutionOperations<T>) {
            val consecutiveFailures = executionComplete.execution.consecutiveFailures
            val totalNumberOfFailures = consecutiveFailures + 1
            if (totalNumberOfFailures > maxRetries) {
                logger.error ("Execution failed for scheduled task - cancelling execution : instance {}, totalNumberOfFailures {}", executionComplete.execution.taskInstance,  totalNumberOfFailures)
                executionOperations.stop()

                // schedule the abort task
                if (onMaxRetryTaskName != null) {
                    try {
                        logger.debug ( "Queuing on-max-retry task : {}", onMaxRetryTaskName)
                        val taskData: JobbingTaskData =
                            executionComplete.execution.taskInstance.data as JobbingTaskData
                        Correlation.run(taskData.correlationId!!) {
                            scheduler.scheduleTask(onMaxRetryTaskName, taskData.getPayloadContent<Any>()!!)
                        }
                    } catch (e: Exception) {
                        logger.error ("Failed to queue on-max-retry task", e)
                    }
                }
            } else {
                failureHandler.onFailure(executionComplete, executionOperations)
            }
        }
    }

}
