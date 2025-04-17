package org.incept5.example.tasks

import org.incept5.scheduler.model.NamedJobbingTask
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ExampleStringTask : NamedJobbingTask<String>("example-string-task") {

    companion object {
        var count = 0
    }

    @Transactional
    override fun accept(payload: String) {
        count++
        println("ExampleStringTask count is now: $count and payload was: $payload")
    }

}