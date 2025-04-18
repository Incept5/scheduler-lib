package org.incept5.scheduler

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import java.util.Optional

/**
 * Configuration for the scheduler API endpoints
 */
@ConfigMapping(prefix = "incept5.scheduler.api")
interface ApiConfig {
    /**
     * Roles allowed to access protected scheduler API endpoints
     * If not specified, defaults to "platform_admin"
     */
    fun rolesAllowed(): Optional<List<String>>
}