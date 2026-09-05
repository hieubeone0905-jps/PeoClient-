package com.peoclient.nuker.bypass;

import net.minecraft.class_2338;
import net.minecraft.class_634;
import net.minecraft.class_2350;
import net.minecraft.class_2596;
import net.minecraft.class_310;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

    /**
     * Gửi packet tương tác (right-click) lên block để server gửi lại state đúng.
     * Dùng để fix ghost block.
     * 
     * @param pos  vị trí block
     * @param side mặt tương tác (có thể null, sẽ dùng mặt trên)
     */
    public static void sendInteractPacket(class_2338 pos, class_2350 side) {
        if (pos == null) return;
        try {
            // Tìm class PlayerInteractBlockC2SPacket (class_2885)
            Class<?> packetClass = Class.forName("net.minecraft.class_2885");
            // Tìm constructor phù hợp: (Hand hand, BlockHitResult hitResult, int sequence)
            // Hoặc có thể có constructor (Hand, BlockHitResult)
            // Thử tìm constructor với 2 hoặc 3 tham số.
            Constructor<?> ctor = null;
            for (Constructor<?> c : packetClass.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[0].getName().equals("net.minecraft.class_1268") 
                        && params[1].getName().equals("net.minecraft.class_3965")) {
                    ctor = c;
                    break;
                }
                // Trường hợp có sequence (int)
                if (params.length == 3 && params[0].getName().equals("net.minecraft.class_1268")
                        && params[1].getName().equals("net.minecraft.class_3965")
                        && params[2] == int.class) {
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) {
                // Fallback: thử tìm constructor với (Hand, BlockHitResult)
                // Một số phiên bản có thể khác, nhưng 1.21.4 thường có 2 hoặc 3 tham số.
                // Nếu không tìm thấy, bỏ qua.
                return;
            }

            // Tạo BlockHitResult (class_3965)
            Class<?> hitResultClass = Class.forName("net.minecraft.class_3965");
            // Tạo hit result với pos, side, và vị trí trung tâm của block
            // Dùng constructor: BlockHitResult(Vec3D hitPos, Direction side, BlockPos blockPos, boolean inside)
            // hitPos = center của block
            double cx = pos.method_10263() + 0.5;
            double cy = pos.method_10264() + 0.5;
            double cz = pos.method_10260() + 0.5;
            class_243 hitPos = new class_243(cx, cy, cz);
            // Nếu side null, dùng mặt trên (UP)
            if (side == null) side = class_2350.field_11033;
            
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

            // Tạo enum HAND (MAIN_HAND)
            Class<?> handEnum = Class.forName("net.minecraft.class_1268");
            Object mainHand = null;
            for (Object constant : handEnum.getEnumConstants()) {
                if (constant.toString().equals("MAIN_HAND")) {
                    mainHand = constant;
                    break;
                }
            }
            if (mainHand == null) return;

            // Tạo packet
            Object packet;
            if (ctor.getParameterCount() == 2) {
                packet = ctor.newInstance(mainHand, hitResult);
            } else {
                // 3 tham số: hand, hitResult, sequence (thường là 0)
                packet = ctor.newInstance(mainHand, hitResult, 0);
            }

            // Gửi packet
            inject(packet);
        } catch (Throwable ignored) {
            // Không để lỗi làm crash client.
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

    private static Object getNetworkHandler() {
        try {
            for (Field field : mc.getClass().getDeclaredFields()) {
                if (!class_634.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(mc);
                if (value != null) return value;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void inject(Object packet) {
        if (mc.field_1724 == null || !(packet instanceof class_2596)) return;
        Object handler = getNetworkHandler();
        if (handler == null) return;

        packetQueue.add(packet);

        try {
            // ClientPlayNetworkHandler#sendPacket dùng intermediary method_2883
            // trên Minecraft 1.21.4. Reflection tránh phụ thuộc vào private fields.
            var method = handler.getClass().getMethod("method_2883", class_2596.class);
            method.invoke(handler, packet);
        } catch (Throwable ignored) {
            // Optional bypass layer must never break the main Nuker.
        } finally {
            packetQueue.remove(packet);
        }
    }
}