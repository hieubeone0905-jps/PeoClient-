package com.peoclient.diagnostic;

public record PluginMessage(
    long timestamp,
    String channel,
    String displayName,
    int dataLength
) {}