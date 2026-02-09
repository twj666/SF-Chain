# Phase 7 Report

## Completed

1. Added persisted log query API (tenant/app scoped):
   - `GET /v1/logs/ai-calls/records?tenantId=...&appId=...&limit=...`
   - API key protected (`X-SF-API-KEY`)
   - Query limit constrained by config

2. Added retention cleanup API:
   - `DELETE /v1/logs/ai-calls/records/expired`
   - API key protected (`X-SF-API-KEY`)
   - Returns deleted file count

3. Extended ingestion store contract:
   - `AICallLogIngestionStore#query(...)`
   - `AICallLogIngestionStore#purgeExpired()`
   - Added DTO: `AICallLogIngestionRecord`

4. Implemented file-store query and retention strategy:
   - `FileAICallLogIngestionStore#query(...)` reads latest records from tenant/app partition file
   - `FileAICallLogIngestionStore#purgeExpired()` deletes expired files by last-modified timestamp

5. Extended ingestion configuration:
   - `sf-chain.ingestion.retention-days`
   - `sf-chain.ingestion.max-query-limit`

6. Added end-to-end contract test:
   - `AICallLogIngestionContractE2ETest`
   - Starts control-plane HTTP endpoint in embedded server
   - Uses `HttpAICallLogUploadClient` to upload batch
   - Verifies ingestion persistence file contains uploaded call id

7. Added/updated tests:
   - `AICallLogIngestionControllerTest` (query + purge)
   - `FileAICallLogIngestionStoreTest` (query + purge retention)
   - `AICallLogIngestionContractE2ETest` (uploader -> endpoint -> persistence)

## New Config Keys

```yaml
sf-chain:
  ingestion:
    retention-days: 7
    max-query-limit: 500
```

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Add index-based query acceleration for large ingestion files.
- Add paging/seek cursor API instead of limit-only reads.
- Add stricter contract versioning between uploader and control-plane APIs.

## Exit Check

- Persisted ingestion logs are now queryable by tenant/app.
- Retention cleanup mechanism is available and test-covered.
- End-to-end upload contract is validated with a real HTTP control-plane endpoint.
