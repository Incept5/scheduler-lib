package org.incept5.scheduler.db

import org.incept5.scheduler.db.helper.TestDataSource
import org.incept5.scheduler.config.NamedTaskConfig
import org.incept5.scheduler.config.std.FrequencyConfigData
import org.incept5.scheduler.config.std.NamedTaskConfigData
import org.incept5.scheduler.config.std.SchedulerConfigData
import org.incept5.scheduler.model.NamedScheduledTask
import org.incept5.scheduler.model.NamedTask
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.spec.style.ShouldSpec
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ScheduledTaskTest : ShouldSpec({

    val standardWait = eventuallyConfig {
        interval = 2.seconds
        duration = 25.seconds
    }

    should("testDailyTask") {
        val signal = AtomicBoolean()
        val task: NamedTask = object : NamedScheduledTask {
            override fun getName(): String {
                return "test-daily"
            }

            override fun run() {
                println("Running task ${getName()}: (${signal.getAndSet(true)})")
            }
        }
        val timeOfDay = LocalTime.now().plusSeconds(5)
        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                FrequencyConfigData(null, timeOfDay, null),
                null,
                null
            )
        )
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            SchedulerConfigData(taskConfigs),
            mutableListOf(task)
        ).start()

        eventually(standardWait) {
            signal.get()
        }

        fixture.stop()
    }

    should("testFixedDelay") {
        val signal = AtomicInteger()
        val task: NamedTask = object : NamedScheduledTask {
            override fun getName(): String {
                return "test-fixed-delay"
            }

            override fun run() {
                println("Running task ${getName()}: (${signal.incrementAndGet()})")
            }
        }
        val taskConfigs: Map<String, NamedTaskConfig> = mapOf(
            task.getName() to NamedTaskConfigData(
                FrequencyConfigData(Duration.ofSeconds(2)),
                null,
                null
            )
        )
        val fixture = DbTaskScheduler(
            TestDataSource.getInstance(),
            SchedulerConfigData(taskConfigs, Duration.ofSeconds(2)),
            mutableListOf(task)
        ).start()

        eventually(standardWait) {
            signal.get() == 3
        }

        fixture.stop()
    }
})
