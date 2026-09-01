package com.peoclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** Clean account/network manager with fixed, non-overlapping two-column layout. */
public final class AccountScreen extends class_437 {
    private final class_437 parent;
    private class_342 nameField;
    private class_342 proxyField;
    private boolean randomProxy;
    private String status = "";

    public AccountScreen(class_437 parent) {
        super(class_2561.method_43470("PeoClient Accounts"));
        this.parent = parent;
    }

    @Override
    protected void method_25426() {
        method_37067();

        int gap = 14;
        int margin = 26;
        int usable = field_22789 - margin * 2 - gap;
        int colW = Math.max(260, usable / 2);
        int leftX = margin;
        int rightX = leftX + colW + gap;
        int top = 42;
        int bottom = field_22790 - 38;

        // Accounts column.
        nameField = new class_342(field_22793, leftX + 14, top + 50, colW - 28, 22, class_2561.method_43470("Username"));
        nameField.method_1880(16);
        nameField.method_1852(PeoClient.CFG.usernameOverride == null ? "" : PeoClient.CFG.usernameOverride);
        nameField.method_47404(class_2561.method_43470("3-16 letters, numbers or _"));
        method_37063(nameField);

        int by = top + 80;
        int bw = (colW - 38) / 3;
        method_37063(class_4185.method_46430(class_2561.method_43470("Save"), b -> saveAccount())
                .method_46434(leftX + 14, by, bw, 20).method_46431());
        method_37063(class_4185.method_46430(class_2561.method_43470("Clear"), b -> nameField.method_1852(""))
                .method_46434(leftX + 19 + bw, by, bw, 20).method_46431());
        method_37063(class_4185.method_46430(class_2561.method_43470("Launcher"), b -> {
            PeoClient.setUsernameOverride("");
            nameField.method_1852("");
            status = "Launcher account selected.";
        }).method_46434(leftX + 24 + bw * 2, by, bw, 20).method_46431());

        // Saved account buttons. Keep them inside the left column with no scrolling.
        List<String> accounts = safeList(PeoClient.CFG.savedAccounts);
        int rowY = top + 128;
        int rowH = 24;
        int maxRows = Math.max(1, (bottom - rowY - 34) / rowH);
        int visible = Math.min(maxRows, 10);
        for (int i = 0; i < visible; i++) {
            String account = i < accounts.size() ? accounts.get(i) : null;
            if (account == null || account.isBlank()) {
                continue;
            }
            final String selectedAccount = account;
            int yy = rowY + i * rowH;
            method_37063(class_4185.method_46430(class_2561.method_43470("Use  " + selectedAccount), b -> useAccount(selectedAccount))
                    .method_46434(leftX + 14, yy, colW - 76, 20).method_46431());
            method_37063(class_4185.method_46430(class_2561.method_43470("X"), b -> removeAccount(selectedAccount))
                    .method_46434(leftX + colW - 54, yy, 40, 20).method_46431());
        }

        // Network column.
        proxyField = new class_342(field_22793, rightX + 14, top + 50, colW - 28, 22, class_2561.method_43470("Proxy pool"));
        proxyField.method_1880(8192);
        proxyField.method_1852(String.join(", ", safeList(PeoClient.CFG.proxyList)));
        proxyField.method_47404(class_2561.method_43470("socks5://host:port, host:port"));
        method_37063(proxyField);

        randomProxy = PeoClient.CFG.randomProxy;
        method_37063(class_4185.method_46430(class_2561.method_43470(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"), b -> {
            randomProxy = !randomProxy;
            b.method_25355(class_2561.method_43470(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"));
        }).method_46434(rightX + 14, top + 82, colW - 28, 20).method_46431());

        method_37063(class_4185.method_46430(class_2561.method_43470("Save network"), b -> saveNetwork())
                .method_46434(rightX + 14, top + 108, colW - 28, 20).method_46431());

        int noteY = top + 150;
        // Fixed-height informational area; never overlaps controls.
        method_37063(class_4185.method_46430(class_2561.method_43470("Back"), b -> field_22787.method_1507(parent))
                .method_46434(field_22789 / 2 - 45, bottom + 8, 90, 20).method_46431());
    }

    private void useAccount(String account) {
        nameField.method_1852(account);
        PeoClient.setUsernameOverride(account);
        PeoClient.CFG.save();
        status = "Switched to " + account + ".";
    }

    private void saveAccount() {
        String name = nameField.method_1882().trim();
        if (!name.isEmpty() && !name.matches("[A-Za-z0-9_]{3,16}")) {
            status = "Username must be 3-16 letters, numbers or _.";
            return;
        }
        PeoClient.setUsernameOverride(name);
        if (!name.isEmpty()) {
            if (PeoClient.CFG.savedAccounts == null) PeoClient.CFG.savedAccounts = new ArrayList<>();
            PeoClient.CFG.savedAccounts.removeIf(v -> v != null && v.equalsIgnoreCase(name));
            PeoClient.CFG.savedAccounts.add(0, name);
            while (PeoClient.CFG.savedAccounts.size() > 20) {
                PeoClient.CFG.savedAccounts.remove(PeoClient.CFG.savedAccounts.size() - 1);
            }
        }
        PeoClient.CFG.save();
        status = name.isEmpty() ? "Launcher account selected." : "Saved: " + name;
        method_25426();
    }

    private void removeAccount(String account) {
        if (PeoClient.CFG.savedAccounts != null) {
            PeoClient.CFG.savedAccounts.removeIf(v -> v != null && v.equalsIgnoreCase(account));
        }
        if (PeoClient.CFG.usernameOverride != null && PeoClient.CFG.usernameOverride.equalsIgnoreCase(account)) {
            PeoClient.setUsernameOverride("");
        }
        PeoClient.CFG.save();
        status = "Removed: " + account;
        method_25426();
    }

    private void saveNetwork() {
        List<String> pool = new ArrayList<>();
        String raw = proxyField.method_1882();
        if (raw != null && !raw.isBlank()) {
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(this::validProxy)
                    .forEach(pool::add);
        }
        PeoClient.CFG.proxyList = pool;
        PeoClient.CFG.randomProxy = randomProxy;
        PeoClient.CFG.save();
        PeoClient.applyProxySettings(field_22787);
        status = pool.isEmpty()
                ? "Proxy pool empty. No IP change is possible without real proxies."
                : (randomProxy ? "Random proxy saved for the next connection." : "Proxy pool saved.");
    }

    private boolean validProxy(String s) {
        if (s == null || s.isBlank()) return false;
        String value = s;
        int proto = value.indexOf("://");
        if (proto >= 0) value = value.substring(proto + 3);
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) return false;
        try {
            int port = Integer.parseInt(value.substring(colon + 1));
            return port > 0 && port <= 65535;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private List<String> safeList(List<String> value) {
        return value == null ? new ArrayList<>() : value;
    }

    @Override
    public void method_25394(class_332 d, int mouseX, int mouseY, float delta) {
        d.method_25294(0, 0, field_22789, field_22790, 0xD90A0F14);

        int gap = 14;
        int margin = 26;
        int usable = field_22789 - margin * 2 - gap;
        int colW = Math.max(260, usable / 2);
        int leftX = margin;
        int rightX = leftX + colW + gap;
        int top = 42;
        int bottom = field_22790 - 38;

        d.method_51439(field_22793, class_2561.method_43470("PeoClient Accounts & Network").method_27694(s -> s.method_10982(true)),
                field_22789 / 2 - field_22793.method_1727("PeoClient Accounts & Network") / 2, 18, 0xFFFFFFFF, false);

        drawPanel(d, leftX, top, colW, bottom - top, "Accounts");
        drawPanel(d, rightX, top, colW, bottom - top, "Network");

        d.method_51433(field_22793, "Current username", leftX + 14, top + 26, 0xFFD7DDE1, false);
        d.method_51433(field_22793, "Saved accounts", leftX + 14, top + 112, 0xFFD7DDE1, false);
        d.method_51433(field_22793, "SOCKS proxy pool", rightX + 14, top + 26, 0xFFD7DDE1, false);
        d.method_51433(field_22793, "Enter multiple proxies separated by commas.", rightX + 14, top + 138, 0xFF8F9AA3, false);
        d.method_51433(field_22793, "Random selection applies to the next connection.", rightX + 14, top + 156, 0xFF8F9AA3, false);

        if (!status.isBlank()) {
            int sw = field_22793.method_1727(status);
            d.method_51433(field_22793, status, Math.max(10, field_22789 / 2 - sw / 2), field_22790 - 24, 0xFFFFFFFF, false);
        }
        super.method_25394(d, mouseX, mouseY, delta);
    }

    private void drawPanel(class_332 d, int x, int y, int w, int h, String title) {
        d.method_25294(x, y, x + w, y + h, 0xF010171F);
        d.method_49601(x, y, w, h, 0xFF2E465A);
        d.method_51439(field_22793, class_2561.method_43470(title).method_27694(s -> s.method_10982(true)), x + 14, y + 8, 0xFFFFFFFF, false);
    }

    @Override
    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            field_22787.method_1507(parent);
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }
}
