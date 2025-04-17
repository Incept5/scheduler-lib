# Velostone Scheduler Lib

# Description

This library provides a set of classes that allow services to schedule tasks.
2 types of tasks are supported:

- **Periodic tasks**: Tasks that are executed periodically based on a schedule and have no context/payload
- **Job tasks**: Tasks that are executed programmatically on-demand and have a context/payload

# Installation

You can include the scheduler libraries in your project using either Gradle or Maven.

## Gradle

### Using Version Catalog (recommended)

Add to your `libs.versions.toml` file:

```toml
[versions]
incept5-scheduler = "1.0.X"

[libraries]
incept5-scheduler-core = { module = "com.github.incept5.scheduler-lib:scheduler-core", version.ref = "incept5-scheduler" }
incept5-scheduler-db = { module = "com.github.incept5.scheduler-lib:scheduler-db", version.ref = "incept5-scheduler" }
incept5-scheduler-quarkus = { module = "com.github.incept5.scheduler-lib:scheduler-quarkus", version.ref = "incept5-scheduler" }
```

Then in your `build.gradle.kts` file:

```kotlin
// For Quarkus applications (includes all dependencies)
implementation(libs.incept5.scheduler.quarkus)

// Or if you need specific modules only
implementation(libs.incept5.scheduler.core)
implementation(libs.incept5.scheduler.db)
```

### Direct Dependencies

Alternatively, you can add the dependencies directly in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.incept5.scheduler-lib:scheduler-quarkus:1.0.X")
    // Or individual modules if needed
    // implementation("com.github.incept5.scheduler-lib:scheduler-core:1.0.X")
    // implementation("com.github.incept5.scheduler-lib:scheduler-db:1.0.X")
}
```

## Maven

Add the dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- For Quarkus applications (includes all dependencies) -->
    <dependency>
        <groupId>com.github.incept5.scheduler-lib</groupId>
        <artifactId>scheduler-quarkus</artifactId>
        <version>1.0.X</version>
    </dependency>
    
    <!-- Or if you need specific modules only -->
    <!--
    <dependency>
        <groupId>com.github.incept5.scheduler-lib</groupId>
        <artifactId>scheduler-core</artifactId>
        <version>1.0.X</version>
    </dependency>
    <dependency>
        <groupId>com.github.incept5.scheduler-lib</groupId>
        <artifactId>scheduler-db</artifactId>
        <version>1.0.X</version>
    </dependency>
    -->
</dependencies>
```

# Usage

## Database Configuration

The scheduler requires database tables to store task information. You need to configure Flyway to create these tables.

### Quarkus Configuration

Include the `incept5/scheduler` location as an extra Flyway location in your `application.yaml` or `application.properties`:

```yaml
quarkus:
  flyway:
    default-schema: my_schema
    locations: db/migration,incept5/scheduler
```

Then configure the scheduler to use the same schema:

```yaml
task:
  scheduler:
    schema: ${quarkus.flyway.default-schema}
```

### Manual Database Setup

If you're not using Flyway, you can manually create the required tables. The SQL scripts are located in the `scheduler-db` module under `src/main/resources/velostone/scheduler/R__scheduled_tasks.sql`.

### Database Requirements

The scheduler requires a database that supports:
- JSON data type (PostgreSQL recommended)
- Transaction isolation
- Row locking

## Task Types

### Periodic/Recurring Tasks

Periodic tasks run on a schedule without any payload. They're ideal for regular maintenance operations, polling external systems, or scheduled batch processing.

#### Implementation

Create a class that implements `NamedScheduledTask`:

```kotlin
@ApplicationScoped
class ExamplePeriodicTask : NamedScheduledTask {
    // Unique name for this task
    override fun getName(): String {
        return "example-periodic-task"
    }

    // The task logic to execute on schedule
    @Transactional
    override fun run() {
        // Your task implementation here
        logger.info("Running periodic task")
    }
}
```

#### Configuration

Configure the task schedule in your `application.yaml`:

```yaml
task:
  scheduler:
    tasks:
      example-periodic-task:  # Must match the task name from getName()
        frequency:
          # ISO-8601 duration format (PT5M = every 5 minutes)
          recurs: PT5M
        on-failure:
          max-retry: 3
          retry-interval: PT1M
```

#### Frequency Options

The scheduler supports multiple ways to define when a task should run:

