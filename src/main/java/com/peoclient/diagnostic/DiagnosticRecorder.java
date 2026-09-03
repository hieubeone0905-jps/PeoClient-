package com.peoclient.diagnostic;

import net.minecraft.class_310;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class DiagnosticRecorder {
    private static final DiagnosticRecorder instance = new DiagnosticRecorder();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private Path logFile;
    private boolean initialized = false;
    private long lastFlushMs = 0L;

    private DiagnosticRecorder() {}

    public static DiagnosticRecorder get() { return instance; }

    public void init() {
        if (initialized) return;
        try {
            Path dir = class_310.method_1551().field_1697.toPath().resolve("logs/peoclient");
            Files.createDirectories(dir);
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            logFile = dir.resolve("diagnostic_" + date + ".log");
            initialized = true;
        } catch (IOException ignored) {}
    }

    public void record(String message) {
        if (!DiagnosticConfig.get().isEnabled()) return;
        if (!initialized) init();
        String line = LocalDateTime.now().format(timestampFormat) + " [DIAG] " + message;
        logQueue.add(line);
        // Keep the diagnostic file live while Minecraft is running.
        // Do not wait for the queue to reach 10,000 entries.
        long now = System.currentTimeMillis();
        if (logQueue.size() >= 1 && (now - lastFlushMs >= 250L || logQueue.size() >= 250)) {
            flush();
            lastFlushMs = now;
        }
    }

    public void record(String category, String message) {
        record("[" + category + "] " + message);
    }

    public void flush() {
        if (logFile == null || logQueue.isEmpty()) return;
        try {
            if (logFile.getParent() != null) Files.createDirectories(logFile.getParent());
        } catch (IOException ignored) {}
        try (BufferedWriter w = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String line;
            while ((line = logQueue.poll()) != null) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException ignored) {}
    }

    public int getPendingSize() { return logQueue.size(); }

    public Path getLogFile() { return logFile; }
}