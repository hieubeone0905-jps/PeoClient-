package com.peoclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;

/** PeoClient account/network settings screen. */
public final class AccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget proxyField;
    private boolean randomProxy;
    private String status = "";

    public AccountScreen(Screen parent) {
        super(Text.literal("PeoClient Account & Network"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(520, width - 80);
        int x = width / 2 - w / 2;
        int y = height / 2 - 120;

        nameField = new TextFieldWidget(textRenderer, x, y + 62, w, 22,
                Text.literal("Minecraft username"));
        nameField.setMaxLength(16);
        nameField.setText(PeoClient.CFG.usernameOverride == null ? "" : PeoClient.CFG.usernameOverride);
        nameField.setPlaceholder(Text.literal("Leave empty to use launcher account"));
        addDrawableChild(nameField);

        proxyField = new TextFieldWidget(textRenderer, x, y + 140, w, 22,
                Text.literal("Proxy list"));
        proxyField.setMaxLength(4096);
        proxyField.setText(String.join(", ", PeoClient.CFG.proxyList == null ? new ArrayList<>() : PeoClient.CFG.proxyList));
        proxyField.setPlaceholder(Text.literal("socks5://host:port, socks5://host:port"));
        addDrawableChild(proxyField);

        randomProxy = PeoClient.CFG.randomProxy;

        addDrawableChild(ButtonWidget.builder(Text.literal(randomProxy ? "Random Proxy/IP: ON" : "Random Proxy/IP: OFF"), b -> {
            randomProxy = !randomProxy;
            b.setMessage(Text.literal(randomProxy ? "Random Proxy/IP: ON" : "Random Proxy/IP: OFF"));
        }).dimensions(x, y + 172, 220, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply + Save"), b -> apply()).dimensions(x + w - 220, y + 172, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Username"), b -> nameField.setText(""))
                .dimensions(x, y + 204, 160, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(x + w - 160, y + 204, 160, 20).build());
    }

    private void apply() {
        String name = nameField.getText().trim();
        if (!name.matches("[A-Za-z0-9_]{0,16}")) {
            status = "Username: 3-16 letters/numbers/underscore only.";
            return;
        }

        PeoClient.setUsernameOverride(name);
        PeoClient.CFG.proxyList.clear();
        if (!proxyField.getText().isBlank()) {
            Arrays.stream(proxyField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(PeoClient.CFG.proxyList::add);
        }
        PeoClient.CFG.randomProxy = randomProxy;
        PeoClient.CFG.save();
        PeoClient.applyProxySettings(MinecraftClient.getInstance());
        status = randomProxy && !PeoClient.CFG.proxyList.isEmpty()
                ? "Saved. A random proxy is selected for the next connection."
                : "Saved.";
    }

    @Override
    public void render(DrawContext d, int mouseX, int mouseY, float delta) {
        d.fill(0, 0, width, height, 0xE50A0F14);
        int w = Math.min(560, width - 60);
        int x = width / 2 - w / 2;
        int y = height / 2 - 150;

        d.fill(x, y, x + w, y + 290, 0xF010171F);
        d.drawBorder(x, y, w, 290, 0xFF3A5366);
        d.drawTextWithShadow(textRenderer, "PeoClient Settings", x + 18, y + 18, 0xFFFFFFFF);
        d.drawTextWithShadow(textRenderer, "Account / Username", x + 18, y + 44, 0xFFB9C4CD);
        d.drawTextWithShadow(textRenderer, "Display name used by the client.", x + 18, y + 86, 0xFF84919C);
        d.drawTextWithShadow(textRenderer, "Proxy / IP pool", x + 18, y + 122, 0xFFB9C4CD);
        d.drawTextWithShadow(textRenderer, "One SOCKS5/host:port entry per comma.", x + 18, y + 164, 0xFF84919C);
        d.drawTextWithShadow(textRenderer, "Important: a client cannot invent a public IP. Random IP uses working proxies.", x + 18, y + 234, 0xFFFFFFFF);
        if (!status.isBlank()) d.drawTextWithShadow(textRenderer, status, x + 18, y + 258, 0xFFBFC9D1);

        super.render(d, mouseX, mouseY, delta);
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
