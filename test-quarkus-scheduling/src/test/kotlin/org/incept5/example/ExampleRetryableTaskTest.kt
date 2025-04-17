package org.incept5.example

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class ExampleRetryableTaskTest {

    @Test
    fun testTaskScheduling() {

        // check count is 0
        given()
            .`when`().get("/example-retryable-task")
            .then()
            .statusCode(200)
            .body(`is`("Count is now: 0"))

        // schedule a task
        given()
            .`when`().put("/example-retryable-task/schedule")
            .then()
            .statusCode(204)

        // wait for count to become 3
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            given()
                .`when`().get("/example-retryable-task")
                .then()
                .statusCode(200)
                .body(`is`("Count is now: 3"))
        }

        // wait at least 5 seconds to ensure the task is not scheduled
        Thread.sleep(5000)

        // should still be 3
        given()
            .`when`().get("/example-retryable-task")
            .then()
            .statusCode(200)
            .body(`is`("Count is now: 3"))
    }

}