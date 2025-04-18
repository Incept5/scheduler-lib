package org.incept5.example

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Alternative
import jakarta.enterprise.inject.Produces
import org.incept5.scheduler.ApiConfig
import java.util.Optional

/**
 * A mock implementation of ApiConfig for testing
 */
@ApplicationScoped
@Alternative
class TestApiConfig : ApiConfig {
    override fun rolesAllowed(): Optional<List<String>> {
        return Optional.of(listOf("platform_admin", "scheduler_admin"))
    }
}