package com.gregtechceu.gtceu.api.sync_system.data_transformers.gtceu;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.sync_system.data_transformers.ValueTransformer;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;

public class GTRecipeTransformer implements ValueTransformer<GTRecipe> {

    private static @Nullable RecipeManager getRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getRecipeManager();
    }

    @Override
    public Tag serializeNBT(GTRecipe value, ValueTransformer.TransformerContext<GTRecipe> context) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", value.id.toString());
        tag.put("recipe",
                GTRecipeSerializer.CODEC.encodeStart(context.nbtOps(), value).result().orElse(new CompoundTag()));
        tag.putInt("ocLevel", value.ocLevel);
        return tag;
    }

    @Override
    public @Nullable GTRecipe deserializeNBT(Tag tag, ValueTransformer.TransformerContext<GTRecipe> context) {
        if (tag instanceof CompoundTag comp && comp.isEmpty()) return null;
        GTRecipe result = null;
        if (tag instanceof CompoundTag compoundTag) {
            result = GTRecipeSerializer.CODEC.parse(context.nbtOps(), compoundTag.get("recipe")).result().orElse(null);
            if (result != null) {
                result.id = Identifier.parse(compoundTag.getStringOr("id", ""));
                result.ocLevel = compoundTag.getIntOr("ocLevel", 0);
            }
        } else if (tag instanceof StringTag stringTag) { // Backwards Compatibility
            Identifier id = Identifier.parse(stringTag.asString().orElseThrow());
            RecipeManager recipeManager = getRecipeManager();
            var recipe = recipeManager == null ? null : recipeManager
                    .byKey(ResourceKey.create(Registries.RECIPE, id))
                    .map(RecipeHolder::value)
                    .orElse(null);
            if (recipe instanceof GTRecipe gtRecipe) {
                result = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                result = GTRecipeTypes.FURNACE_RECIPES.toGTrecipe(Identifier.parse(stringTag.asString().orElseThrow()),
                        smeltingRecipe);
            }
        } else if (tag instanceof ByteArrayTag byteArray) { // Backwards Compatibility
            RecipeManager recipeManager = getRecipeManager();
            if (recipeManager == null) return null;
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            result = (GTRecipe) recipeManager
                    .byKey(ResourceKey.create(Registries.RECIPE, buf.readIdentifier()))
                    .map(RecipeHolder::value)
                    .orElse(null);
            buf.release();
        }
        return result;
    }

    @Override
    public void writeToPacket(FriendlyByteBuf buf, GTRecipe value, TransformerContext<GTRecipe> context) {
        GTRecipeSerializer.toNetwork(buf, value);
    }

    @Override
    public @Nullable GTRecipe readFromPacket(FriendlyByteBuf buf, TransformerContext<GTRecipe> context) {
        return GTRecipeSerializer.fromNetworkWithoutDatapackSync(buf);
    }
}
