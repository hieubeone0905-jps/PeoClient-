# PeoClient 1.21.4 - Mapping Fix

## Problem

The PeoClient Java sources are written with Fabric Intermediary names such as `net.minecraft.class_310`, `class_2338`, `method_*`, and `field_*`. The previous attempt supplied the raw Intermediary mapping artifact to Loom. Loom 1.10.5 then failed during project configuration because that mapping set has no `named` namespace:

`Could not find 'named' mapping position!`

## Fix in this package

`build.gradle` now resolves the Minecraft 1.21.4 Intermediary artifact and creates a tiny-v2 mapping jar at configuration time. The generated mapping has:

- `official` = original official namespace
- `intermediary` = Fabric Intermediary namespace
- `named` = an identity copy of Intermediary

Loom therefore has the required `named` namespace while the existing source can continue using the exact `class_*` / `method_*` / `field_*` identifiers.

## Nuker

The Nuker implementation and its settings are not rewritten by this fix. The change is limited to Gradle/Loom mapping setup, so it should not alter Nuker's runtime algorithm.
