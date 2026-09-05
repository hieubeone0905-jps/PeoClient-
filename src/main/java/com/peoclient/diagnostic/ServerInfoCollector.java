package com.peoclient.diagnostic;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thụ động ghi lại thông tin server từ các packet Plugin Message và các nguồn khác.
 * KHÔNG gửi bất kỳ lệnh hay packet nào.
 */
public final class ServerInfoCollector {
    private static final ServerInfoCollector instance = new ServerInfoCollector();

    private final ConcurrentLinkedQueue<PluginMessage> pluginMessages = new ConcurrentLinkedQueue<>();
    private volatile String serverBrand = "unknown";
    private volatile String serverAddress = "unknown";
    private volatile long connectedTime = 0;
    private volatile String disconnectReason = null;
    private volatile boolean kicked = false;

    private ServerInfoCollector() {}

    public static ServerInfoCollector get() {
        return instance;
    }

    public void onConnect(String address) {
        serverAddress = address;
        connectedTime = System.currentTimeMillis();
        pluginMessages.clear();
        serverBrand = "unknown";
        disconnectReason = null;
        kicked = false;
        DiagnosticRecorder.get().record("ServerInfo", "Connected to " + address);
    }

    public void onDisconnect(String reason) {
        this.disconnectReason = reason;
        kicked = true;
        DiagnosticRecorder.get().record("ServerInfo", "Disconnected: " + reason);
    }

    public void recordPluginMessage(String channel, byte[] data) {
        if (channel == null) return;
        // Lọc các plugin phổ biến
        String displayName = channel;
        String pluginName = detectPluginName(channel);
        if (pluginName != null) displayName = pluginName;

        PluginMessage msg = new PluginMessage(
                System.currentTimeMillis(),
                channel,
                displayName,
                data != null ? data.length : 0
        );
        pluginMessages.add(msg);
        while (pluginMessages.size() > 200) pluginMessages.poll(); // giới hạn

        // Ghi log nếu là plugin quan trọng
        if (isImportantChannel(channel)) {
            DiagnosticRecorder.get().record("PluginMessage",
                    "[" + displayName + "] " + channel + " (" + (data != null ? data.length : 0) + " bytes)");
        }
    }

    public void setServerBrand(String brand) {
        if (brand != null && !brand.isBlank()) {
            this.serverBrand = brand;
            DiagnosticRecorder.get().record("ServerInfo", "Server brand: " + brand);
        }
    }

    private String detectPluginName(String channel) {
        if (channel == null) return null;
        String lower = channel.toLowerCase(Locale.ROOT);
        if (lower.contains("grim")) return "Grim";
        if (lower.contains("vulcan")) return "Vulcan";
        if (lower.contains("ncp") || lower.contains("nocheat")) return "NoCheatPlus";
        if (lower.contains("anti")) return "AntiCheat";
        if (lower.contains("labymod")) return "LabyMod";
        if (lower.contains("viaversion")) return "ViaVersion";
        if (lower.contains("geyser")) return "Geyser";
        if (lower.contains("floodgate")) return "Floodgate";
        if (lower.contains("mc")) return "Minecraft";
        if (lower.contains("brand")) return "Brand";
        return null;
    }

    private boolean isImportantChannel(String channel) {
        if (channel == null) return false;
        String lower = channel.toLowerCase(Locale.ROOT);
        return lower.contains("grim") || lower.contains("vulcan") ||
               lower.contains("ncp") || lower.contains("nocheat") ||
               lower.contains("anti") || lower.contains("labymod") ||
               lower.contains("via") || lower.contains("geyser");
    }

    public ConcurrentLinkedQueue<PluginMessage> getPluginMessages() {
        return new ConcurrentLinkedQueue<>(pluginMessages);
    }

    public String getServerBrand() {
        return serverBrand;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    public boolean wasKicked() {
        return kicked;
    }

    public long getConnectedTime() {
        return connectedTime;
    }

    public List<String> getDetectedPlugins() {
        Set<String> set = new LinkedHashSet<>();
        for (PluginMessage msg : pluginMessages) {
            if (msg.displayName() != null && !msg.displayName().equals("unknown")) {
                set.add(msg.displayName());
            }
        }
        return new ArrayList<>(set);
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server: ").append(serverAddress).append("\n");
        sb.append("Brand: ").append(serverBrand).append("\n");
        sb.append("Connected: ").append(new Date(connectedTime)).append("\n");
        sb.append("Kicked: ").append(kicked).append("\n");
        if (disconnectReason != null) sb.append("Reason: ").append(disconnectReason).append("\n");
        sb.append("Plugins detected: ").append(getDetectedPlugins()).append("\n");
        sb.append("Total plugin messages: ").append(pluginMessages.size());
        return sb.toString();
    }

    public void reset() {
        pluginMessages.clear();
        serverBrand = "unknown";
        disconnectReason = null;
        kicked = false;
        connectedTime = 0;
    }
}