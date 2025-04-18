package org.incept5.example

import org.incept5.example.tasks.ExampleRecurringTask
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class ExampleRecurringTaskTest {

    @Test
    @TestSecurity(user = "admin", roles = ["platform_admin"])
    fun `recurring task can be triggered via http POST with platform_admin role`() {
        // debug logging for restassured
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        val taskName = "example-recurring-task"
        val count = ExampleRecurringTask.count.get()

        // POST with admin role (provided by @TestSecurity annotation)
        val response = RestAssured.given()
            .post("/api/scheduler/recurring-tasks/$taskName")

        assertThat(response.statusCode, CoreMatchers.equalTo(200))

        // wait for our subscriber to pick up the target file
        Awaitility.await().atMost(Duration.ofSeconds(20)).until {
            ExampleRecurringTask.count.get() > count
        }
    }

    @Test
    @TestSecurity(user = "user", roles = ["user"])
    fun `user without required role cannot trigger the recurring task`() {
        // debug logging for restassured
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        val taskName = "example-recurring-task"

        // POST with user role (provided by @TestSecurity annotation)
        val response = RestAssured.given()
            .post("/api/scheduler/recurring-tasks/$taskName")

        // Should get a 403 Forbidden (not 401 Unauthorized since they are authenticated but not authorized)
        assertThat(response.statusCode, CoreMatchers.equalTo(403))
    }
    
    @Test
    @TestSecurity(user = "scheduler_admin", roles = ["scheduler_admin"])
    fun `scheduler_admin role can also trigger the recurring task`() {
        // debug logging for restassured
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        val taskName = "example-recurring-task"
        val count = ExampleRecurringTask.count.get()

        // POST with scheduler_admin role (provided by @TestSecurity annotation)
        val response = RestAssured.given()
            .post("/api/scheduler/recurring-tasks/$taskName")

        assertThat(response.statusCode, CoreMatchers.equalTo(200))

        // wait for our subscriber to pick up the target file
        Awaitility.await().atMost(Duration.ofSeconds(20)).until {
            ExampleRecurringTask.count.get() > count
        }
    }
}