1. **ISO-8601 Duration** (recurs):
   ```yaml
   frequency:
     recurs: PT5M  # Every 5 minutes
   ```

2. **Cron Expression**:
   ```yaml
   frequency:
     cron: "0 0 * * * ?"  # Every hour
   ```

3. **Time of Day**:
   ```yaml
   frequency:
     time-of-day: "02:00"  # Every day at 2 AM
   ```

4. **Combined with Days of Week**:
   ```yaml
   frequency:
     time-of-day: "09:30"
     days-of-week: "MONDAY,WEDNESDAY,FRIDAY"
   ```

### HTTP Triggering of Recurring Tasks

The scheduler provides HTTP endpoints to manually trigger recurring tasks. This is particularly useful for testing, debugging, or handling exceptional business cases.

#### Triggering a Task

```
POST /api/scheduler/recurring-tasks/{task-name}
```

Example:
```
POST http://localhost:8081/api/scheduler/recurring-tasks/example-periodic-task
Headers: 
  Authorization: Bearer your-auth-token
  Content-Type: application/json
```

#### Listing Available Tasks

You can also get a list of all registered recurring tasks:

```
GET /api/scheduler/recurring-tasks
Headers: 
  Authorization: Bearer your-auth-token
```

Response:
```json
[
  {
    "name": "example-periodic-task",
    "lastExecutionTime": "2023-05-15T10:30:00Z",
    "nextExecutionTime": "2023-05-15T10:35:00Z",
    "status": "SCHEDULED"
  },
  ...
]
```

#### Security Considerations

These endpoints should be secured appropriately in production environments. By default, they require authentication.

### Adhoc Jobbing Tasks

Jobbing tasks are executed on-demand with a payload/context. They're ideal for processing specific data or handling events that require background processing.

> **NOTE**: For simple event-driven scenarios, consider using a pub/sub messaging system instead, as it provides a simpler abstraction.

#### Implementation

Create a class that extends `NamedJobbingTask` with your payload type:

```kotlin
@ApplicationScoped
class ExampleComplexPayloadTask : NamedJobbingTask<ExampleComplexPayloadTask.ComplexPayload>("example-complex-payload-task") {
    
    // Define your payload data class
    data class ComplexPayload(
        val id: String,
        val data: String
    )
    
    // The task logic to execute with the payload
    @Transactional
    override fun accept(payload: ComplexPayload) {
        logger.info("Processing job with payload: $payload")
        // Your implementation here
    }
}
```

#### Scheduling Jobs

Inject the task into your service and schedule it with a payload:

```kotlin
@ApplicationScoped
class ExampleService(private val task: ExampleComplexPayloadTask) {
    
    fun processData(id: String, data: String) {
        // Schedule the task to run asynchronously
        task.schedule(ExampleComplexPayloadTask.ComplexPayload(id, data))
        
        // Or schedule with a delay
        task.schedule(
            ExampleComplexPayloadTask.ComplexPayload(id, data),
            Duration.ofMinutes(5) // Run after 5 minutes
        )
    }
}
```

#### Configuration

You can configure retry behavior for jobbing tasks in your `application.yaml`:

```yaml
task:
  scheduler:
    tasks:
      example-complex-payload-task:  # Must match the task name
        on-failure:
          max-retry: 3
          retry-interval: PT1M
```

### Self-Configuring Tasks

In some cases, it's preferable for a task to configure itself rather than relying on external configuration. This is supported by implementing the `NamedTaskConfig` interface.

#### Implementation

Create a task that implements both the task interface and `NamedTaskConfig`:

```kotlin
/**
 * A task that is scheduled to run by default at 2am on the first of the month
 */
@ApplicationScoped
class JwkRotationTask(
    private val rotatingJwkService: RotatingJwkService
) : NamedScheduledTask, NamedTaskConfig {

    // Configuration properties can be overridden in application.yaml
    @ConfigProperty(name = "authn.jwks.rotation.max-age-duration")
    var maxAge: Duration = Duration.parse("P12W")  // 12 weeks default
    
    @ConfigProperty(name = "authn.jwks.rotation.task-cron-expression")
    var cronExpression = "0 0 2 1 * ? *"  // 2am on the first of each month
    
    override fun getName(): String {
        return "authn-jwk-rotation-task"
    }
    
    override fun run() {
        rotatingJwkService.rotateAndDeleteStaleJwks(Instant.now().plus(maxAge))
    }
    
    // Define the frequency directly in the task
    override fun frequency(): Optional<FrequencyConfig> {
        return Optional.of(FrequencyConfigData(cron = cronExpression))
    }
    
    // Define retry behavior directly in the task
    override fun onFailure(): Optional<RetryConfig> {
        return Optional.empty()  // No retry on failure
    }
    
    override fun onIncomplete(): Optional<RetryConfig> {
        return Optional.empty()  // No retry on incomplete
    }
}
```

