# Nuker Omnidirectional Update

Base: `PeoClient--main (7)(1).zip`

## Changes
- Nuker target-face selection now explicitly prefers the block face closest to the player's eye direction.
- If that face cannot be reached, all remaining block faces are tested.
- Covers mining from above (down face), below (up face), and straight-on from all four horizontal directions.
- Existing range, shape, filter, raycast, rotate, mode, multi, cooldown, sorting, break-progress and interaction logic are unchanged.
- No extra breaking delay or artificial cooldown was added.
- The existing six-face raycast safety is preserved when Raycast is enabled.

## Build note
The source was updated successfully in the working tree. A local Gradle build could not be completed in this environment because Gradle 8.12.1 is not cached and external network access is unavailable. Run the existing GitHub Actions workflow to compile the ZIP.
