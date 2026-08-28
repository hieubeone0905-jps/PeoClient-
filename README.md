# PeoClient 1.21.4 v1

Fabric client-side utility/mod project for Minecraft 1.21.4.

## Modules / UI v1
- Wurst-inspired searchable hub with left hack list, 3-column module grid, expandable settings panel and per-module keybind capture.

## Modules
- X-Ray: configurable target block registry IDs, fluids, surface hiding, opacity/alpha and reload-on-change.
- Fullbright: Gamma and Night Vision modes with restoration of the user's original gamma.
- Nuker: range, shape, sorting, filtering/whitelist, raycast, flattening, multi-break, instant mode and optional visual/rotation settings reserved for renderer integration.
- InventoryCleaner: blacklist, greedy cleanup, stack limits, hotbar category targets and offhand target.

## Important
This project is an independent implementation. It does not include copied source from BleachHack or LiquidBounce. Their projects can be used as behavioral references subject to their respective licenses.

## Build
Requires Java 21 and a network-capable Gradle environment. Run:

    ./gradlew build

The built mod will be under `build/libs/`.

## Current verification
The source has been statically reviewed and packaged. A full Gradle build could not be performed in this environment because Gradle 8.12.1 is not cached and `services.gradle.org` is unreachable from the build environment.

## Detailed modules

- `docs/NUKER_BLEACHHACK_PARITY.md` — Nuker parity settings and 1.21.4 adaptation.
- `docs/INVENTORY_CLEANER.md` — InventoryCleaner behaviour and configuration.
- `src/main/java/com/peoclient/inventory/InventoryCleaner.java` — independent detailed cleaner implementation.
