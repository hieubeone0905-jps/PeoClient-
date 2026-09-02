package com.peoclient.diagnostic;

/** Builds the human-readable disconnect report. */
public final class KickLogFormatter {
    private KickLogFormatter() {}

    public static String formatReport() {
        return KickDiagnostics.get().buildReport();
    }
}
