# Phase 11 Report

## Completed

1. Added ingestion governance API:
   - `AICallLogGovernanceController`
   - `GET /v1/logs/ai-calls/index-maintenance/metrics`
   - `POST /v1/logs/ai-calls/index-maintenance/rebuild`
   - `POST /v1/logs/ai-calls/contract-allowlist/validate`

2. Added maintenance runtime metrics snapshot:
   - `IngestionIndexMaintenanceMetrics`
   - `IngestionIndexMaintenanceService#metrics()`
   - `IngestionIndexMaintenanceService#rebuildOnce()`
   - Tracks run/success/failure/rebuiltFileCount/avgDurationMs

3. Added contract allowlist guardrails:
   - `ContractAllowlistGuardService`
   - Enforces non-empty allowlist
   - Enforces max active versions
   - Optional overlap requirement with current allowlist

4. Added adaptive ingestion index stride:
   - `FileAICallLogIngestionStore#resolveStride(...)`
   - Stride expands with file size to reduce index overhead on large data files

5. Added test coverage:
   - `ContractAllowlistGuardServiceTest`
   - `IngestionIndexMaintenanceServiceTest`
   - `FileAICallLogIngestionStoreTest` (adaptive stride + batch rebuild)

## New Config Keys

```yaml
sf-chain:
  ingestion:
    adaptive-index-stride-enabled: true
    max-active-contract-versions: 2
    require-current-version-overlap: true
```

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Integrate governance endpoint with remote control plane pull/push workflow.
- Add rollout automation for allowlist updates (canary -> full rollout).
- Add centralized observability export bridge (metrics/logs) for multi-app deployments.

## Exit Check

- Local ingestion now exposes controllable maintenance and measurable runtime health.
- Contract allowlist changes can be pre-validated with safety constraints before rollout.
- Large-file index overhead is reduced by adaptive stride without changing ingestion contract.
