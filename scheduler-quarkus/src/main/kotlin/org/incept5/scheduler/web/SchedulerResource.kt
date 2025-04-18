package org.incept5.scheduler.web

import io.quarkus.security.Authenticated
import jakarta.annotation.security.RolesAllowed
import org.incept5.scheduler.ApiConfig
import org.incept5.scheduler.TaskScheduler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import jakarta.annotation.PostConstruct
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.ForbiddenException

@Path("/api/scheduler")
@ApplicationScoped
@Authenticated
open class SchedulerResource(
    private val scheduler: TaskScheduler,
    private val apiConfig: ApiConfig
) {
    // Make this protected so it can be accessed by subclasses in tests
    protected lateinit var allowedRoles: Array<String>
    
    @PostConstruct
    open fun init() {
        // Default to "platform_admin" if no roles are configured
        allowedRoles = apiConfig.rolesAllowed()
            .map { it.toTypedArray() }
            .orElse(arrayOf("platform_admin"))
    }

    /**
     * Run a recurring task NOW instead of waiting for the next
     * scheduled time.
     * Requires roles specified in incept5.scheduler.api.roles-allowed configuration
     */
    @POST
    @Path("/recurring-tasks/{taskName}")
    @Transactional
    open fun triggerRecurringTask(
        @PathParam("taskName") taskName: String,
        @Context securityContext: SecurityContext
    ): Response {
        // Check if user has any of the configured roles
        val hasRequiredRole = allowedRoles.any { role -> 
            securityContext.isUserInRole(role) 
        }
        
        if (!hasRequiredRole) {
            throw ForbiddenException("User does not have any of the required roles: ${allowedRoles.joinToString()}")
        }
        
        return try {
            scheduler.scheduleRecurringTask(taskName)
            Response.ok().build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.NOT_FOUND).entity("Task does not exist").build()
        }
    }
}