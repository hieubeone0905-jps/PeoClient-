# AntiVipProMax integration

This build adds an `AntiVipProMax` module to the existing PeoClient hub.

- Default key: C
- Settings: Grim Mode, Vulcan Mode, Intensity 1-10, Auto Adjust
- Existing Nuker settings and NukerLogic are left unchanged.
- The module is a compatibility/settings layer and does not inject synthetic movement/rotation packets or attempt to defeat server anti-cheat.

The supplied packet-spoof/bypass sources were not wired into the client because doing so would replace the existing Nuker control path and could destabilize the 1.21.4 client. They are therefore not part of the active build.
