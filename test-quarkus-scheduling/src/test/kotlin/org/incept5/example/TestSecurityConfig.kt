package org.incept5.example

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Qualifier
import org.incept5.scheduler.ApiConfig
import java.util.Optional

/**
 * Qualifier annotation to distinguish our test implementation
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class TestConfig

/**
 * Test configuration for the API security
 */
@ApplicationScoped
class TestSecurityConfig : ApiConfig {
    override fun rolesAllowed(): Optional<List<String>> {
        return Optional.of(listOf("platform_admin", "scheduler_admin"))
    }
}