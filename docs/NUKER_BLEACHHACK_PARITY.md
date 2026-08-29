# Nuker parity reference

PeoClient's Nuker settings and behaviour are ported from the user-supplied BleachHack 1.20.4 `Nuker.java` reference, adapted to Fabric 1.21.4 and PeoClient's Java architecture.

Settings:

1. Mode: `Normal`, `SurvMulti`, `Multi`, `Instant`
2. Multi: `1..10`
3. Cooldown: `0..4` ticks
4. Shape: `Cube`, `Sphere`
5. Range: `1..6`
6. Sort: `Closest`, `Furthest`, `Softest`, `Hardest`, `None`
7. Filter: `Blacklist` / `Whitelist` plus editable block-id list
8. Raycast
9. Flatten
10. Rotate
11. NoParticles
12. Highlight: `Opacity` / `Expand`, color
13. RangeHighlight: width and color

The block-breaking loop follows Minecraft's real block-breaking progression and the BleachHack decision order rather than fabricating server-side instant breaks.
