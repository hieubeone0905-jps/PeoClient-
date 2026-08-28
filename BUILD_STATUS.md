# Build status

Target: Minecraft 1.21.4 / Java 21 / Fabric.

## Fixed after GitHub Actions failure

The previous build failed because `splitEnvironmentSourceSets()` placed the Java sources in the wrong source-set/classpath for this client-only project. That caused `net.minecraft.client.*` to be unavailable during compilation and made the client mixins report `Mixin has no targets`.

The project now uses the standard Fabric Loom source set so the client classes are available to the main client-only source tree. GitHub Actions also uses `actions/setup-java@v5`.

Build with:

```text
./gradlew build --no-daemon
```

Windows:

```text
gradlew.bat build
```


## Latest CI fix
- Corrected the Yarn 1.21.4 class name `EnderpearlItem` to `EnderPearlItem` in InventoryCleaner.
- This fixes the compile error reported by GitHub Actions at InventoryCleaner.java:216.


## V1 final rework in this archive
- Rebuilt Hub layout to prevent text overlap. Settings are on the left, immediately beside the hack list; the module grid is on the right/center.
- Added independent scrolling for hack list, module grid, and settings.
- HUD is fixed to the top-left with `PeoClient 1.21.4 V1` followed only by enabled modules, with dark backing and strong shadows for readability.
- Fullbright settings now follow Wurst's `Method`, `Fade`, and `Default brightness`; Gamma is forced to 16.0.
- X-Ray target list follows Wurst's 1.21.4-era default list, with `Only show exposed` and `Opacity` settings. Chunk occlusion is disabled while X-Ray is active.
- Nuker exposes the full supplied BleachHack setting set, including Filter, Raycast, Flatten, Rotate, NoParticles, Highlight and RangeHighlight controls.
- InventoryCleaner exposes the supplied LiquidBounce setting set: category limits, blacklist, Greedy, offhand target and nine hotbar targets. Disposal is acknowledgement-gated to reduce ghost items.

A Gradle build was not executable in this environment because the Gradle distribution host was unreachable. Java syntax was checked with `javac` parsing; dependency-backed compilation still needs to be confirmed by the user's GitHub Actions runner.
