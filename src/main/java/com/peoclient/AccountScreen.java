package com.peoclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Clean account/network manager with fixed, non-overlapping two-column layout. */
public final class AccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget proxyField;
    private boolean randomProxy;
    private String status = "";

    public AccountScreen(Screen parent) {
        super(Text.literal("PeoClient Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();

        int gap = 14;
        int margin = 26;
        int usable = width - margin * 2 - gap;
        int colW = Math.max(260, usable / 2);
        int leftX = margin;
        int rightX = leftX + colW + gap;
        int top = 42;
        int bottom = height - 38;

        // Accounts column.
        nameField = new TextFieldWidget(textRenderer, leftX + 14, top + 50, colW - 28, 22, Text.literal("Username"));
        nameField.setMaxLength(16);
        nameField.setText(PeoClient.CFG.usernameOverride == null ? "" : PeoClient.CFG.usernameOverride);
        nameField.setPlaceholder(Text.literal("3-16 letters, numbers or _"));
        addDrawableChild(nameField);

        int by = top + 80;
        int bw = (colW - 38) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAccount())
                .dimensions(leftX + 14, by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> nameField.setText(""))
                .dimensions(leftX + 19 + bw, by, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Launcher"), b -> {
            PeoClient.setUsernameOverride("");
            nameField.setText("");
            status = "Launcher account selected.";
        }).dimensions(leftX + 24 + bw * 2, by, bw, 20).build());

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
            addDrawableChild(ButtonWidget.builder(Text.literal("Use  " + selectedAccount), b -> useAccount(selectedAccount))
                    .dimensions(leftX + 14, yy, colW - 76, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> removeAccount(selectedAccount))
                    .dimensions(leftX + colW - 54, yy, 40, 20).build());
        }

        // Network column.
        proxyField = new TextFieldWidget(textRenderer, rightX + 14, top + 50, colW - 28, 22, Text.literal("Proxy pool"));
        proxyField.setMaxLength(8192);
        proxyField.setText(String.join(", ", safeList(PeoClient.CFG.proxyList)));
        proxyField.setPlaceholder(Text.literal("socks5://host:port, host:port"));
        addDrawableChild(proxyField);

        randomProxy = PeoClient.CFG.randomProxy;
        addDrawableChild(ButtonWidget.builder(Text.literal(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"), b -> {
            randomProxy = !randomProxy;
            b.setMessage(Text.literal(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"));
        }).dimensions(rightX + 14, top + 82, colW - 28, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Save network"), b -> saveNetwork())
                .dimensions(rightX + 14, top + 108, colW - 28, 20).build());

        int noteY = top + 150;
        // Fixed-height informational area; never overlaps controls.
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(width / 2 - 45, bottom + 8, 90, 20).build());
    }

    private void useAccount(String account) {
        nameField.setText(account);
        PeoClient.setUsernameOverride(account);
        PeoClient.CFG.save();
        status = "Switched to " + account + ".";
    }

    private void saveAccount() {
        String name = nameField.getText().trim();
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
        init();
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
        init();
    }

    private void saveNetwork() {
        List<String> pool = new ArrayList<>();
        String raw = proxyField.getText();
        if (raw != null && !raw.isBlank()) {
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(this::validProxy)
                    .forEach(pool::add);
        }
        PeoClient.CFG.proxyList = pool;
        PeoClient.CFG.randomProxy = randomProxy;
        PeoClient.CFG.save();
        PeoClient.applyProxySettings(client);
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
    public void render(DrawContext d, int mouseX, int mouseY, float delta) {
        d.fill(0, 0, width, height, 0xD90A0F14);

        int gap = 14;
        int margin = 26;
        int usable = width - margin * 2 - gap;
        int colW = Math.max(260, usable / 2);
        int leftX = margin;
        int rightX = leftX + colW + gap;
        int top = 42;
        int bottom = height - 38;

        d.drawText(textRenderer, Text.literal("PeoClient Accounts & Network").styled(s -> s.withBold(true)),
                width / 2 - textRenderer.getWidth("PeoClient Accounts & Network") / 2, 18, 0xFFFFFFFF, false);

        drawPanel(d, leftX, top, colW, bottom - top, "Accounts");
        drawPanel(d, rightX, top, colW, bottom - top, "Network");

        d.drawText(textRenderer, "Current username", leftX + 14, top + 26, 0xFFD7DDE1, false);
        d.drawText(textRenderer, "Saved accounts", leftX + 14, top + 112, 0xFFD7DDE1, false);
        d.drawText(textRenderer, "SOCKS proxy pool", rightX + 14, top + 26, 0xFFD7DDE1, false);
        d.drawText(textRenderer, "Enter multiple proxies separated by commas.", rightX + 14, top + 138, 0xFF8F9AA3, false);
        d.drawText(textRenderer, "Random selection applies to the next connection.", rightX + 14, top + 156, 0xFF8F9AA3, false);

        if (!status.isBlank()) {
            int sw = textRenderer.getWidth(status);
            d.drawText(textRenderer, status, Math.max(10, width / 2 - sw / 2), height - 24, 0xFFFFFFFF, false);
        }
        super.render(d, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext d, int x, int y, int w, int h, String title) {
        d.fill(x, y, x + w, y + h, 0xF010171F);
        d.drawBorder(x, y, w, h, 0xFF2E465A);
        d.drawText(textRenderer, Text.literal(title).styled(s -> s.withBold(true)), x + 14, y + 8, 0xFFFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
