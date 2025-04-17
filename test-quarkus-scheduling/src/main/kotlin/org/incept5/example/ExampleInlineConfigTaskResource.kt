package org.incept5.example

import org.incept5.example.tasks.ExampleInlineConfigTask
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/example-inline-config-task")
class ExampleInlineConfigTaskResource(val task: ExampleInlineConfigTask) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getCount() = "Count is now: ${ExampleInlineConfigTask.count}"

    @PUT
    @Path("/schedule")
    @Transactional
    fun scheduleTask() {
        task.scheduleJob("some payload")
    }

}