# Phase 5 Report

## Completed

1. Added control-plane log ingestion API (batch):
   - `AICallLogIngestionController`
   - Endpoint: `POST /v1/logs/ai-calls/batch`
   - Header auth: `X-SF-API-KEY`
   - Batch size guard and input validation

2. Added ingestion feature configuration domain:
   - `SfChainIngestionProperties`
   - `sf-chain.ingestion.enabled`
   - `sf-chain.ingestion.api-key`
   - `sf-chain.ingestion.max-batch-size`

3. Added ingestion auto-configuration:
   - `SfChainLogIngestionAutoConfiguration`
   - Activation condition:
     - `sf-chain.enabled=true`
     - `sf-chain.features.management-api=true`
     - `sf-chain.ingestion.enabled=true`
   - Registered in:
     - `AutoConfiguration.imports`
     - `spring.factories`

4. Added upload-side observability snapshot:
   - `AICallLogUploadStats`
   - `AICallLogUploadGateway#stats()`
   - `AsyncAICallLogUploader` now tracks:
     - queue size
     - sampled-out count
     - dropped count
     - upload success count
     - upload failed count

5. Added management query endpoint for upload metrics:
   - `GET ${sf-chain.path.api-prefix}/ai-logs/upload-statistics`

6. Hardened upload JSON serialization compatibility:
   - `HttpAICallLogUploadClient` now uses mapper copy with `findAndRegisterModules()`
   - Ensures `LocalDateTime` payloads can be serialized in non-Spring test/runtime contexts

7. Added test coverage:
   - `AICallLogIngestionControllerTest`
   - `AsyncAICallLogUploaderTest`
   - `HttpAICallLogUploadClientTest` (local HTTP server integration)

## New Config Keys

```yaml
sf-chain:
  ingestion:
    enabled: false
    api-key: ${SF_CHAIN_INGESTION_API_KEY}
    max-batch-size: 500
```

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Define unified persistence strategy for control-plane logs (DB or append-only storage), not only in-memory cache.
- Introduce tenant/app level isolation and quota controls for ingestion.
- Add end-to-end contract tests between client uploader and control-plane APIs (config + logs together).

## Exit Check

- Control-plane can receive batched AI-call logs with API key verification.
- Client-side upload path has measurable runtime statistics.
- Phase 4 upload pipeline and Phase 5 ingestion API can interoperate via `/v1/logs/ai-calls/batch`.
