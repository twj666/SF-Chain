# Phase 1 Report

## Completed
- Removed framework-wide `@ComponentScan` from auto-configuration path.
- Split auto-configuration by responsibility:
  - Core: `SfChainAutoConfiguration`
  - Persistence: `SfChainPersistenceAutoConfiguration`
  - Management API: `SfChainManagementAutoConfiguration`
  - Persistence-bound management APIs: `SfChainPersistenceManagementAutoConfiguration`
  - Static UI: `SfChainStaticUiAutoConfiguration`
- Introduced feature flags with safe defaults in `SfChainFeaturesProperties`:
  - `management-api=false`
  - `local-persistence=false`
  - `local-migration=false`
  - `static-ui=false`
- Kept built-in operations available through explicit core bean registration.
- Updated auto-configuration registration files:
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - `META-INF/spring.factories`

## Remaining (for next phases)
- Split artifacts into separate Maven modules (`core`, `starter-lite`, etc.).
- Add remote config client and versioned snapshot sync.
- Add async log uploader and center-side ingestion.

## Exit Check
- Core runtime loads by default.
- Management API / local persistence / static UI are opt-in only.
- Regression tests cover default and feature-flag paths.
