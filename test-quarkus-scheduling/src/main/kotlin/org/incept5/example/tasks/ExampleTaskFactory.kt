package org.incept5.example.tasks

import org.incept5.scheduler.model.NamedTask
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import jakarta.inject.Singleton

@Dependent
class ExampleTaskFactory {

    @Produces
    @Singleton
    @Startup
    @Named("extra-tasks")
    fun createExtraTasks(): List<NamedTask> {
        return listOf(
            ExtraTask("extra-task-1"),
            ExtraTask("extra-task-2")
        )
    }

    @Produces
    @Singleton
    @Startup
    @Named("more-extra-tasks")
    fun createMoreExtraTasks(): List<NamedTask> {
        return listOf(
            ExtraTask("extra-task-3"),
            ExtraTask("extra-task-4")
        )
    }

}