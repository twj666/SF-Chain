# Phase 2 Report

## Completed

1. Split project into Maven multi-module reactor:
   - Root aggregator: `pom.xml` (`sf-chain-parent`)
   - Legacy module: `sf-chain-legacy-starter` (artifact `io.github.twj666:sf-chain`)
   - Lite module: `sf-chain-spring-boot-starter-lite` (artifact `io.github.twj666:sf-chain-spring-boot-starter-lite`)

2. Kept legacy artifact coordinate unchanged:
   - `io.github.twj666:sf-chain:1.0.11`
   - Existing consumers can continue using current dependency path.

3. Added lightweight starter artifact:
   - Depends on legacy artifact with explicit exclusions for heavy coupling deps:
     - `spring-boot-starter-web`
     - `spring-boot-starter-webflux`
     - `spring-boot-starter-data-jpa`
     - `spring-boot-starter-jdbc`
     - `mysql-connector-j`
     - `postgresql`
     - `hibernate-types-52`
   - Re-introduces only required runtime baseline deps:
     - `spring-boot-autoconfigure`
     - `reactor-core`

4. Moved existing source tree to legacy module:
   - `src` -> `sf-chain-legacy-starter/src`
   - `build-frontend.sh` -> `sf-chain-legacy-starter/build-frontend.sh`

5. Fixed duplicate configuration properties binding in legacy module:
   - Removed duplicate `@ConfigurationProperties(prefix = "ai.operations")` on `SfChainAutoConfiguration.aiOperationRegistry` bean method.

## Verification

1. Full reactor tests pass (JDK17):
   - Command:
     - `mvn test -DskipITs`
   - Result: `BUILD SUCCESS`

2. Dependency-tree verification:
   - Lite module compile dependency tree keeps only:
     - `io.github.twj666:sf-chain`
     - `spring-boot-autoconfigure`
     - `reactor-core`
   - Heavy dependencies are excluded from lite starter transitive surface.

## Remaining (for next phases)

- Further split legacy code into dedicated modules (`core`, `provider-openai`, `config-client`, `logging-client`) to eliminate reliance on exclusion-based lite strategy.
- Introduce remote config client and versioned snapshot sync.
- Introduce async log upload client and center-side ingestion APIs.

## Exit Check

- Multi-module split completed.
- Legacy and lite artifacts can be built together.
- Existing artifact remains available for backward compatibility.
- Lite starter path available for low-coupling integration.
