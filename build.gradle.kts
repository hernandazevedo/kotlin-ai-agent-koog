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
                // Koog AI Agent Framework - Updated to 0.6.2
                implementation("ai.koog:koog-agents:0.6.2")

                // Koog Features - Observability
                implementation("ai.koog:agents-features-opentelemetry:0.6.2")
                implementation("ai.koog:agents-features-trace:0.6.2")

                // Koog Features - Memory & History Compression
                implementation("ai.koog:agents-features-memory:0.6.2")

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

                // LLM Clients for Multi-Provider Support
                implementation("ai.koog:prompt-executor-anthropic-client:0.6.2")
                implementation("ai.koog:prompt-executor-openai-client:0.6.2")
                implementation("ai.koog:prompt-executor-llms:0.6.2")
                implementation("ai.koog:agents-ext-jvm:0.6.2")

            }
        }

        val jvmTest by getting
    }
}
