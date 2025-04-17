package org.incept5.scheduler

import org.incept5.scheduler.config.SchedulerConfig
import io.smallrye.config.ConfigMapping

/**
 * Quarkus compatible configuration for the db scheduler
 */
@ConfigMapping(prefix = "task.scheduler")
interface TaskSchedulerConfig : SchedulerConfig