# Kotlin AI Agent with Koog Framework

A multiplatform AI agent built with Kotlin Multiplatform (KMP) and JetBrains Koog framework. This agent can explore, read, modify code, and perform Git operations.

## Features

- **Kotlin Multiplatform**: Built with KMP for cross-platform support
- **Koog Framework**: Uses JetBrains' official AI agent framework
- **Project Exploration**: Lists directories and files
- **Code Reading**: Reads and analyzes files
- **Code Editing**: Modifies files based on tasks
- **Git Operations**: Status, diff, commit, branch management via MCP
- **OpenAI Integration**: Uses GPT-4 for intelligent decision-making
- **MCP Integration**: Extensible tool system using Model Context Protocol

## Available Tools

### File System Tools (Koog SimpleTool)
1. **list_directory**: Lists all files and directories in a path
2. **read_file**: Reads file contents
3. **edit_file**: Modifies files by replacing their content
4. **create_file**: Creates new files with specified content

### Git Tools (via MCP Server)
5. **git_status**: Show the working tree status
6. **git_diff**: Show changes between commits, commit and working tree
7. **git_commit**: Record changes to the repository
8. **git_log**: Show commit logs
9. **git_branch**: List, create, or delete branches
10. **git_checkout**: Switch branches or restore working tree files
11. **git_add**: Add file contents to the staging area

**Note**: Git tools are automatically discovered when the MCP Git Server is running. If the server is unavailable, the agent falls back to file system tools only.

## Prerequisites

