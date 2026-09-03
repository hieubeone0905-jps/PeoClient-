# 4-file integration based on PASS3

- Base is PASS3; no source files were removed.
- Integrated AntiKickEngine with micro-pause controls and enabled gating.
- Integrated AutoBlockReload 50ms/2-tick reload plus PASS3 raycast-aware ghost detection and packet reload.
- Config default `nukerRotate=false`; `nukerRaycast=true` preserved.
- Existing public APIs and PeoClient call sites preserved.
- Local Gradle build was not claimed here; use GitHub Actions/build environment to verify against the project's exact dependencies.
