# BUILD STATUS

This revision updates the Nuker settings UI and filter list.

- Numeric Nuker controls are displayed as slider-style controls with explicit 0/min to maximum ranges.
- The Nuker settings panel remains scrollable.
- The main Nuker settings panel shows up to five selected blacklist/whitelist blocks with icon, translated name, and registry ID.
- Clicking a selected row removes it.
- The existing Block Picker remains available for adding/removing blocks.

Build verification in this environment was attempted, but the Gradle wrapper could not download Gradle 8.12.1 because `services.gradle.org` was not reachable. The ZIP itself was validated after packaging.
