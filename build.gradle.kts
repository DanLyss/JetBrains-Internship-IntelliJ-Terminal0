plugins {
    kotlin("jvm") version "2.0.21"
}

group = "terminal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    outputs.upToDateWhen { false }
}

kotlin {
    jvmToolchain(21)
}
