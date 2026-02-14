plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.agents"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
        @Suppress("OPT_IN_USAGE")
        mainRun {
            mainClass.set("com.agents.MainKt")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Koog AI Agent Framework
                implementation("ai.koog:koog-agents:0.6.1")

                // Koog Features - Observability
                implementation("ai.koog:agents-features-opentelemetry:0.6.1")
                implementation("ai.koog:agents-features-trace:0.6.1")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }

        val jvmMain by getting {
            dependencies {
                // HTTP Client for MCP
                implementation("com.squareup.okhttp3:okhttp:4.12.0")

                // JLine for interactive terminal UI
                implementation("org.jline:jline:3.27.1")
            }
        }

        val jvmTest by getting
    }
}

// Create a task to generate a wrapper script that runs via java -cp
tasks.register("createRunScript") {
    dependsOn("jvmJar")
    doLast {
        val scriptFile = file("build/bin/kotlin-ai-agent-koog")
        scriptFile.parentFile.mkdirs()

        val classpath = configurations.getByName("jvmRuntimeClasspath")
            .files
            .joinToString(":") { it.absolutePath } +
            ":" + kotlin.jvm().compilations.getByName("main").output.classesDirs.asPath

        scriptFile.writeText("""#!/bin/bash
exec java -cp "$classpath" com.agents.MainKt "$@"
""")
        scriptFile.setExecutable(true)

        println("Created executable script at: ${scriptFile.absolutePath}")
    }
}
