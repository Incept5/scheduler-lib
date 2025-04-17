package org.incept5.scheduler

import org.incept5.scheduler.db.DbTaskScheduler
import org.incept5.scheduler.model.NamedTask
import io.github.oshai.kotlinlogging.KotlinLogging
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import jakarta.inject.Singleton
import javax.sql.DataSource

/**
 * Wrap DbTaskScheduler to start/stop it on Quarkus startup/shutdown.
 *
 */

private val LOG = KotlinLogging.logger {}

@Singleton
class QuarkusTaskScheduler(dataSource: DataSource,
                           configuration: TaskSchedulerConfig,
                           namedTasks: Instance<NamedTask>,
                           extraTasks: Instance<List<NamedTask>>
    ) : DbTaskScheduler(
    dataSource,
    configuration,
    // combine namedTasks and extraTasks into a single list of NamedTask
    namedTasks.stream().toList() + extraTasks.stream().flatMap { it.stream() }.toList()
) {

    /**
     * Wait till after the CDI container has started before starting the schedule tasks
     */
    fun onStart(@Observes ev: StartupEvent?) {
        LOG.info { "Starting task scheduler..."}
        this.start()
    }

    fun onStop(@Observes ev: ShutdownEvent?) {
        LOG.info { "Stopping task scheduler"}
        this.stop()
    }
}