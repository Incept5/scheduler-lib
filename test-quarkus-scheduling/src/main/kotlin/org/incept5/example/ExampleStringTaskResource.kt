package org.incept5.example

import org.incept5.example.tasks.ExampleStringTask
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/example-string-task")
class ExampleStringTaskResource(val exampleStringTask: ExampleStringTask) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getCount() = "Count is now: ${ExampleStringTask.count}"

    @PUT
    @Path("/schedule")
    @Transactional
    fun scheduleTask() {
        exampleStringTask.scheduleJob("some payload")
    }

    @PUT
    @Path("/schedule-and-rollback")
    @Transactional
    fun scheduleTaskThenRollback() {
        exampleStringTask.scheduleJob("some payload")
        throw RuntimeException("Rolling back")
    }

}