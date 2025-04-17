package org.incept5.example

import org.incept5.example.tasks.ExampleRecurringTask
import io.quarkus.test.junit.QuarkusTest
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
    fun `recurring task can be triggered via http POST`() {

        // debug logging for restassured
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        val taskName = "example-recurring-task"
        val count = ExampleRecurringTask.count.get()

        // POST the file to the server

        val response = RestAssured.given()
            .header("Authorization","Bearer backoffice-admin-token")
            .post("/api/scheduler/recurring-tasks/$taskName")

        assertThat(response.statusCode, CoreMatchers.equalTo(200))

        // wait for our subscriber to pick up the target file
        Awaitility.await().atMost(Duration.ofSeconds(20)).until {
            ExampleRecurringTask.count.get() > count
        }
    }

    @Test
    fun `backoffice admin role is required to trigger the recurring task`() {

        // debug logging for restassured
        RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())

        val taskName = "example-recurring-task"
        val count = ExampleRecurringTask.count.get()

        // POST the file to the server

        val response = RestAssured.given()
            .header("Authorization","Bearer user-token")
            .post("/api/scheduler/recurring-tasks/$taskName")

        assertThat(response.statusCode, CoreMatchers.equalTo(401))
    }
}