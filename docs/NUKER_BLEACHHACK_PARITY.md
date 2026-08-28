# Nuker settings parity

The Nuker configuration follows the supplied BleachHack 1.20.4 `Nuker.java` setting order:

1. Mode: Normal / SurvMulti / Multi / Instant
2. Multi: 1..10
3. Cooldown: 0..4 ticks
4. Shape: Cube / Sphere
5. Range: 1..6
6. Sort: Closest / Furthest / Softest / Hardest / None
7. Filter with Blacklist / Whitelist and block list
8. Raycast
9. Flatten
10. Rotate
11. NoParticles
12. Highlight with Opacity / Expand mode and color
13. RangeHighlight with width and color

The breaking loop intentionally uses Minecraft's block-breaking progression and survival break-delta checks rather than pretending that a server accepted an impossible instant break.
