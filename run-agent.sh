#!/bin/bash
./gradlew createRunScript -q
./build/bin/kotlin-ai-agent-koog "$@"