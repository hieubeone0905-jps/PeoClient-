# Compile fix — 2026-09-03

## Reported build error
GitHub Actions reported `cannot find symbol: net.minecraft.class_2727` in `NukerBypassUltimateV2.java`.

## Fixes made without removing the Nuker/V2 feature set
- Replaced the invalid 1.21.4 movement packet mapping `class_2727` with `class_2828.class_2829` (`PlayerMoveC2SPacket.PositionAndOnGround`).
- Replaced the invalid rotation packet mapping `class_2729` with `class_2828.class_2831` (`PlayerMoveC2SPacket.LookAndOnGround`) and supplied the 1.21.4 `horizontalCollision` argument.
- Replaced the invalid block-breaking packet mapping `class_2724` with `class_2846` (`PlayerActionC2SPacket`) and its `class_2847` action enum.
- Removed the unrelated/invalid `class_279` import that was being used as the old block-action enum.
- Fixed packet delivery: the previous fallback called `method_10839`, which is the disconnect callback, not a packet sender. It now uses `ClientCommonNetworkHandler.method_52787` (`sendPacket`).

## Preserved
The existing V2 logic remains: target tracking, rotation smoothing, position buffering/desync calculation, break packet sequence, timing, Grim/Vulcan/NCP settings, intensity/desync settings, diagnostics, and existing Nuker integration were retained.

## Verification
Static source checks were run after the edits. Full Gradle compilation still depends on the GitHub Actions dependency environment; the supplied log confirms the previous failure was at Java compilation on the invalid `class_2727` mapping.
