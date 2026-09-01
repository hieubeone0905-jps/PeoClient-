# PeoClient 1.21.4 - Mixin build fix

## Root cause
The mixin classes were declaring targets with intermediary `class_####` types while the project compiles with Yarn named mappings. This caused the Mixin annotation processor to report `Mixin has no targets` on the `@Mixin(...)` lines.

## Changes
- Converted all PeoClient mixins to Yarn named Minecraft classes.
- Kept the same mixin JSON names and behavior.
- Kept the existing X-Ray, account, inventory acknowledgement and Nuker particle hooks.
- Fixed a duplicate `case "AntiVipProMax"` in `PoeScreen.java`.
- Fixed the AntiVipProMax Hub enabled state to use `AntiVipProMaxModule` instead of the old `AntiCheatModule`.
- Kept the existing Nuker logic/settings untouched.

## Files corrected
- BlockRenderManagerMixin.java
- ChunkOcclusionDataBuilderMixin.java
- ClientPlayNetworkHandlerMixin.java
- MinecraftClientAccessor.java
- MultiplayerScreenMixin.java
- ParticleManagerMixin.java
- ScreenAccessor.java
- SelectWorldScreenMixin.java
- SessionAccessor.java
- SimpleOptionAccessor.java
- TitleScreenMixin.java
- PoeScreen.java

## Build note
A local Gradle build could not be executed in this environment because Gradle 8.12.1 was not cached and `services.gradle.org` is unreachable here. The source was checked for the reported duplicate switch case and the mixin target/method mappings were aligned with Yarn 1.21.4.
