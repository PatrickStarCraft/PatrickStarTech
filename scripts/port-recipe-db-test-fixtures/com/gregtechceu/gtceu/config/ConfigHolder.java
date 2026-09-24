package com.gregtechceu.gtceu.config;

public final class ConfigHolder {

    public static final ConfigHolder INSTANCE = new ConfigHolder();
    public final Dev dev = new Dev();

    private ConfigHolder() {}

    public static final class Dev {
        public boolean debug;
    }
}
