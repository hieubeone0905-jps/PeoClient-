package com.peoclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Clean, non-overlapping account/proxy manager. Username profiles are persisted in peoclient.json. */
public final class AccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget proxyField;
    private boolean randomProxy;
    private String status = "";
    private int scroll;

    public AccountScreen(Screen parent) {
        super(Text.literal("PeoClient Accounts & Network"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        int panelW = Math.min(760, width - 80);
        int x = width / 2 - panelW / 2;
        int top = 36;

        nameField = new TextFieldWidget(textRenderer, x + 18, top + 54, panelW - 36, 22, Text.literal("Username"));
        nameField.setMaxLength(16);
        nameField.setText(PeoClient.CFG.usernameOverride == null ? "" : PeoClient.CFG.usernameOverride);
        nameField.setPlaceholder(Text.literal("3-16 letters, numbers or _"));
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save account"), b -> saveAccount()).dimensions(x + 18, top + 82, 130, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear field"), b -> nameField.setText("")).dimensions(x + 156, top + 82, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset to launcher"), b -> {
            PeoClient.setUsernameOverride("");
            nameField.setText("");
            status = "Launcher account restored.";
        }).dimensions(x + 274, top + 82, 150, 20).build());

        proxyField = new TextFieldWidget(textRenderer, x + 18, top + 138, panelW - 36, 22, Text.literal("Proxy pool"));
        proxyField.setMaxLength(8192);
        proxyField.setText(String.join(", ", safeList(PeoClient.CFG.proxyList)));
        proxyField.setPlaceholder(Text.literal("socks5://host:port, host:port"));
        addDrawableChild(proxyField);

        randomProxy = PeoClient.CFG.randomProxy;
        addDrawableChild(ButtonWidget.builder(Text.literal(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"), b -> {
            randomProxy = !randomProxy;
            b.setMessage(Text.literal(randomProxy ? "Random proxy: ON" : "Random proxy: OFF"));
        }).dimensions(x + 18, top + 166, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save network"), b -> saveNetwork()).dimensions(x + 176, top + 166, 130, 20).build());

        // Saved account buttons are created after the text widgets and laid out vertically.
        rebuildAccountButtons(x, top);
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(x + panelW - 108, height - 34, 90, 20).build());
    }

    private void rebuildAccountButtons(int x, int top) {
        int y = top + 226 - scroll;
        int panelW = Math.min(760, width - 80);
        List<String> accounts = safeList(PeoClient.CFG.savedAccounts);
        int maxVisible = 20;
        for (int i = 0; i < Math.min(maxVisible, accounts.size()); i++) {
            final String account = accounts.get(i);
            int yy = y + i * 27;
            if (yy < top + 214 || yy > height - 50) continue;
            addDrawableChild(ButtonWidget.builder(Text.literal("Use  " + account), b -> {
                nameField.setText(account);
                PeoClient.setUsernameOverride(account);
                status = "Switched to " + account + ".";
                saveAccount();
            }).dimensions(x + 18, yy, panelW - 120, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> removeAccount(account))
                    .dimensions(x + panelW - 92, yy, 28, 22).build());
        }
    }

    private void saveAccount() {
        String name = nameField.getText().trim();
        if (!name.isEmpty() && !name.matches("[A-Za-z0-9_]{3,16}")) {
            status = "Username must be 3-16 letters, numbers or _.";
            return;
        }
        PeoClient.setUsernameOverride(name);
        if (!name.isEmpty()) {
            PeoClient.CFG.savedAccounts.removeIf(v -> v != null && v.equalsIgnoreCase(name));
            PeoClient.CFG.savedAccounts.add(0, name);
            while (PeoClient.CFG.savedAccounts.size() > 20) PeoClient.CFG.savedAccounts.remove(PeoClient.CFG.savedAccounts.size() - 1);
        }
        PeoClient.CFG.save();
        status = name.isEmpty() ? "Launcher account restored." : "Account saved: " + name;
        refresh();
    }

    private void removeAccount(String account) {
        PeoClient.CFG.savedAccounts.removeIf(v -> v != null && v.equalsIgnoreCase(account));
        PeoClient.CFG.save();
        status = "Removed: " + account;
        if (PeoClient.CFG.usernameOverride.equalsIgnoreCase(account)) {
            PeoClient.setUsernameOverride("");
            nameField.setText("");
        }
        refresh();
    }

    private void saveNetwork() {
        List<String> pool = new ArrayList<>();
        if (!proxyField.getText().isBlank()) {
            Arrays.stream(proxyField.getText().split(","))
                    .map(String::trim)
                    .filter(this::validProxy)
                    .forEach(pool::add);
        }
        PeoClient.CFG.proxyList = pool;
        PeoClient.CFG.randomProxy = randomProxy;
        PeoClient.CFG.save();
        PeoClient.applyProxySettings(client);
        status = pool.isEmpty() ? "Proxy pool is empty." : (randomProxy ? "Random proxy will be selected for the next connection." : "Proxy pool saved.");
    }

    private boolean validProxy(String s) {
        if (s.isBlank()) return false;
        String value = s;
        int proto = value.indexOf("://");
        if (proto >= 0) value = value.substring(proto + 3);
        int colon = value.lastIndexOf(':');
        if (colon <= 0 || colon == value.length() - 1) return false;
        try { int port = Integer.parseInt(value.substring(colon + 1)); return port > 0 && port <= 65535; }
        catch (NumberFormatException ex) { return false; }
    }

    private List<String> safeList(List<String> value) {
        return value == null ? new ArrayList<>() : value;
    }

    @Override
    public void render(DrawContext d, int mouseX, int mouseY, float delta) {
        d.fill(0, 0, width, height, 0xD90A0F14);
        int panelW = Math.min(760, width - 80);
        int x = width / 2 - panelW / 2;
        int top = 36;
        int bottom = height - 42;

        d.fill(x, top, x + panelW, bottom, 0xF010171F);
        d.drawBorder(x, top, panelW, bottom - top, 0xFF30475A);
        d.drawTextWithShadow(textRenderer, Text.literal("PeoClient Settings").styled(st -> st.withBold(true)), x + 18, top + 16, 0xFFFFFFFF);
        d.drawTextWithShadow(textRenderer, "Saved accounts", x + 18, top + 196, 0xFFBBC5CD);
        d.fill(x + 18, top + 212, x + panelW - 18, top + 213, 0xFF30475A);
        d.drawTextWithShadow(textRenderer, "A username profile is remembered locally and can be switched with one click.", x + 18, top + 112, 0xFF87949E);
        d.drawTextWithShadow(textRenderer, "Proxy pool: working SOCKS proxies are required; a client cannot invent a public IP.", x + 18, top + 188, 0xFF87949E);
        if (!status.isBlank()) d.drawTextWithShadow(textRenderer, status, x + 18, bottom - 18, 0xFFFFFFFF);
        super.render(d, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelW = Math.min(760, width - 80);
        int x = width / 2 - panelW / 2;
        if (mouseX >= x && mouseX <= x + panelW && mouseY >= 250 && mouseY <= height - 45) {
            int count = safeList(PeoClient.CFG.savedAccounts).size();
            int max = Math.max(0, count * 27 - (height - 300));
            scroll = (int)Math.max(0, Math.min(max, scroll - verticalAmount * 27));
            refresh();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void refresh() {
        // Rebuild only the saved-account button rows while retaining text fields/network controls.
        // Minecraft screen widgets are rebuilt safely through init.
        init();
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
