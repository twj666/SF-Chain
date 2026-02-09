# Phase 9 Report

## Completed

1. Added index-accelerated cursor seek for large log files:
   - `FileAICallLogIngestionStore` now supports line-offset index sidecar (`*.jsonl.idx.json`)
   - Cursor query uses nearest index checkpoint + local skip, reducing full-file scan cost
   - Index is invalidated on append and lazily rebuilt on next paged query

2. Added multi-version contract compatibility:
   - Ingestion config supports:
     - `supported-contract-version` (single)
     - `supported-contract-versions` (list)
   - Controller merges both definitions into a version allowlist
   - Upload request version check now supports compatibility windows (e.g., `v1` + `v2`)

3. Added configuration for index behavior:
   - `sf-chain.ingestion.index-enabled`
   - `sf-chain.ingestion.index-stride`

4. Improved lifecycle behavior:
   - Retention cleanup also deletes corresponding index sidecar files
   - Non-data files are excluded from retention delete loop

5. Updated tests for compatibility matrix and indexed paging:
   - `AICallLogIngestionControllerTest`
     - unsupported version rejection
     - supported list version acceptance (`v2`)
   - `FileAICallLogIngestionStoreTest`
     - verifies paged query and index sidecar generation

## New Config Keys

```yaml
sf-chain:
  ingestion:
    supported-contract-versions: [v1, v2]
    index-enabled: true
    index-stride: 200
```

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Add optional background index rebuild task for very large files.
- Add request/response schema evolution policy document for v2+ contracts.
- Add end-to-end mixed-client compatibility tests (v1/v2 concurrently writing).

## Exit Check

- Cursor pagination now supports index-accelerated seek.
- Ingestion contract validation supports multi-version compatibility.
- Compatibility matrix scenarios are covered by tests.
