# PeoClient 1.21.4 — NUKER RENDER FIX13

Fixes the FIX12 startup crash in `WorldRendererNukerUpdateMixin`.

The Minecraft 1.21.4 `WorldRenderer#updateBlock` (`method_8570`) descriptor uses `class_1922` (`BlockView`), not `class_1920`. The mixin parameter was corrected to match the real descriptor.

No Nuker behavior, packet logic, range, multi, cooldown, or AntiVipProMax behavior is changed.
