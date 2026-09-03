# PeoClient 1.21.4 Nuker Fix V3

- Restored the missing `NukerLogic.isValidTarget(Client, Target)` helper.
- The helper validates range, area, flatten mode, block state, filter, raycast and break progress before a queued target is started.
- No renderer reload or renderer queue was reintroduced.
- Existing continuous-breaking logic and current Nuker settings were preserved.
