package com.peoclient.diagnostic;

import net.minecraft.class_310;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class KickLogManager {
    private static final KickLogManager instance = new KickLogManager();
    private Path logDir;

    private KickLogManager() {}

    public static KickLogManager get() { return instance; }

    public void init() {
        try {
            logDir = class_310.method_1551().field_1697.toPath().resolve("logs/peoclient/kicks");
            Files.createDirectories(logDir);
        } catch (IOException ignored) {}
    }

    public Path createLogFile(String accountName) {
        if (logDir == null) init();
        String safeAccount = accountName.replaceAll("[^a-zA-Z0-9_]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = timestamp + "_" + safeAccount + ".log";
        Path file = logDir.resolve(filename);
        int counter = 1;
        while (Files.exists(file)) {
            filename = timestamp + "_" + safeAccount + "_" + (counter++) + ".log";
            file = logDir.resolve(filename);
        }
        return file;
    }

    public void writeLog(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException ignored) {}
    }

    public void cleanupOldLogs(int retentionDays) {
        if (logDir == null) return;
        try {
            long cutoff = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000;
            Files.list(logDir).filter(p -> p.toString().endsWith(".log"))
                 .filter(p -> p.toFile().lastModified() < cutoff)
                 .forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }
}