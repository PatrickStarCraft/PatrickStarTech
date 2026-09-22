package com.gregtechceu.gtceu.utils.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

/** JSON persistence for context-free UI text (not registry-dependent item/entity hover payloads). */
public final class ComponentJson {
    private ComponentJson() {}

    public static String toJson(Component component) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow().toString();
    }

    public static MutableComponent fromJson(String json) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow().copy();
    }
}
