plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    // publish to nexus
    `maven-publish`
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // db scheduler dependencies
    api(libs.db.scheduler)

    // jakarta dependencies
    implementation(platform(libs.jakarta.bom))
    api("jakarta.annotation:jakarta.annotation-api") // PreDestroy will shut down the scheduler

    // jackson
    implementation(platform(libs.jackson.bom))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-core")


    // core dependencies
    implementation(libs.incept5.correlation)
    api(project(":scheduler-core"))

    // testing dependencies
    testRuntimeOnly(libs.slf4j.simple) // slf4j backend
    testImplementation(libs.hsqldb)
    testImplementation(libs.kotlin.logging)

    // ko test dependencies
    testImplementation(platform(libs.kotest.bom))
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-assertions-shared")
    testImplementation("io.kotest:kotest-framework-api")
    testRuntimeOnly("io.kotest:kotest-runner-junit5")

}


tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // For JitPack compatibility, we need to use the correct group ID format
            // JitPack expects: com.github.{username}.{repository}
            val publishGroupId = rootProject.properties["publishGroupId"]?.toString()
                ?: if (System.getenv("JITPACK") != null) {
                    // When building on JitPack
                    "com.github.incept5.scheduler-lib"
                } else {
                    // For local development
                    "com.github.incept5"
                }

            // Explicitly set the coordinates
            groupId = publishGroupId
            artifactId = "scheduler-db"
            version = project.version.toString()

            from(components["java"])

            // POM information
            pom {
                name.set("scheduler Core")
                description.set("Core functionality for scheduling in Rest Services")
                url.set("https://github.com/incept5/scheduler-lib")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("incept5")
                        name.set("Incept5")
                        email.set("info@incept5.com")
                    }
                }

                // Important for JitPack to resolve dependencies correctly
                scm {
                    connection.set("scm:git:github.com/incept5/scheduler-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/incept5/scheduler-lib.git")
                    url.set("https://github.com/incept5/scheduler-lib/tree/main")
                }
            }
        }
    }
}

// For JitPack compatibility
tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

// Always publish to local Maven repository after build for local development
tasks.named("build") {
    finalizedBy(tasks.named("publishToMavenLocal"))
}
