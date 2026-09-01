# InventoryCleaner compile fix

The previous build failed because InventoryCleaner.java used intermediary names (`net.minecraft.class_*`, `method_*`, `field_*`) while the project is configured for Yarn named mappings `1.21.4+build.8`.

This version converts InventoryCleaner to Yarn named mappings, including:
- MinecraftClient / player / interactionManager / currentScreen
- PlayerInventory / getStack / getOffHandStack
- ItemStack / SlotActionType / Items / Registries / DataComponentTypes
- item category classes
- ItemStack component and durability APIs

Also corrected the ClientPlayNetworkHandlerMixin acknowledgement argument order to match InventoryCleaner.onServerSlotUpdate().
