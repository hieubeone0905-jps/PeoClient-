package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_634;
import net.minecraft.class_2596;
import net.minecraft.class_310;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Quản lý packet cho AntiVipProMax.
 * Không tham chiếu trực tiếp tới các packet class có mapping thay đổi.
 */
public final class BypassPacketManager {
    private static final class_310 mc = class_310.method_1551();
    private static final Queue<Object> packetQueue = new ConcurrentLinkedQueue<>();

    // Giới hạn tần suất gửi interact packet
    private static long lastInteractTime = 0;
    private static final long INTERACT_COOLDOWN_MS = 3000; // 3 giây

    private BypassPacketManager() {}

    public static void sendRotation(float yaw, float pitch, boolean onGround) {
        Object packet = createMovePacket(yaw, pitch, onGround);
        inject(packet);
    }

    public static void sendPosition(double x, double y, double z, boolean onGround) {
        Object packet = createPositionPacket(x, y, z, onGround);
        inject(packet);
    }

    public static void sendBlockAction(class_2338 pos, class_2350 side) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.class_2846");
            Object action = findAbortAction(packetClass);
            if (action == null) return;
            for (Constructor<?> ctor : packetClass.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length == 3 && p[0].isAssignableFrom(action.getClass())
                        && p[1].isAssignableFrom(pos.getClass())
                        && p[2].isAssignableFrom(side.getClass())) {
                    ctor.setAccessible(true);
                    inject(ctor.newInstance(action, pos, side));
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Gửi packet tương tác (right‑click) lên block để server gửi lại state đúng.
     * Có cơ chế giới hạn tần suất để tránh bị kick.
     */
    public static void sendInteractPacket(class_2338 pos, class_2350 side) {
        if (pos == null) return;
        long now = System.currentTimeMillis();
        if (now - lastInteractTime < INTERACT_COOLDOWN_MS) {
            return; // Chưa đủ thời gian chờ
        }
        try {
            Class<?> packetClass = Class.forName("net.minecraft.class_2885");
            Constructor<?> ctor = null;
            for (Constructor<?> c : packetClass.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[0].getName().equals("net.minecraft.class_1268") 
                        && params[1].getName().equals("net.minecraft.class_3965")) {
                    ctor = c;
                    break;
                }
                if (params.length == 3 && params[0].getName().equals("net.minecraft.class_1268")
                        && params[1].getName().equals("net.minecraft.class_3965")
                        && params[2] == int.class) {
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) return;

            double cx = pos.method_10263() + 0.5;
            double cy = pos.method_10264() + 0.5;
            double cz = pos.method_10260() + 0.5;
            class_243 hitPos = new class_243(cx, cy, cz);
            if (side == null) side = class_2350.field_11033;

            Class<?> hitResultClass = Class.forName("net.minecraft.class_3965");
            Constructor<?> hitCtor = null;
            for (Constructor<?> c : hitResultClass.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 4 && p[0].getName().equals("net.minecraft.class_243")
                        && p[1].getName().equals("net.minecraft.class_2350")
                        && p[2].getName().equals("net.minecraft.class_2338")
                        && p[3] == boolean.class) {
                    hitCtor = c;
                    break;
                }
            }
            if (hitCtor == null) return;
            hitCtor.setAccessible(true);
            Object hitResult = hitCtor.newInstance(hitPos, side, pos, false);

            Class<?> handEnum = Class.forName("net.minecraft.class_1268");
            Object mainHand = null;
            for (Object constant : handEnum.getEnumConstants()) {
                if (constant.toString().equals("MAIN_HAND")) {
                    mainHand = constant;
                    break;
                }
            }
            if (mainHand == null) return;

            Object packet;
            if (ctor.getParameterCount() == 2) {
                packet = ctor.newInstance(mainHand, hitResult);
            } else {
                packet = ctor.newInstance(mainHand, hitResult, 0);
            }

            inject(packet);
            lastInteractTime = now; // Cập nhật thời gian
        } catch (Throwable ignored) {
            // Không làm crash client
        }
    }

    public static void sendResetPacket() {
        if (mc.field_1724 == null) return;
        sendRotation(mc.field_1724.method_36454(), mc.field_1724.method_36455(), mc.field_1724.method_24828());
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
                    if (p.length == 4 && p[0] == float.class && p[1] == float.class
                            && p[2] == boolean.class && p[3] == boolean.class) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(yaw, pitch, onGround, false);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object createPositionPacket(double x, double y, double z, boolean onGround) {
        try {
            Class<?> base = Class.forName("net.minecraft.class_2828");
            for (Class<?> nested : base.getDeclaredClasses()) {
                for (Constructor<?> ctor : nested.getDeclaredConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    if (p.length == 5 && p[0] == double.class && p[1] == double.class
                            && p[2] == double.class && p[3] == boolean.class && p[4] == boolean.class) {
                        ctor.setAccessible(true);
                        return ctor.newInstance(x, y, z, onGround, false);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object findAbortAction(Class<?> packetClass) {
        for (Class<?> nested : packetClass.getDeclaredClasses()) {
            if (!nested.isEnum()) continue;
            Object[] constants = nested.getEnumConstants();
            if (constants == null || constants.length < 2) continue;
            return constants[1]; // ABORT_DESTROY_BLOCK
        }
        return null;
    }

    private static Object getNetworkHandler() {
        try {
            for (Field field : mc.getClass().getDeclaredFields()) {
                if (!class_634.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(mc);
                if (value != null) return value;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void inject(Object packet) {
        if (mc.field_1724 == null || !(packet instanceof class_2596)) return;
        Object handler = getNetworkHandler();
        if (handler == null) return;

        packetQueue.add(packet);
        try {
            var method = handler.getClass().getMethod("method_2883", class_2596.class);
            method.invoke(handler, packet);
        } catch (Throwable ignored) {
        } finally {
            packetQueue.remove(packet);
        }
    }
}