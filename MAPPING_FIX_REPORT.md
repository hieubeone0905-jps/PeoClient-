# PeoClient 1.21.4 - Mapping Fix

## What changed
- The existing PeoClient source was already written against Fabric Intermediary names (`class_`, `method_`, `field_`).
- The previous build.gradle selected Yarn named mappings, which caused errors such as `cannot find symbol net.minecraft.class_310`.
- This build uses the Minecraft 1.21.4 Intermediary mapping artifact instead, so the existing source and Mixin targets use one consistent namespace.
- PoeScreen's AntiVipProMax wiring was fixed to use `AntiVipProMaxModule` and the duplicate switch case was removed.

## What was intentionally not changed
- NukerLogic and its existing settings/GUI behavior were preserved.
- AccountScreen and the existing Mixin source were preserved in their working intermediary form.
- No anti-cheat bypass packet implementation was added to the build.

## Expected effect
This fixes the specific `cannot find symbol net.minecraft.class_*` compile failures without rewriting the Nuker algorithm.
