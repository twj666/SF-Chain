# Phase 19 Report

## Completed

1. Added control-plane response signature verification (optional, default-off):
   - `RemoteConfigClient` now verifies `X-SF-SIGNATURE-TS` and `X-SF-SIGNATURE` on successful responses.
   - covered paths: snapshot pull, governance POST responses, finalize reconcile pull.

2. Added replay defense and timestamp window checks:
   - timestamp skew guard via configurable max skew seconds.
   - in-memory replay key cache (`timestamp + signature`) with rolling expiration window.
   - failed signature checks remove provisional replay key to avoid false-positive lockout.

3. Added server property extensions for protocol hardening:
   - `sf-chain.server.response-signature-enabled`
   - `sf-chain.server.response-signing-secret`
   - `sf-chain.server.response-signature-max-skew-seconds`
   - `sf-chain.server.response-signature-replay-window-seconds`

4. Added governance sync metrics snapshot and recorder path:
   - new value object: `GovernanceSyncMetricsSnapshot`
   - `RemoteConfigSyncService#metrics()` exposes runtime counters.
   - counters include:
     - sync run/failure
     - lease acquire attempts/success
     - lease remote/local success split
     - finalize reconcile attempts/success/failure
     - finalize retry attempts/success/failure

5. Added lease acquisition mode tracing:
   - `GovernanceLeaseManager` now exposes `AcquireMode` (`DISABLED/REMOTE/LOCAL/NONE`).
   - sync metrics can distinguish remote lease hit vs local fallback.

6. Added/updated tests for Phase 19 behavior:
   - new `RemoteConfigClientTest`:
     - signed response accepted
     - duplicate signed response rejected as replay
   - `RemoteConfigSyncServiceTest#shouldRecordRemoteLeaseAndReconcileMetrics`
   - `GovernanceLeaseManagerTest` now asserts acquire mode.

## Verification

1. Module-level test run passed with JDK 17:
   - command:
     - `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`
     - `export PATH="$JAVA_HOME/bin:$PATH"`
     - `mvn -q -pl sf-chain-legacy-starter -DskipITs test`
   - result: success

2. Note on root build environment:
   - root-level `mvn` under non-17 JDK still has pre-existing compiler compatibility issue (`TypeTag :: UNKNOWN`).
   - this is environment/JDK selection related and not introduced by Phase 19 logic.

## Remaining (for next phase)

- Implement control-plane server-side signature verification middleware aligned with the same timestamp/replay policy.
- Add contract tests for lease expiration and stale-token release behavior on the configuration-center API side.
- Export governance metrics to a standardized telemetry sink (Micrometer/OpenTelemetry) with alert thresholds.

## Exit Check

- Phase 19 objective delivered: protocol hardening (response integrity + anti-replay) and governance observability (lease/reconcile/retry metrics) are now in place, and all additions are feature-flagged for safe rollout.
