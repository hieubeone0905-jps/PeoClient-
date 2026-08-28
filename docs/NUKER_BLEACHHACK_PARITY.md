# Nuker 1.21.4 parity notes

The Nuker settings and selection algorithm are adapted to match the public BleachHack Nuker model:

- Normal / SurvMulti / Multi / Instant
- Multi 1..10
- Cooldown 0..4 ticks
- Cube / Sphere
- Range 1..6
- Closest / Furthest / Softest / Hardest / None
- Filter with blacklist/whitelist
- Raycast
- Flatten
- Rotation hook
- NoParticles
- Highlight and RangeHighlight configuration state

The original BleachHack code is not copied into PeoClient. Minecraft 1.21.4/Yarn APIs are used for the implementation.
