package org.incept5.example.tasks

import org.incept5.scheduler.model.NamedJobbingTask
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class ExampleComplexPayloadTask : NamedJobbingTask<ExampleComplexPayloadTask.ComplexPayload>("example-complex-payload-task") {

    companion object {
        var count = 0
    }

    @Transactional
    override fun accept(payload: ComplexPayload) {
        count++
        println("ExampleComplexPayloadTask count is now: $count and payload was: $payload")
    }

    data class ComplexPayload(
        val id: String,
        val name: String,
        val myEnum: MyEnum
    )

    enum class MyEnum {
        ONE,
        TWO,
        THREE
    }

}