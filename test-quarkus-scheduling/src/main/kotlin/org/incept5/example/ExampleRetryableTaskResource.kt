package org.incept5.example

import org.incept5.example.tasks.ExampleRetryableTask
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/example-retryable-task")
class ExampleRetryableTaskResource(val task: ExampleRetryableTask) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getCount() = "Count is now: ${ExampleRetryableTask.count}"

    @PUT
    @Path("/schedule")
    @Transactional
    fun scheduleTask() {
        task.scheduleJob("some payload")
    }

}