plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.quarkus)
}

dependencies {

    api("io.quarkus:quarkus-core")
    api("jakarta.enterprise:jakarta.enterprise.cdi-api")
    api("jakarta.inject:jakarta.inject-api")
    api("jakarta.transaction:jakarta.transaction-api")
    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api(project(":scheduler-core"))
    api(project(":scheduler-quarkus"))

    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    runtimeOnly("io.quarkus:quarkus-arc")
    implementation(libs.quarkus.rest.jackson)
    runtimeOnly("io.quarkus:quarkus-jdbc-postgresql")
    runtimeOnly("io.quarkus:quarkus-config-yaml")
    runtimeOnly("io.quarkus:quarkus-flyway")
    
    // Security dependencies for testing
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-elytron-security-properties-file")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.hamcrest:hamcrest")
    testImplementation("org.junit.jupiter:junit-jupiter-api")

}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Set the system property for all test-related tasks
tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// Also set it for the JVM running the build
tasks.withType<JavaExec> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}
