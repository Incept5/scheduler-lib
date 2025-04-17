package org.incept5.example.tasks

import org.incept5.scheduler.model.NamedScheduledTask
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.atomic.AtomicInteger

@ApplicationScoped
class ExampleRecurringTask : NamedScheduledTask {

    companion object {
        var count = AtomicInteger(0)
    }

    override fun run() {
        count.incrementAndGet()
        println("ExampleRecurringTask count is now: ${count.get()}")
    }

    override fun getName(): String {
        return "example-recurring-task"
    }
}