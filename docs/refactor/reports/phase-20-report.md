# Phase 20 Report

## Completed

1. Hardened lease-release protocol to be idempotent:
   - `RemoteConfigClient#releaseGovernanceLease(...)` now tolerates `404/409` responses as non-fatal stale-token cases.
   - this prevents sync loop disruption when control-plane lease token has already expired or been replaced.

2. Refined HTTP post handling for governance APIs:
   - `RemoteConfigClient#postJson(...)` now supports tolerated non-2xx statuses for endpoint-specific contract semantics.
   - response-signature verification keeps strict behavior for successful (2xx) responses.

3. Added control-plane lease contract tests:
   - `RemoteConfigClientTest#shouldFollowLeaseAcquireReleaseContract`
   - verifies acquire-occupy-release-reacquire lifecycle against a stateful embedded server.

4. Added stale-token release compatibility test:
   - `RemoteConfigClientTest#shouldIgnoreStaleLeaseRelease`
   - verifies `409` on release does not throw and is treated as idempotent no-op.

## Verification

1. Targeted tests:
   - `mvn -q -pl sf-chain-legacy-starter -DskipITs -Dtest=RemoteConfigClientTest,RemoteConfigSyncServiceTest,GovernanceLeaseManagerTest test`
   - result: success

2. Module test suite:
   - `mvn -q -pl sf-chain-legacy-starter -DskipITs test`
   - result: success

## Remaining (for next phase)

- Add control-plane contract tests for finalize reconcile delta windows and ACK cursor progression.
- Externalize governance sync metrics to Micrometer/OpenTelemetry exporters (currently in-memory snapshot only).
- Add signed error-response contract alignment if control-plane requires signed non-2xx payloads.

## Exit Check

- Phase 20 objective delivered: lease protocol is now operationally safer under stale-token races, and client-side contract coverage for lease lifecycle is complete.
