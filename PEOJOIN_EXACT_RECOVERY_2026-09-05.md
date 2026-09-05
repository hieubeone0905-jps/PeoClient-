# PeoJoin exact Hub recovery flow - 2026-09-05

- Nuker default keybind is M (saved custom keybinds are preserved).
- PeoJoin stays passive while Nuker is ON.
- Hub detection no longer requires ClientWorld replacement or an 80-block teleport.
- On this server, compass + slimeball in the hotbar is treated as the Hub profile, with world/transfer/teleport signals retained as additional confirmation paths.
- Once Hub is detected, Nuker is toggled OFF through the same client module toggle path used by its keybind.
- Wait exactly at least 3 seconds after entering Hub.
- Select the compass and perform the normal right-click interaction to open the server GUI.
- Find and click the minecraft:diamond_pickaxe entry.
- Wait at least 5 seconds, then send the real /home command to the server.
- Wait at least 5 seconds, then toggle Nuker ON through the same module toggle path used by its keybind.
- Nuker targeting/range/multi/break-speed logic is not modified by PeoJoin.
