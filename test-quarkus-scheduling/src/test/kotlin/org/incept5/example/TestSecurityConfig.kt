package org.incept5.example

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.incept5.scheduler.ApiConfig
import java.util.Optional

/**
 * Test configuration for the API security
 */
@ApplicationScoped
class TestSecurityConfig {
    
    /**
     * Produces a test implementation of ApiConfig for testing
     */
    @Produces
    @ApplicationScoped
    fun apiConfig(): ApiConfig {
        return object : ApiConfig {
            override fun rolesAllowed(): Optional<List<String>> {
                return Optional.of(listOf("platform_admin", "scheduler_admin"))
            }
        }
    }
}