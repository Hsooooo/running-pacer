# CHANGELOG - 2026-01-16

## Summary
- Added an in-process MCP server with SSE transport to expose running data tools, resources, and prompts.
- Refactored MCP tools architecture to "Purpose-Driven" model (reduced from 6 tools to 2 consolidated tools).
- Introduced Coaching Service with VDOT calculation, TSS tracking, and goal-based training plans.
- Expanded database schema to support Race Goals and Training Load analysis.

## Added
- **MCP Core**:
  - `src/main/kotlin/io/hansu/pacer/mcp/RunningPacerMcpServer.kt`: MCP server bootstrap.
  - `src/main/kotlin/io/hansu/pacer/config/McpServerConfig.kt`: WebMVC SSE transport configuration.
- **New Purpose-Driven Tools**:
  - `src/main/kotlin/io/hansu/pacer/mcp/tools/RunningQueryTool.kt`: Unified data access tool (activities, summary, anomalies).
  - `src/main/kotlin/io/hansu/pacer/mcp/tools/RunningInsightTool.kt`: Unified analysis tool (compare, trend, coaching, goal setting).
- **Coaching & Analysis**:
  - `src/main/kotlin/io/hansu/pacer/service/CoachingService.kt`: Core logic for VDOT/TSS based coaching.
  - `src/main/kotlin/io/hansu/pacer/service/VdotCalculator.kt`: Jack Daniels' formula implementation.
  - `src/main/kotlin/io/hansu/pacer/service/TssCalculator.kt`: Training Stress Score calculation.
  - `src/main/kotlin/io/hansu/pacer/service/RunningToolService.kt`: Orchestrator for tool requests.
- **Domain & Schema**:
  - `src/main/resources/db/migration/V5__create_race_goals_and_training_loads.sql`: New tables for goals and load tracking.
  - `RaceGoalEntity`, `TrainingLoadEntity` and repositories.

## Changed
- **Tool Architecture**: Deprecated and removed dispersed tools (`RunningMcpTools.kt`) in favor of unified `RunningQueryTool` and `RunningInsightTool`.
- **Query Service**: `RunningQueryService` updated to support `userId` scoping.
- **Dependencies**: Added `jackson-datatype-jsr310` for proper LocalDate serialization.
- **Null Safety**: Enhanced `RunningToolService` to handle missing parameters with sensible defaults (e.g., last 30 days for performance analysis).

## Testing
- Validated MCP SSE endpoint (`/sse`) connectivity via curl.
- Confirmed JSON serialization of new DTOs.
- Verified robust parameter handling for MCP tool calls.
