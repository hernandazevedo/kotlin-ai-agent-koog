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

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                // HTTP Client for MCP
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
            }
        }

        val jvmTest by getting
    }
}
