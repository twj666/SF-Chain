# Phase 6 Report

## Completed

1. Added control-plane ingestion persistence strategy (append-only file):
   - `AICallLogIngestionStore`
   - `FileAICallLogIngestionStore`
   - Writes JSONL by `tenantId + appId` partition
   - Persistence failure is fail-open (does not block ingest main flow)

2. Added tenant/app isolation constraints in ingestion path:
   - `sf-chain.ingestion.require-tenant-app` (default: true)
   - Rejects requests without `tenantId` or `appId` when enabled

3. Added per-tenant-app quota control:
   - `MinuteWindowQuotaService`
   - Fixed minute window counter keyed by `tenantId|appId`
   - Enforced in ingestion API before write/accept
   - Returns HTTP 429 when exceeded

4. Extended ingestion configuration:
   - `sf-chain.ingestion.per-tenant-app-per-minute-limit`
   - `sf-chain.ingestion.file-persistence-enabled`
   - `sf-chain.ingestion.file-persistence-dir`

5. Updated ingestion auto-configuration:
   - `SfChainLogIngestionAutoConfiguration` now wires:
     - quota service
     - file ingestion store (or NO_OP fallback)

6. Added and updated test coverage:
   - `AICallLogIngestionControllerTest`
     - API key failure
     - success path
     - missing tenant/app rejection
     - quota exceeded rejection
   - `FileAICallLogIngestionStoreTest`
     - verifies JSONL persistence by tenant/app file partition

## New Config Keys

```yaml
sf-chain:
  ingestion:
    require-tenant-app: true
    per-tenant-app-per-minute-limit: 5000
    file-persistence-enabled: true
    file-persistence-dir: .sf-chain/ingestion-logs
```

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Add retrieval/query API for persisted control-plane logs (tenant/app scoped).
- Add compaction/retention strategy for JSONL files.
- Add full end-to-end contract test that starts control-plane endpoint and verifies uploader -> ingestion -> persistence.

## Exit Check

- Ingestion path now has tenant/app isolation + quota + persistent storage capability.
- Existing upload pipeline remains compatible and can continue fail-open behavior.
