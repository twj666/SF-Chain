# Baseline Capabilities (Phase 0)

## Scope
This baseline captures the behavior before Phase 1 decoupling changes, and serves as the regression target.

## Core Runtime
- `AIService.execute(...)` supports operation lookup and execution.
- `AIService.executeStream(...)` supports reactive stream output.
- `@AIOp` operations are registered through `BaseAIOperation.init()`.
- Operation model mapping comes from `ai.operations.*` and runtime override.

## Model Layer
- OpenAI-compatible model registration is loaded from `ai.openai-models.models.*`.
- Model instances are created by `OpenAIModelFactory`.

## Logging
- `AICallLogAspect` tracks operation execution.
- `AICallLogManager` stores and aggregates logs in-memory.

## Management & Persistence (pre-decoupling)
- Controllers can be loaded into consumer app via framework component scanning.
- Local persistence and migration can be activated by persistence configuration.

## Decoupling Targets
- Core capability available without management API or local persistence.
- Management API and persistence become feature-flag controlled.
- No framework-wide `@ComponentScan` in auto-configuration.
