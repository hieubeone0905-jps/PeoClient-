# PeoClient 1.21.4 - Fix7

Changes:
- X-Ray now has an Edit blocks picker with search, Minecraft block/item icons, display names and registry IDs, persisted in peoclient.json.
- All modules listed in the hub get an individually persisted keybind setting. Delete/Backspace clears a bind; Escape cancels binding capture.
- Implemented module hotkeys (X-Ray, Nuker [Multi], Fullbright, InventoryCleaner) toggle their modules. Other module keybinds are stored and ready for future module implementations.
- Existing Nuker picker remains intact.
- Hub allows selecting non-implemented modules so their keybind can be configured.

Build note: Gradle compile could not be executed in the sandbox because services.gradle.org is unreachable (UnknownHostException). The ZIP archive was validated after packaging.