#### Benefits of Self-Configuration

1. **Encapsulation**: Task configuration is defined alongside its implementation
2. **Default Values**: Sensible defaults can be provided but still overridden
3. **Type Safety**: Configuration is checked at compile time
4. **Documentation**: Configuration is documented in the code

This approach is supported for both periodic and jobbing tasks.

# Architecture

## Modules

This library is organized into several modules, each with a specific responsibility:

### scheduler-core

The foundation module containing:
- Core interfaces and models
- Configuration classes
- `TaskScheduler` interface definition
- Task abstractions (`NamedTask`, `NamedScheduledTask`, `NamedJobbingTask`)

**When to use**: Include this module when you only need the core interfaces, for example when defining tasks in a shared module.

### scheduler-db

A database implementation of the `TaskScheduler` interface that:
- Leverages the [db-scheduler](https://github.com/kagkarlsson/db-scheduler) open source library
- Provides persistence for tasks and their execution state
- Handles task scheduling, execution, and retries
- Supports JSON serialization of task payloads

**When to use**: Include this module when you need a persistent, database-backed scheduler.

### scheduler-quarkus

A Quarkus integration module that:
- Provides CDI integration for the scheduler
- Automatically registers tasks in the application context
- Exposes HTTP endpoints for task management
- Handles Quarkus-specific configuration

**When to use**: Include this module in Quarkus applications to get a fully configured scheduler with minimal setup.

### test-quarkus-scheduling

An example Quarkus application that:
- Demonstrates how to use the scheduler in a real application
- Contains integration tests for various task types
- Provides reference implementations of different task patterns

**When to use**: Reference this module for examples and patterns when implementing your own tasks.

## Design Principles

1. **Separation of Concerns**: Each module has a clear responsibility
2. **Extensibility**: Core interfaces can have multiple implementations
3. **Configuration over Code**: Most behavior can be configured without code changes
4. **Resilience**: Built-in retry mechanisms and failure handling
5. **Observability**: Tasks can be monitored and triggered manually

## Advanced Features

- **Transaction Support**: Tasks can be executed within transactions
- **Retry Policies**: Configurable retry behavior for failed tasks
- **Monitoring**: Task execution statistics and status information
- **Manual Triggering**: HTTP endpoints for manual task execution
- **Payload Serialization**: Support for complex payload objects

# Best Practices

## Task Design

1. **Keep Tasks Focused**: Each task should have a single responsibility
2. **Idempotency**: Design tasks to be idempotent (can be safely executed multiple times)
3. **Timeouts**: Consider setting appropriate timeouts for long-running tasks
4. **Error Handling**: Properly handle and log exceptions within tasks
5. **Payload Size**: Keep payloads small and serializable for jobbing tasks

## Configuration

1. **Naming Convention**: Use consistent, descriptive names for tasks
2. **Retry Strategy**: Configure appropriate retry intervals and max attempts
3. **Execution Frequency**: Don't schedule tasks too frequently to avoid overloading the system
4. **Resource Constraints**: Consider database connection pool size when configuring concurrent tasks

## Monitoring

1. **Logging**: Add appropriate logging in task implementations
2. **Metrics**: Monitor task execution times and failure rates
3. **Alerts**: Set up alerts for repeatedly failing tasks

# Troubleshooting

## Common Issues

### Tasks Not Running

1. Check that the task is properly registered as a CDI bean
2. Verify the task name in configuration matches the `getName()` implementation
3. Check database tables for task entries
4. Ensure the database schema is correctly configured

### Task Failures

1. Check logs for exceptions in task execution
2. Verify that retry configuration is appropriate
3. Test the task manually using the HTTP endpoint

### Database Issues

1. Ensure the database tables are created correctly
2. Check for database connection issues
3. Verify transaction isolation level is appropriate

## Debugging

Enable debug logging for the scheduler:

```yaml
quarkus:
  log:
    category:
      "org.incept5.scheduler":
        level: DEBUG
```


