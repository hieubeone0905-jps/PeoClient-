# PeoClient 1.21.4 V1 modules

The V1 Hub exposes four implemented modules:

- **Fullbright** — Wurst-style settings: `Method (Gamma/Night Vision)`, `Fade`, `Default brightness`. Gamma mode forces brightness to 16.0 (1600%) and restores the captured pre-enable value.
- **X-Ray** — Wurst-style settings: `Ores`, `Only show exposed`, `Opacity`. The default target list follows Wurst's 1.21.4-era X-Ray list, including ores, chests, spawners and utility blocks.
- **Nuker [Multi]** — settings ported from the supplied BleachHack 1.20.4 Nuker: `Mode`, `Multi`, `Cooldown`, `Shape`, `Range`, `Sort`, `Filter`, filter mode/list, `Raycast`, `Flatten`, `Rotate`, `NoParticles`, `Highlight`, highlight mode/color concept, `RangeHighlight`, width/color concept.
- **InventoryCleaner** — settings and workflow adapted from the supplied LiquidBounce nextgen module: category quotas, blacklist, greedy mode, offhand target and nine hotbar targets, plus merge/sort/disposal ordering. Inventory disposal waits for authoritative server slot updates to reduce ghost-item corrections.

The other names in the Hub are currently presentation-only placeholders and are not advertised as implemented modules.
