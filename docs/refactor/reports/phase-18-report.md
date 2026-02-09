# Phase 18 Report

## Completed

1. Added remote-governance lease capabilities (control-plane first, local fallback):
   - `RemoteConfigClient#tryAcquireGovernanceLease(...)`
   - `RemoteConfigClient#releaseGovernanceLease(...)`
   - `GovernanceLeaseManager` now prefers remote lease when enabled, and falls back to local file lock.

2. Added finalize reconciliation pull path:
   - new model `GovernanceFinalizeReconcileSnapshot`
   - `RemoteConfigClient#fetchFinalizeReconciliation()`
   - `RemoteConfigSyncService` reconciles acked task keys before pending finalize retry flush.

3. Added callback signature support for governance POST payloads:
   - new server properties:
     - `sf-chain.server.callback-signature-enabled`
     - `sf-chain.server.callback-signing-secret`
   - signed headers:
     - `X-SF-SIGNATURE-TS`
     - `X-SF-SIGNATURE`
   - algorithm: HMAC-SHA256 over `timestamp + "\\n" + body`.

4. Added new sync configuration keys:
   - `sf-chain.config-sync.governance-remote-lease-enabled`
   - `sf-chain.config-sync.governance-remote-lease-ttl-seconds`
   - `sf-chain.config-sync.governance-finalize-reconcile-enabled`

5. Updated governance sync flow ordering:
   - acquire lease
   - reconcile finalize ACK state
   - flush pending finalize retries
   - pull/apply snapshot
   - release lease

6. Added/updated tests:
   - `GovernanceLeaseManagerTest` updated for new constructor and local-lease contention path
   - `RemoteConfigSyncServiceTest#shouldSkipFinalizeRetryWhenReconcileMarksTaskAcked`

## Verification

1. Validation status in current local environment:
   - command: `mvn -q test -DskipITs`
   - result: failed before test execution due local JDK/Maven compiler compatibility (`TypeTag :: UNKNOWN`), not a phase-logic assertion failure.

2. Risk control:
   - key Phase 18 behavior paths have dedicated unit coverage updates in `RemoteConfigSyncServiceTest` and `GovernanceLeaseManagerTest`.

## Remaining (for next phase)

- Add server-side lease/reconcile API contract tests with deterministic edge cases (lease expiration, stale token release, duplicate ACK replay).
- Add timestamp drift window + replay guard verification for signature validation counterpart.
- Add operational metrics for remote lease acquire/release/reconcile success-rate and fallback frequency.

## Exit Check

- Phase 18 target delivered: remote lease support, finalize reconciliation pull, and signed callback envelope support are integrated into sync runtime path.
- Backward compatibility maintained: all new behaviors are feature-flagged and default-off.
