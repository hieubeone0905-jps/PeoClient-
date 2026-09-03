package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_634;
import net.minecraft.class_2350;
import net.minecraft.class_2596;
import net.minecraft.class_310;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Packet helper used by the AntiVipProMax/Nuker bypass layer.
 *
 * The packet types themselves are resolved through intermediary names at
 * runtime.  This keeps this optional helper isolated from mapping changes,
 * while the actual send path is made robust against inherited/private fields
 * and methods in the 1.21.4 client classes.
 */
public final class BypassPacketManager {
    private static final class_310 mc = class_310.method_1551();
    private static final Queue<class_2596<?>> packetQueue = new ConcurrentLinkedQueue<>();

    private BypassPacketManager() {}

    public static void sendRotation(float yaw, float pitch, boolean onGround) {
        inject(createMovePacket(yaw, pitch, onGround));
    }

    public static void sendPosition(double x, double y, double z, boolean onGround) {
        inject(createPositionPacket(x, y, z, onGround));
    }

    /** Sends ABORT_DESTROY_BLOCK for the supplied block position. */
    public static void sendBlockAction(class_2338 pos, class_2350 side) {
        if (pos == null || side == null) return;
        try {
            Class<?> packetClass = Class.forName("net.minecraft.class_2846");
            Object action = findAbortAction(packetClass);
            if (action == null) return;

            for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length != 3) continue;
                if (!p[0].isAssignableFrom(action.getClass())) continue;
                if (!p[1].isAssignableFrom(pos.getClass())) continue;
                if (!p[2].isAssignableFrom(side.getClass())) continue;

                ctor.setAccessible(true);
                Object packet = ctor.newInstance(action, pos, side);
                inject(packet);
                return;
            }
        } catch (Throwable ignored) {
            // This is an optional layer; never break the main Nuker loop.
        }
    }

    public static void sendResetPacket() {
        if (mc.field_1724 == null) return;
        sendRotation(
                mc.field_1724.method_36454(),
                mc.field_1724.method_36455(),
                mc.field_1724.method_24828()
        );
    }

    public static void clearQueue() {
        packetQueue.clear();
    }

    private static Object createMovePacket(float yaw, float pitch, boolean onGround) {
        try {
            Class<?> base = Class.forName("net.minecraft.class_2828");
            for (Class<?> nested : base.getDeclaredClasses()) {
                for (Constructor<?> ctor : nested.getDeclaredConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    if (p.length == 4
                            && p[0] == float.class
                            && p[1] == float.class
                            && p[2] == boolean.class
                            && p[3] == boolean.class) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(yaw, pitch, onGround, false);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object createPositionPacket(double x, double y, double z, boolean onGround) {
        try {
            Class<?> base = Class.forName("net.minecraft.class_2828");
            for (Class<?> nested : base.getDeclaredClasses()) {
                for (Constructor<?> ctor : nested.getDeclaredConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    if (p.length == 5
                            && p[0] == double.class
                            && p[1] == double.class
                            && p[2] == double.class
                            && p[3] == boolean.class
                            && p[4] == boolean.class) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(x, y, z, onGround, false);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object findAbortAction(Class<?> packetClass) {
        for (Class<?> nested : packetClass.getDeclaredClasses()) {
            if (!nested.isEnum()) continue;
            Object[] constants = nested.getEnumConstants();
            if (constants == null) continue;

            // Prefer the actual enum constant name when available.
            for (Object constant : constants) {
                if (constant instanceof Enum<?> e
                        && "ABORT_DESTROY_BLOCK".equals(e.name())) {
                    return constant;
                }
            }

            // Intermediary runtime names can hide the readable enum name.
            // In 1.21.4 ABORT_DESTROY_BLOCK is ordinal 1.
            if (constants.length > 1) return constants[1];
        }
        return null;
    }

    /**
     * Finds ClientPlayNetworkHandler without assuming the exact Minecraft
     * field name.  Fields inherited from a superclass are also inspected.
     */
    private static class_634 getNetworkHandler() {
        try {
            Class<?> type = mc.getClass();
            while (type != null) {
                for (Field field : type.getDeclaredFields()) {
                    if (!class_634.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object value = field.get(mc);
                    if (value instanceof class_634 handler) return handler;
                }
                type = type.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Sends a clientbound-safe packet through ClientPlayNetworkHandler. */
    private static void inject(Object packet) {
        if (mc.field_1724 == null || !(packet instanceof class_2596<?> typedPacket)) return;

        class_634 handler = getNetworkHandler();
        if (handler == null) return;

        packetQueue.add(typedPacket);
        try {
            Method send = findSendPacketMethod(handler.getClass());
            if (send == null) return;
            send.setAccessible(true);
            send.invoke(handler, typedPacket);
        } catch (Throwable ignored) {
            // Optional bypass layer must never break the main Nuker.
        } finally {
            packetQueue.remove(typedPacket);
        }
    }

    /**
     * Finds the intermediary sendPacket method while tolerating inheritance.
     * The normal 1.21.4 intermediary name is method_2883.
     */
    private static Method findSendPacketMethod(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if ("method_2883".equals(method.getName())
                        && method.getParameterCount() == 1
                        && class_2596.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
