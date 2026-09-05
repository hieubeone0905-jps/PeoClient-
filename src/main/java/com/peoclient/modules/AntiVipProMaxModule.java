    private static int lastLoggedSuspicion = -1;
    private static long lastAutoAdjustLogMs = 0;

    public static void tick() {
        if (isEnabled() && !NukerBypassEngine.isEnabled()) updateEngine();
        if (isEnabled()) {
            NukerBypassEngine.tick();
            NukerBypassUltimateV2.tick();
        }
        if (isEnabled()) {
            if (PeoClient.CFG.bypassV2Enabled) {
                if (!NukerBypassUltimateV2.isActive()) setBypassV2(true);
            } else if (NukerBypassUltimateV2.isActive()) {
                setBypassV2(false);
            }
        }
        // AutoAdjust nâng cấp với success rate
        if (isEnabled() && isAutoAdjust()) {
            int susp = getSuspicionLevel();
            int ping = getPing();
            int rate = AntiKickEngine.getLastSuccessRate();
            if (rate < 90 && rate > 0) {
                // Nếu success rate thấp, tăng protection level
                AntiKickEngine.setProtectionLevel(Math.min(10, AntiKickEngine.getProtectionLevel() + 1));
            } else if (rate > 97) {
                // Nếu quá cao, giảm intensity
                NukerBypassUltimateV2.setIntensity(Math.max(1, NukerBypassUltimateV2.getIntensity() - 1));
            }
            if (susp > 5 || ping > 200) {
                NukerBypassUltimateV2.setIntensity(Math.min(10, PeoClient.CFG.bypassV2Intensity + 1));
                AntiKickEngine.setProtectionLevel(Math.min(10, AntiKickEngine.getProtectionLevel() + 1));
            } else if (susp < 3 && ping < 100) {
                NukerBypassUltimateV2.setIntensity(Math.max(1, PeoClient.CFG.bypassV2Intensity - 1));
                AntiKickEngine.setProtectionLevel(Math.max(1, AntiKickEngine.getProtectionLevel() - 1));
            }
            if (System.currentTimeMillis() - lastAutoAdjustLogMs > 30000) {
                lastAutoAdjustLogMs = System.currentTimeMillis();
                DiagnosticRecorder.get().record("AutoAdjust", "susp=" + susp + " ping=" + ping +
                    " v2I=" + NukerBypassUltimateV2.getIntensity() +
                    " akeL=" + AntiKickEngine.getProtectionLevel() +
                    " successRate=" + rate + "%");
            }
        }
    }