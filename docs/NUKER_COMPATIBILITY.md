# Nuker compatibility layer

The old packet-spoof Bypass implementation has been replaced by `NukerCompatibility`.

- Key **B** toggles compatibility mode.
- Breaking remains on the normal Minecraft client interaction path.
- No background thread is used.
- No synthetic position packets are generated.
- No packet-queue reflection is used.
- The existing Nuker settings remain authoritative: Mode, Multi, Cooldown, Shape, Range and Sort.

This is intended to make the client state consistent and reduce implementation-caused kicks. It is not an anti-cheat evasion mechanism.
