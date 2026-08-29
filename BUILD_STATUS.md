Build status
============

Fix15 source patch: corrected missing Minecraft 1.21.4 Yarn imports used by Nuker raycast logic:
- net.minecraft.util.hit.BlockHitResult
- net.minecraft.util.hit.HitResult

GitHub Actions should now be able to compile past PeoClient.java lines 627/632.

Local verification note: the environment could not run Gradle because the wrapper attempted to download Gradle 8.12.1 from services.gradle.org and DNS/network access is unavailable here.
