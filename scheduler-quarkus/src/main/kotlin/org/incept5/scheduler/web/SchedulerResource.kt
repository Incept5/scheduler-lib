package org.incept5.scheduler.web

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import org.incept5.scheduler.TaskScheduler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

@Path("/api/scheduler")
@ApplicationScoped
@Authenticated
class SchedulerResource(private val scheduler: TaskScheduler) {


    /**
     * Run a recurring task NOW instead of waiting for the next
     * scheduled time.
     * MUST have platform_admin role to access this endpoint
     */
    @POST
    @Path("/recurring-tasks/{taskName}")
    @Transactional
    @Authenticated
    @RolesAllowed("platform_admin")
    fun triggerRecurringTask(
        @PathParam("taskName") taskName: String
    ): Response {
        return try {
            scheduler.scheduleRecurringTask(taskName)
            Response.ok().build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.NOT_FOUND).entity("Task does not exist").build()
        }
    }
}