// PeoJoinModule.java
package com.peoclient.modules;

import com.peoclient.PeoClient;
import com.peoclient.diagnostic.DiagnosticRecorder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.class_310;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_3675;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayDeque;
import java.util.Queue;

public final class PeoJoinModule {
    private static final class_310 mc = class_310.method_1551();
    private static final AtomicBoolean enabled = new AtomicBoolean(false);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread workerThread;
    private static State currentState = State.IDLE;
    private static long stateStartTime = 0;
    private static int waitTicks = 0;
    private static boolean nukerWasEnabled = false;

    // Cấu hình
    private static String serverName = "Skyblock";
    private static int preJoinDelay = 5; // giây
    private static int postJoinDelay = 5; // giây
    private static int walkForwardTicks = 20; // số tick di chuyển
    private static boolean enableNukerOnJoin = true;

    public enum State {
        IDLE,
        WAITING_FOR_KICK,
        KICKED,
        LOBBY,
        SELECTING_SERVER,
        WAITING_JOIN,
        JOINED,
        WAITING_HOME,
        MOVING_FORWARD,
        ENABLING_NUKER,
        DONE
    }

    public static void toggle() {
        enabled.set(!enabled.get());
        if (enabled.get()) {
            start();
        } else {
            stop();
        }
        PeoClient.CFG.save();
        DiagnosticRecorder.get().record("PeoJoin", "Toggled to " + enabled.get());
    }

    public static boolean isEnabled() { return enabled.get(); }

    private static void start() {
        if (running.get()) return;
        running.set(true);
        currentState = State.IDLE;
        stateStartTime = System.currentTimeMillis();
        nukerWasEnabled = false;
        workerThread = new Thread(PeoJoinModule::loop, "PeoJoin-Thread");
        workerThread.setDaemon(true);
        workerThread.start();
        DiagnosticRecorder.get().record("PeoJoin", "Started");
    }

