# Ghost-block render fix (Minecraft 1.21.4)

The client now observes `ClientWorld#handleBlockUpdate` and places affected block
positions into a deduplicated render-refresh queue. Up to 12 affected positions are
rebuilt per client tick. A full `WorldRenderer.reload()` is only used as a throttled
fallback when the queue exceeds 128 positions, with a 2-second cooldown.

This is a client-side visual recovery only. It does not fabricate server state,
send fake block packets, or simulate right-click input.
