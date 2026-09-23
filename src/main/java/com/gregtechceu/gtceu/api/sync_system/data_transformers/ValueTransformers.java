package com.gregtechceu.gtceu.api.sync_system.data_transformers;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.ConsumedInputsData;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.TypeDeclaration;
import com.gregtechceu.gtceu.api.sync_system.data_transformers.collections.*;
import com.gregtechceu.gtceu.api.sync_system.data_transformers.gtceu.*;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.gregtechceu.gtceu.api.sync_system.NBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public final class ValueTransformers {

    private static final Map<Class<?>, ValueTransformer<?>> REGISTERED = new Reference2ReferenceOpenHashMap<>();
    private static final Map<Class<?>, Supplier<ValueTransformer<?>>> REGISTERED_SUPPLIERS = new Reference2ReferenceOpenHashMap<>();

    private static final Map<Type, Type> PRIMITIVE_TO_BOXED = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class);

    private static final Map<Type, ValueTransformer<?>> TYPE_CACHE = new Reference2ReferenceOpenHashMap<>();

    /**
     * Gets the {@link ValueTransformer} associated with a specific type.
     */
    public static @Nullable ValueTransformer<?> get(Type type) {
        if (type instanceof Class<?> cls) type = cls.isPrimitive() ? PRIMITIVE_TO_BOXED.get(cls) : cls;
        return TYPE_CACHE.computeIfAbsent(type, ValueTransformers::generateOrGetTransformer);
    }

    private static @Nullable ValueTransformer<?> generateOrGetTransformer(Type type) {
        TypeDeclaration declaration = new TypeDeclaration(type);
        Class<?> clazz = declaration.getClassValue();

        if (clazz != null && REGISTERED.containsKey(clazz)) return REGISTERED.get(clazz);

        if (clazz == null || clazz.isArray()) {
            ValueTransformer<?> componentTx = get(declaration.getArrayComponentType().getRawType());
            if (componentTx != null) return new ObjectArrayTransformer<>(componentTx);
            return null;
        }

        if (clazz.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
            return new EnumTransformer<>(enumClass);
        }

        for (var entry : REGISTERED_SUPPLIERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) return entry.getValue().get();
        }

        for (var entry : REGISTERED.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) return entry.getValue();
        }

        return null;
    }

    /**
     * Registers a {@link ValueTransformer} for the given class or interface.
     * If registering a type with generic arguments, instead use {@code registerGenericTransformerSupplier} to create a
     * new
     * transformer instance for each set of generic type arguments.
     * 
     * @param type        The class to register this {@link ValueTransformer} for
     * @param transformer The transformer being registered
     */
    public static void registerTransformer(Class<?> type, ValueTransformer<?> transformer) {
        if (REGISTERED.containsKey(type))
            throw new IllegalArgumentException("Attempted to register transformer for %s twice".formatted(type));
        REGISTERED.put(type, transformer);
    }

    public static <T> void registerCodecTransformer(Class<T> type, Codec<T> codec) {
        registerTransformer(type, new CodecTransformer<>(codec));
    }

    public static <T> void registerCodecTransformer(Class<T> type, Codec<T> codec,
                                                    BiConsumer<FriendlyByteBuf, T> writePacket,
                                                    Function<FriendlyByteBuf, @Nullable T> readPacket) {
        registerTransformer(type, new CodecTransformer<>(codec, writePacket, readPacket));
    }

    public static <T> void registerRegistryTransformer(Class<T> type, Registry<T> registry) {
        registerCodecTransformer(type, registry.byNameCodec(),
                (buf, value) -> buf.writeVarInt(registry.getId(value)),
                buf -> buf.readById(registry::byId));
    }

    /**
     * Registers a supplier that supplies instances of a specific transformer type.
     * The supplier will be called to create new instances of the transformer for each unique set of generic type
     * arguments passed to the given class.
     *
     * @param type The class to register this {@link ValueTransformer} supplier for
     * @param func Supplier function
     */
    public static <T> void registerGenericTransformerSupplier(Class<T> type, Supplier<ValueTransformer<?>> func) {
        if (REGISTERED_SUPPLIERS.containsKey(type))
            throw new IllegalArgumentException("Attempted to register transformer for %s twice".formatted(type));
        REGISTERED_SUPPLIERS.put(type, func);
    }

    static {

        //// Primitives

        // spotless:off

        registerCodecTransformer(Integer.class, Codec.INT, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
        registerCodecTransformer(Long.class, Codec.LONG, FriendlyByteBuf::writeVarLong, FriendlyByteBuf::readVarLong);
        registerCodecTransformer(Float.class, Codec.FLOAT, FriendlyByteBuf::writeFloat, FriendlyByteBuf::readFloat);
        registerCodecTransformer(Double.class, Codec.DOUBLE, FriendlyByteBuf::writeDouble, FriendlyByteBuf::readDouble);
        registerCodecTransformer(Short.class, Codec.SHORT, (buf, s) -> buf.writeShort(s), FriendlyByteBuf::readShort);
        registerCodecTransformer(Byte.class, Codec.BYTE, (buf, b) -> buf.writeByte(b), FriendlyByteBuf::readByte);
        registerCodecTransformer(Character.class, SyncSystemCodecs.CHAR, (buf, c) -> buf.writeChar(c), FriendlyByteBuf::readChar);
        registerCodecTransformer(Boolean.class, Codec.BOOL, FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);

        // Primtive arrays
        registerCodecTransformer(int[].class, Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of), FriendlyByteBuf::writeVarIntArray, FriendlyByteBuf::readVarIntArray);
        registerCodecTransformer(long[].class, Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of),
                (buf, value) -> buf.writeLongArray(value), buf -> buf.readLongArray());
        registerCodecTransformer(byte[].class, Codec.BYTE_BUFFER.xmap(ByteBuffer::array, ByteBuffer::wrap),
                (buf, value) -> buf.writeByteArray(value), buf -> buf.readByteArray());

        //// Java classes and standard minecraft/forge classes

        registerCodecTransformer(String.class, Codec.STRING, FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf);
        registerCodecTransformer(UUID.class, UUIDUtil.CODEC,
                (buf, value) -> buf.writeUUID(value), buf -> buf.readUUID());
        registerCodecTransformer(CompoundTag.class, CompoundTag.CODEC,
                (buf, value) -> buf.writeNbt(value), buf -> buf.readNbt());
        registerCodecTransformer(Identifier.class, Identifier.CODEC,
                (buf, value) -> Identifier.STREAM_CODEC.encode(buf, value),
                buf -> Identifier.STREAM_CODEC.decode(buf));

        registerRegistryTransformer(Item.class, BuiltInRegistries.ITEM);
        registerCodecTransformer(ItemStack.class, ItemStack.CODEC);
        registerRegistryTransformer(Fluid.class, BuiltInRegistries.FLUID);
        registerCodecTransformer(FluidStack.class, FluidStack.CODEC);
        registerCodecTransformer(Component.class, ComponentSerialization.CODEC);

        registerTransformer(BlockPos.class, new BlockPosTransformer());
        registerCodecTransformer(BlockState.class, BlockState.CODEC,
                (buf, value) -> buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(value)),
                buf -> buf.readById(Block.BLOCK_STATE_REGISTRY::byId));

        registerTransformer(NBTSerializable.class, new NBTSerializableTransformer());
        registerTransformer(ISyncManaged.class, new SyncDataHolder.SyncManagedTransformer());

        registerGenericTransformerSupplier(List.class, ListTransformer::new);
        registerGenericTransformerSupplier(Map.class, MapTransformer::new);
        registerGenericTransformerSupplier(Set.class, SetTransformer::new);

        //// GT specific classes

        registerTransformer(CoverBehavior.class, new CoverBehaviorTransformer());
        registerTransformer(GTRecipe.class, new GTRecipeTransformer());

        registerCodecTransformer(MachineRenderState.class, MachineRenderState.CODEC);
        registerTransformer(MonitorGroup.class, new MonitorGroupTransformer());
        registerCodecTransformer(ConsumedInputsData.class, ConsumedInputsData.CODEC);

        registerCodecTransformer(GTRecipeType.class, Identifier.CODEC.xmap(GTRegistries.RECIPE_TYPES::get, GTRecipeType::getRegistryName));
        registerCodecTransformer(Material.class, Identifier.CODEC.xmap(GTRegistries.MATERIALS::get, Material::getResourceLocation));
        // spotless:on
    }
}
