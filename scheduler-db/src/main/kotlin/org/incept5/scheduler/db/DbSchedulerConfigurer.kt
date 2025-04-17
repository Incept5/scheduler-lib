package org.incept5.scheduler.db

import com.github.kagkarlsson.scheduler.SchedulerBuilder

/**
 * Configure the DbScheduler before it starts
 */
interface DbSchedulerConfigurer {

    fun configureBuilder(builder: SchedulerBuilder)

}