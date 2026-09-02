package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2596;
import net.minecraft.class_310;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Quản lý packet cho AntiVipProMax.
 *
 * Không tham chiếu trực tiếp tới các packet class có mapping thay đổi giữa
 * các bản Minecraft/Fabric Yarn. Packet được tạo bằng reflection để tránh
 * lỗi compile khi mapping 1.21.4 thay đổi tên intermediary.
 */
public final class BypassPacketManager {
    private static final class_310 mc = class_310.method_1551();
    private static final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();

    private BypassPacketManager() {}

    public static void sendRotation(float yaw, float pitch, boolean onGround) {
        Object packet = createMovePacket(yaw, pitch, onGround);
        inject(packet);
    }

    public static void sendPosition(double x, double y, double z, boolean onGround) {
        Object packet = createPositionPacket(x, y, z, onGround);
        inject(packet);
    }

    /**
     * Gửi action block nếu packet/action có thể được tìm thấy trên mapping
     * runtime. Nếu mapping không khớp thì bỏ qua thay vì làm crash client.
     */
    public static void sendBlockAction(class_2338 pos, class_2350 side) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.class_2846");
            Object action = findAbortAction(packetClass);
            if (action == null) return;

            for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == 3
                        && p[0].isAssignableFrom(action.getClass())
                        && p[1].isAssignableFrom(pos.getClass())
                        && p[2].isAssignableFrom(side.getClass())) {
                    ctor.setAccessible(true);
                    inject(ctor.newInstance(action, pos, side));
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Optional bypass packet; never let it break the main Nuker.
        }
    }

    public static void sendResetPacket() {
        if (mc.field_1724 == null) return;
        sendRotation(mc.field_1724.method_36454(), mc.field_1724.method_36455(), mc.field_1724.field_6228);
    }

    public static void clearQueue() {
        packetQueue.clear();
    }

    private static Object createMovePacket(float yaw, float pitch, boolean onGround) {
        try {
            Class<?> base = Class.forName("net.minecraft.class_2828");
            // Prefer LookAndOnGround: (float yaw, float pitch, boolean onGround, boolean horizontalCollision)
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
            // Prefer PositionAndOnGround: (double x, double y, double z, boolean onGround, boolean horizontalCollision)
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
            if (constants == null || constants.length < 2) continue;
            // PlayerActionC2SPacket.Action keeps ABORT_DESTROY_BLOCK at ordinal 1.
            return constants[1];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void inject(Object packet) {
        if (mc.field_1724 == null || !(packet instanceof class_2596)) return;
        var handler = mc.field_1724.field_6214;
        if (handler == null) return;

        packetQueue.add(packet);

        try {
            Field sendQueueField = null;
            for (Field field : handler.getClass().getDeclaredFields()) {
                if (!Queue.class.isAssignableFrom(field.getType())) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(handler);
                if (value instanceof Queue<?>) {
                    sendQueueField = field;
                    break;
                }
            }

            if (sendQueueField != null) {
                Object queue = sendQueueField.get(handler);
                if (queue instanceof Queue) {
                    ((Queue<Object>) queue).add(packet);
                    packetQueue.remove(packet);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Fall through to normal packet sending.
        }

        try {
            handler.method_10839((class_2596) packet);
        } catch (Throwable ignored) {
            // Never break the main Nuker because of the optional bypass layer.
        } finally {
            packetQueue.remove(packet);
        }
    }
}
