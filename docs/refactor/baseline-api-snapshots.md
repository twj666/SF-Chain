# Baseline API Snapshots (Phase 0)

## Core Beans
- `AIOperationRegistry`
- `ModelRegistry`
- `AIService`
- `ChatContextService`
- `AICallLogManager`

## Configuration Domains
- `sf-chain.*`
- `sf-chain.path.*`
- `sf-chain.logging.*`
- `sf-chain.persistence.*`
- `ai.openai-models.*`
- `ai.operations.*`

## Runtime Expectations
- Core AI invocation works even when management API is absent.
- Built-in operations remain available:
  - `json-repair`
  - `model-validation`

## New Feature Flags Introduced in Phase 1
- `sf-chain.features.management-api`
- `sf-chain.features.local-persistence`
- `sf-chain.features.local-migration`
- `sf-chain.features.static-ui`