- JDK 17 or higher
- Gradle
- OpenAI API Key
- (Optional) [MCP Git Server](https://github.com/hernandazevedo/mcp-git-server) running for Git operations

## Setup

1. Clone the repository

2. Configure your OpenAI API key:
```bash
export OPENAI_API_KEY="your-api-key-here"
```

Or create a `.env` file:
```
OPENAI_API_KEY=your-api-key-here
```

3. Build the project:
```bash
./gradlew build
```

4. (Optional) Start the MCP Git Server for Git operations:

First, clone and setup the [MCP Git Server](https://github.com/hernandazevedo/mcp-git-server):
```bash
git clone https://github.com/hernandazevedo/mcp-git-server.git
cd mcp-git-server
chmod +x mcp-git-server.sh
```

Then start the server:
```bash
# In a separate terminal
./gradlew run
# or
./mcp-git-server.sh http
```

The MCP server will start on `http://localhost:8080` by default.

## Usage

Run the agent by providing the project path and task:

```bash
./gradlew run --args="/path/to/project 'Your task here'"
```

### Examples

```bash
# Add a new function
./gradlew run --args="/Users/username/my-project 'Add a function to calculate Fibonacci numbers'"

# Refactor code
./gradlew run --args="/Users/username/my-project 'Refactor the UserService class to use dependency injection'"

# Fix bugs
./gradlew run --args="/Users/username/my-project 'Fix the bug in the authentication method'"

# With Git operations (requires MCP server running)
./gradlew run --args="/Users/username/my-project 'Fix the calculator bug and commit the changes with a descriptive message'"

# Check git status
./gradlew run --args="/Users/username/my-project 'Check the git status and tell me what files have changed'"
```

## Architecture

### Main Components

- **AIAgent (Koog)**: Orchestrates the main execution loop
- **simpleOpenAIExecutor (Koog)**: Interfaces with the OpenAI API
- **SimpleTool (Koog)**: Base class for creating tools
- **FileSystemProvider**: Abstraction for file system operations (KMP compatible)
- **FileSystem**: Platform-specific implementations (expect/actual pattern)

### MCP Integration Components

- **McpClient**: HTTP client for JSON-RPC communication with MCP servers
- **McpProtocol**: Data classes implementing MCP 2024-11-05 specification
- **McpToolAdapter**: Bridges MCP tools to Koog's SimpleTool interface
- **McpToolDiscovery**: Auto-discovers and registers tools from MCP servers

### Project Structure

```
src/
├── commonMain/kotlin/com/agents/
│   ├── FileSystemProvider.kt       # Common interface
│   └── tools/
│       └── FileSystemTools.kt      # Koog SimpleTool implementations
└── jvmMain/kotlin/com/agents/
    ├── FileSystemProvider.jvm.kt   # JVM implementation
    ├── Main.kt                     # Entry point with Koog AIAgent
    └── mcp/
        ├── McpClient.kt
        ├── McpProtocol.kt
        ├── McpToolAdapter.kt       # MCP → Koog adapter
        └── McpToolDiscovery.kt
```

### Execution Flow

1. The agent initializes with Koog's AIAgent
2. Connects to the MCP server (if available)
3. Auto-discovers Git tools from the MCP server
4. Receives a task and the project path
5. Explores the project using `list_directory`
6. Reads relevant files with `read_file`
7. Makes necessary modifications via `edit_file` or `create_file`
8. (Optional) Performs Git operations via MCP tools
9. Returns a summary of the changes made

## Configuration

### Environment Variables

- `OPENAI_API_KEY`: Your OpenAI API key (required)
- `MCP_SERVER_URL`: MCP server URL (default: `http://localhost:8080/mcp`)
- `GIT_WORKING_DIR`: Working directory for Git operations on MCP server side

### Example Configuration

```bash
export OPENAI_API_KEY="sk-..."
export MCP_SERVER_URL="http://localhost:8080/mcp"
export GIT_WORKING_DIR="/path/to/repo"
```

## Technology Stack

- **Kotlin Multiplatform**: Cross-platform development
- **Koog 0.6.0**: JetBrains AI agent framework
- **OpenAI GPT-4o**: Language model for intelligent decisions
- **OkHttp**: HTTP client for MCP communication
- **kotlinx.serialization**: JSON serialization
- **kotlinx.coroutines**: Asynchronous programming

## Advantages of KMP + Koog Migration

1. **Multiplatform Support**: Can now target JVM, JS, WasmJS, Android, and iOS
2. **Enterprise-Ready**: Built on JetBrains' proven AI framework
3. **Type-Safe Tools**: Koog provides type-safe tool definitions
4. **Better Reliability**: Built-in retries and fault-tolerance
5. **Observability**: Native support for tracing and monitoring
6. **Maintainability**: Cleaner architecture with Koog's DSL

## MCP Integration Details

This project implements **Full MCP Client Integration with Koog**, which:

- Follows the [Model Context Protocol](https://modelcontextprotocol.io/) specification (2024-11-05)
- Uses JSON-RPC 2.0 over HTTP for communication
- Auto-discovers tools from MCP servers at startup
- Adapts MCP tools to Koog's SimpleTool interface
- Gracefully degrades when MCP server is unavailable
- Supports multiple MCP servers (extensible architecture)

### Tool Discovery Process

1. Agent starts and attempts to connect to MCP server
2. Sends `initialize` request to establish connection
3. Sends `tools/list` request to discover available tools
4. Creates McpToolAdapter instances for each tool (wrapping as Koog SimpleTool)
5. Registers tools with the Koog agent
6. Tools are now available for the AI to use

## Limitations

- Git operations require MCP Git Server to be running
- Requires a valid OpenAI API key
- Write operations require read-write FileSystemProvider

## Next Steps

- Add more MCP servers (database, API tools, etc.)
- Add more platforms (Android, iOS, JS)
- Implement platform-specific file system providers
- Add code verification
- Improve error handling and retry logic
- Create comprehensive integration tests
- Add support for more LLMs

## References

- [Koog Framework](https://docs.koog.ai/)
- [JetBrains Koog GitHub](https://github.com/JetBrains/koog)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [MCP Git Server](https://github.com/hernandazevedo/mcp-git-server)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)

## License

Apache 2.0 (following Koog framework license)
