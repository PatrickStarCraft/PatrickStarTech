package com.gregtechceu.gtceu.api;

/** Shared voltage table, independent of configuration and mod initialization. */
public final class GTVoltages {

    private GTVoltages() {}

    public static final long[] V = { 8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608,
            33554432, 134217728, 536870912, 2147483648L };
}
