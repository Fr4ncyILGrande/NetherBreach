# Verification record

Version 1.0.0 was built locally with Java 25.0.4.1, `spigot-api 26.2-R0.1-SNAPSHOT` and `maven-compiler-plugin 3.14.1`.

Passed checks:

- Maven build and direct `javac --release 25 -Xlint:all,-classfile` compilation, with no warnings attributable to plugin code.
- YAML validation for `plugin.yml` and `config.yml`.
- Embedded metadata verification: version `1.0.0`, author `Fr4ncyILGrande` and default material `GOLDEN_PICKAXE`.
- JAR integrity verification with `unzip -t`.
- `jdeps` analysis: runtime dependencies are limited to Java SE and the Bukkit/Spigot API provided by the server.
- Class scan: NetherBreach classes only, with no network client, telemetry, metrics, updater or socket.
- Spigot 26.2 API verification for `Player#getLocale`, `PlayerLocaleChangeEvent` and `PrepareItemCraftEvent`, used to localize messages and items without Paper dependencies.
- Six passing JUnit tests for language resolution and roof boundaries, including the distinction between physical height 256 and logical height 128.
- Real startup on stable Paper 26.2-134 with Java 25: plugin loading, recipe registration, enable, reload and console commands completed without plugin errors.
- Real startup on Purpur 26.2-2633 with Java 25: loading, recipe registration, reload and clean shutdown completed without plugin errors.
- Official Spigot 26.2 build produced through BuildTools and real startup on `4648-Spigot-8db49a2-96ae8cd`: loading, recipe registration, reload, metadata and clean shutdown completed without plugin errors.
- Invalid language, use-count and recipe values produced safe fallbacks and readable warnings while the plugin remained enabled.
- Real-server localization checks passed for the default English fallback, invalid-setting fallbacks, and forced Italian mode.
- Recipe output verified as a golden pickaxe; marked items are aligned with the configured material during normalization.

The plugin emits a cancellable `BlockBreakEvent` before changing a block so protection plugins can participate in the decision. Paper, Purpur and Spigot expose the same Bukkit/Spigot API surface used by the project.
