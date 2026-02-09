# Mainline Phase 1 Report

## Goal

Build a truly usable minimal starter baseline (not only dependency exclusions), so consumers can safely adopt SF-Chain without loading web/persistence/ingestion heavy auto-config paths.

## Completed

1. Added lite auto-configuration filter to hard-block heavy legacy auto-config classes:
   - `LiteAutoConfigurationImportFilter`
   - blocks:
     - `SfChainPersistenceAutoConfiguration`
     - `SfChainManagementAutoConfiguration`
     - `SfChainPersistenceManagementAutoConfiguration`
     - `SfChainStaticUiAutoConfiguration`
     - `SfChainLogIngestionAutoConfiguration`

2. Registered filter in lite module Spring metadata:
   - `META-INF/spring.factories`
   - key: `org.springframework.boot.autoconfigure.AutoConfigurationImportFilter`

3. Added build-time dependency guardrails for lite module:
   - Maven Enforcer banned transitive dependencies:
     - `spring-boot-starter-web`
     - `spring-boot-starter-webflux`
     - `spring-boot-starter-data-jpa`
     - `spring-boot-starter-jdbc`
     - `mysql-connector-j`
     - `postgresql`

4. Added lite module tests:
   - `LiteAutoConfigurationImportFilterTest`
   - verifies heavy auto-configs are filtered while core/remote paths remain available.

5. Added test dependency for lite module:
   - `spring-boot-starter-test` (test scope only)

## Verification

1. Lite module tests passed:
   - `mvn -q -pl sf-chain-spring-boot-starter-lite test`

2. Dependency tree check passed (no heavy deps present):
   - `mvn -pl sf-chain-spring-boot-starter-lite dependency:tree "-Dincludes=org.springframework.boot:spring-boot-starter-web,org.springframework.boot:spring-boot-starter-webflux,org.springframework.boot:spring-boot-starter-data-jpa,org.springframework.boot:spring-boot-starter-jdbc,com.mysql:mysql-connector-j,org.postgresql:postgresql"`

## Remaining

- Phase 2: apply lite starter in a real consumer (`ai-poet/poet-agent`) and complete migration validation.
- Phase 3: finalize central config-center integration checklist + cutover/backout runbook.

## Exit Check

Mainline Phase 1 is complete: lite starter now has runtime and build-time guardrails preventing heavy coupling from leaking into consumers.
