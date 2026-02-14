#!/bin/bash

# Script to run the Kotlin AI Agent in interactive mode with proper stdin handling
# This ensures the interactive prompts work correctly

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🤖 Kotlin AI Agent - Interactive Mode${NC}"
echo ""

# Check if arguments provided
if [ "$#" -lt 2 ]; then
    echo "Usage: ./run-interactive.sh <project-path> <task> [options]"
    echo ""
    echo "Examples:"
    echo "  ./run-interactive.sh example-project 'Add error handling to Calculator'"
    echo "  ./run-interactive.sh /path/to/project 'Refactor UserService' --user dev@example.com"
    echo ""
    echo "Options:"
    echo "  --user <userId>  Set user ID for Langfuse tracking"
    echo ""
    exit 1
fi

PROJECT_PATH="$1"
TASK="$2"
shift 2
EXTRA_ARGS="$@"

# Check if project exists
if [ ! -d "$PROJECT_PATH" ]; then
    echo -e "${YELLOW}⚠️  Warning: Project path '$PROJECT_PATH' does not exist${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${GREEN}📁 Project:${NC} $PROJECT_PATH"
echo -e "${GREEN}📝 Task:${NC} $TASK"
echo ""

# Build first (suppress output unless error)
echo -e "${GREEN}🔨 Building project...${NC}"
if ! ./gradlew build > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Build failed, trying to continue anyway...${NC}"
fi

# Create run script
echo -e "${GREEN}📦 Creating run script...${NC}"
./gradlew createRunScript > /dev/null 2>&1

echo ""
echo -e "${GREEN}🚀 Starting agent in interactive mode...${NC}"
echo -e "${YELLOW}💡 You'll be prompted to approve each file operation with a diff preview${NC}"
echo ""

# Run the agent with stdin properly connected
./build/bin/kotlin-ai-agent-koog "$PROJECT_PATH" "$TASK" --interactive $EXTRA_ARGS
