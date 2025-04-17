package org.incept5.example.tasks

import org.incept5.scheduler.model.NamedJobbingTask
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ExampleRetryableTask(val failUntilCount: Int = 3) : NamedJobbingTask<String>("example-retryable-task") {

    companion object {
        var count = 0
    }

    @Transactional
    override fun accept(payload: String) {
        count++
        println("ExampleRetryableTask count is now: $count and payload was: $payload")

        if (count < failUntilCount) {
            throw Exception("ExampleRetryableTask failed on count: $count")
        }

        println("ExampleRetryableTask succeeded on count: $count")
    }

}