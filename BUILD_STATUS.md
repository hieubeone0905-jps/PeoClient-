PeoClient 1.21.4 - GUI panel overflow fix (Fix9)

Changes:
- Reworked two-column Hub layout to keep both panels fully inside the viewport at all resolutions.
- Settings panel receives remaining width instead of exceeding the screen.
- Long setting values are ellipsized to remain inside their row.
- Footer/header anchors are clamped to the viewport.
- Preserves independent scissoring and scroll behavior.

Target: Minecraft 1.21.4 / Fabric.
