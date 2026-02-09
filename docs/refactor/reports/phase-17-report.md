# Phase 17 Report

## Completed

1. Added governance lease lock for multi-instance arbitration:
   - `GovernanceLeaseManager` (file-lock based lease)
   - `RemoteConfigSyncService` acquires lease per sync cycle before governance apply/finalize paths
   - prevents concurrent governance arbitration from multiple running instances sharing the same lock target

2. Standardized finalize ACK protocol:
   - new `GovernanceFinalizeAck` with fields:
     - `acknowledged`
     - `ackId`
     - `ackVersion`
     - `serverTimeEpochMs`
   - `RemoteConfigClient#pushGovernanceFinalize(...)` now returns structured ACK

3. Extended finalize state model for reconciliation:
   - new `GovernanceFinalizeRecord` for finalized status + ack metadata
   - `GovernanceSyncRuntimeState.finalizedStates` now stores `GovernanceFinalizeRecord`

4. Added pending finalize retry scheduling:
   - `GovernanceFinalizeTask` now tracks retry metadata (`retryCount`, `nextAttemptAtEpochMs`, `updatedAtEpochMs`)
   - `RemoteConfigSyncService` retries unacked finalize tasks on next sync loops

5. Added runtime state compaction/retention policy:
   - max size limits for finalized and pending maps
   - oldest entries are compacted when exceeding limits
   - controlled by:
     - `governance-state-max-finalized`
     - `governance-state-max-pending`

6. Added idempotency and protocol payload enhancements:
   - finalize payload includes deterministic `idempotencyKey`
   - supports control-plane cross-node deduplication

7. Added configuration keys:
   - `sf-chain.config-sync.governance-lease-enabled`
   - `sf-chain.config-sync.governance-lease-file`
   - `sf-chain.config-sync.governance-finalize-retry-seconds`
   - `sf-chain.config-sync.governance-state-max-finalized`
   - `sf-chain.config-sync.governance-state-max-pending`

8. Added tests:
   - `GovernanceLeaseManagerTest`
   - `GovernanceSyncStateStoreTest`
   - updated `RemoteConfigSyncServiceTest` for structured ACK + retry behavior

## Verification

1. Full reactor test:
   - `mvn test -DskipITs`
   - Result: success

## Remaining (for next phase)

- Replace file-lock lease with distributed lease backend (Redis/DB) for stronger cross-host arbitration.
- Introduce control-plane reconciliation API for missed ACK replay and explicit ack cursor checkpoints.
- Add signed callback envelope and integrity verification for finalize/event delivery hardening.

## Exit Check

- Governance arbitration now has lease protection for multi-instance contention.
- Finalize callback protocol has explicit ACK metadata and retryable reconciliation path.
- Runtime state has bounded growth with compaction policy and durable recovery.
