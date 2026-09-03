# Compile Fix 2026-09-03 — PASS 3

Base: PeoClient 1.21.4 PASS2

## Fixed errors from latest GitHub Actions build

1. `AutoBlockReload.java:94`
   - `Entity.method_5745(...)` returns `class_239` (`HitResult`), not `class_3965`.
   - Changed to receive `class_239` and only process it when `instanceof class_3965`.

2. `AutoBlockReload.java:171`
   - Same raycast return-type mismatch fixed with `instanceof class_3965`.

3. `AutoBlockReload.java:192`
   - `Vec3d.method_1020(...)` is vector subtraction and returns `class_243`.
   - Dot product is `Vec3d.method_1026(...)`.

4. `NukerBypassUltimateV2.java:337`
   - Same dot-product mapping fixed to `method_1026(...)`.

## Preservation

No Nuker, AutoBlockReload, AntiKick, AntiVipProMax, InventoryCleaner, XRay, Fullbright, UI, or other project functionality was intentionally removed.

## Additional static sweep

Checked the Java source for the previously identified incompatible patterns:
- `method_31560`
- `.normalize()`
- `toImmutable()`
- `field_6228`
- `field_6214`
- `method_52787`
- `class_2850`
- `method_1029().method_1020(...)`
- direct assignment of `method_5745(...)` to `class_3965`

No remaining matches for those known-bad patterns were found, except the intentional `method_10839` disconnect mixin already present in `ClientDisconnectMixin.java`.

## Build verification limitation

A local Gradle compile was attempted, but this environment cannot download the configured Gradle 8.12.1 distribution because outbound DNS/network access is unavailable (`UnknownHostException: services.gradle.org`). Therefore this pass is statically verified but not locally Gradle-compiled here.

The authoritative verification remains the user's GitHub Actions build. Run:

`./gradlew clean build --no-daemon --stacktrace`
