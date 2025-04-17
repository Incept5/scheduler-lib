package org.incept5.scheduler

import org.incept5.scheduler.config.SchedulerConfig
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.time.Duration
import java.util.Optional

/**
 * Quarkus compatible configuration for the db scheduler
 */
@ConfigMapping(prefix = "task.scheduler")
interface TaskSchedulerConfig : SchedulerConfig {
    @WithDefault("false")
    override fun lenientConfig(): Boolean
    
    override fun schema(): Optional<String>
    
    override fun threadCount(): Optional<Int>
    
    override fun pollingInterval(): Optional<Duration>
    
    override fun heartbeatInterval(): Optional<Duration>
    
    override fun shutdownMaxWait(): Optional<Duration>
    
    override fun unresolvedTimeout(): Optional<Duration>
    
    override fun tasks(): Map<String, org.incept5.scheduler.config.NamedTaskConfig>
}