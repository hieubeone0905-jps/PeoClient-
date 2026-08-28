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
