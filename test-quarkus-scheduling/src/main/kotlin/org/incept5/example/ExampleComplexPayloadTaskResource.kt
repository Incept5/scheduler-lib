package org.incept5.example

import org.incept5.example.tasks.ExampleComplexPayloadTask
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/example-complex-payload-task")
class ExampleComplexPayloadTaskResource(val task: ExampleComplexPayloadTask) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getCount() = "Count is now: ${ExampleComplexPayloadTask.count}"

    @PUT
    @Path("/schedule")
    @Transactional
    fun scheduleTask() {
        task.scheduleJob(ExampleComplexPayloadTask.ComplexPayload("someid", "name", ExampleComplexPayloadTask.MyEnum.TWO))
    }

    @PUT
    @Path("/schedule-and-rollback")
    @Transactional
    fun scheduleTaskThenRollback() {
        task.scheduleJob(ExampleComplexPayloadTask.ComplexPayload("someid", "name", ExampleComplexPayloadTask.MyEnum.TWO))
        throw RuntimeException("Rolling back")
    }

}