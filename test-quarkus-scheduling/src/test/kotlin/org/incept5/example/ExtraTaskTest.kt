package org.incept5.example

import org.incept5.example.tasks.ExtraTask
import io.quarkus.test.junit.QuarkusTest
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import java.time.Duration

@QuarkusTest
class ExtraTaskTest {

    @Test
    fun testExtraTasks() {
        // wait for tasks to run
        Awaitility.await().atMost(Duration.ofSeconds(10)).until {
            ExtraTask.count > 0
        }
    }

}