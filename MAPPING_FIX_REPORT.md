# PeoClient 1.21.4 - Mapping Fix

## Root cause found
The project was resolving `net.fabricmc:intermediary:1.21.4` without the `v2` classifier. That artifact contains the older Tiny v1 mapping file with only `official` and `intermediary` namespaces. The custom generator in `build.gradle` expects Tiny v2 and therefore never added the `named` namespace, producing Loom's error `Could not find 'named' mapping position!`.

## Fix
The intermediary dependency now explicitly uses the Fabric Tiny v2 artifact:

`net.fabricmc:intermediary:${project.minecraft_version}:v2`

The existing identity `named` generation is retained so the current Java sources can continue using Intermediary names such as `class_*`, `method_*`, and `field_*`.

## Scope
Only the Gradle mapping dependency was changed. Nuker and other Java source files were not modified by this mapping fix.
