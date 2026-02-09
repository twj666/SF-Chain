# SF-Chain Ingestion Contract Evolution Policy

## Versioning Model

1. Contract version is explicit in request payload field `contractVersion`.
2. Server validates version against allowlist:
   - `sf-chain.ingestion.supported-contract-version`
   - `sf-chain.ingestion.supported-contract-versions`
3. Unsupported versions return `400`.

## Compatibility Rules

1. Additive change (safe):
   - New optional fields can be added to request schema.
2. Breaking change (unsafe):
   - Remove/rename existing fields
   - Change field type/semantic
3. Breaking change requires new major contract version (`v2`, `v3`, ...).

## Rollout Strategy

1. Producer-first:
   - Server allowlist includes both old and new versions.
2. Client migration:
   - Roll out clients gradually to new version.
3. Cleanup:
   - Remove old version from allowlist only after migration completes.

## Operational Controls

1. Keep at most two active versions in production at a time.
2. Keep mixed-version compatibility tests in CI for active versions.
3. Record accepted contract version in ingestion response and logs for audit.