    private static void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
            try { workerThread.join(1000); } catch (InterruptedException ignored) {}
            workerThread = null;
        }
        currentState = State.IDLE;
        if (nukerWasEnabled && !PeoClient.CFG.nuker) {
            // Nếu Nuker đã được bật lại, giữ nguyên
        }
        DiagnosticRecorder.get().record("PeoJoin", "Stopped");
    }

    private static void loop() {
        while (running.get()) {
            try {
                if (mc.field_1724 == null) {
                    // Chưa vào game
                    handleDisconnected();
                } else {
                    handleConnected();
                }
                Thread.sleep(50); // 20ms per loop, but allow interrupts
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but continue
                DiagnosticRecorder.get().record("PeoJoin", "Error: " + e.getMessage());
            }
        }
    }

    private static void handleDisconnected() {
        // Nếu đang ở ngoài game, chờ vào
        if (currentState == State.IDLE) {
            currentState = State.WAITING_FOR_KICK;
            stateStartTime = System.currentTimeMillis();
            DiagnosticRecorder.get().record("PeoJoin", "Waiting for kick/disconnect...");
        }
        // Nếu đã bị kick, chuyển sang lobby
        if (currentState == State.WAITING_FOR_KICK) {
            // Kiểm tra xem có đang ở màn hình kick không? (lobby)
            Screen screen = mc.field_1755;
            if (screen instanceof MultiplayerScreen) {
                currentState = State.LOBBY;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "In lobby, selecting server...");
            }
        }

        // Xử lý lobby: tự động chọn server
        if (currentState == State.LOBBY) {
            // Chọn server Skyblock
            if (mc.field_1755 instanceof MultiplayerScreen) {
                // Tìm server trong danh sách và click vào
                selectSkyblockServer();
                currentState = State.SELECTING_SERVER;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Selecting Skyblock server...");
            }
        }

        // Sau khi chọn server, chờ vào
        if (currentState == State.SELECTING_SERVER) {
            if (System.currentTimeMillis() - stateStartTime > preJoinDelay * 1000L) {
                // Đã chờ đủ, bây giờ click Join
                clickJoinButton();
                currentState = State.WAITING_JOIN;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Waiting to join...");
            }
        }

        // Chờ vào game
        if (currentState == State.WAITING_JOIN) {
            if (mc.field_1724 != null) {
                currentState = State.JOINED;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Joined server!");
            }
        }
    }

    private static void handleConnected() {
        if (currentState == State.JOINED) {
            // Vào server rồi, gõ /home
            if (System.currentTimeMillis() - stateStartTime > 1000) {
                sendChatCommand("/home");
                currentState = State.WAITING_HOME;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Sent /home");
            }
        }

        if (currentState == State.WAITING_HOME) {
            // Chờ 5s để /home xong
            if (System.currentTimeMillis() - stateStartTime > postJoinDelay * 1000L) {
                currentState = State.MOVING_FORWARD;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Moving forward...");
            }
        }

        if (currentState == State.MOVING_FORWARD) {
            // Giữ phím W trong một số tick
            if (waitTicks < walkForwardTicks) {
                mc.field_1690.field_1694.method_1446(true);
                waitTicks++;
            } else {
                mc.field_1690.field_1694.method_1446(false);
                waitTicks = 0;
                currentState = State.ENABLING_NUKER;
                stateStartTime = System.currentTimeMillis();
                DiagnosticRecorder.get().record("PeoJoin", "Enabling Nuker...");
            }
        }

        if (currentState == State.ENABLING_NUKER) {
            // Bật Nuker bằng phím M
            if (!PeoClient.CFG.nuker) {
                // Giả định phím M là nút gắn sẵn để bật Nuker
                // Sử dụng phím tắt 'M' (key code 77)
                class_304 key = new class_304("PeoClient Nuker", class_3675.class_307.field_1668, GLFW.GLFW_KEY_M, "PeoClient");
                key.method_1422(class_3675.class_307.field_1668.method_1447(GLFW.GLFW_KEY_M));
                // Khởi tạo action để bật Nuker
                if (PeoClient.MODULE_KEYS.containsKey("Nuker [Multi]")) {
                    class_304 nukerKey = PeoClient.MODULE_KEYS.get("Nuker [Multi]");
                    // Simulate key press
                    nukerKey.method_1422(class_3675.class_307.field_1668.method_1447(GLFW.GLFW_KEY_M));
                    // Toggle Nuker
                    PeoClient.toggleModuleByName("Nuker [Multi]", mc);
                    nukerWasEnabled = true;
                }
                DiagnosticRecorder.get().record("PeoJoin", "Nuker enabled via key M");
            }
            // Reset state để tiếp tục theo dõi
            currentState = State.IDLE;
            stateStartTime = System.currentTimeMillis();
            DiagnosticRecorder.get().record("PeoJoin", "Cycle complete. Waiting for next kick...");
        }

        // Nếu Nuker bị tắt do kick, module sẽ phát hiện và bắt đầu lại
        if (currentState == State.IDLE && PeoClient.CFG.nuker == false && nukerWasEnabled) {
            // Nếu Nuker đã từng được bật và bây giờ bị tắt, có thể do kick
            currentState = State.WAITING_FOR_KICK;
            stateStartTime = System.currentTimeMillis();
            DiagnosticRecorder.get().record("PeoJoin", "Nuker turned off, assuming kick. Waiting...");
        }
    }

    private static void selectSkyblockServer() {
        // Giả định màn hình MultiplayerScreen có danh sách server
        // Cần tìm server có tên chứa "Skyblock" và click vào
        // Tạm thời dùng cách tìm button "Join Server" hoặc click vào entry đầu tiên
        // Code này có thể cần điều chỉnh theo giao diện thực tế
        Screen screen = mc.field_1755;
        if (screen instanceof MultiplayerScreen) {
            // Simulate clicking on a server entry (first entry)
            // Đây là placeholder, cần triển khai thực tế
        }
    }

    private static void clickJoinButton() {
        // Click nút "Join Server" trên màn hình multiplayer
        Screen screen = mc.field_1755;
        if (screen instanceof MultiplayerScreen) {
            // Tìm button có text "Join Server" và click
            // Placeholder
        }
    }

    private static void sendChatCommand(String command) {
        if (mc.field_1724 != null) {
            mc.field_1724.method_7353(class_2561.method_43470(command), true);
            // Actually send command via network
            // Use network handler to send chat packet
            if (mc.field_1724.field_6214 != null) {
                mc.field_1724.field_6214.method_10822(command);
            }
        }
    }

    public static void setServerName(String name) { serverName = name; }
    public static void setPreJoinDelay(int seconds) { preJoinDelay = seconds; }
    public static void setPostJoinDelay(int seconds) { postJoinDelay = seconds; }
    public static void setWalkForwardTicks(int ticks) { walkForwardTicks = ticks; }
    public static void setEnableNukerOnJoin(boolean enable) { enableNukerOnJoin = enable; }

    public static String getStatus() {
        if (!enabled.get()) return "OFF";
        return "State: " + currentState + " (Tick: " + waitTicks + ")";
    }
}