package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Blockstate variants with explicit overlap and coverage checks against the real block state definition. */
public final class VariantBlockStateBuilder {

    private final Block block;
    private final Map<Map<Property<?>, Comparable<?>>, ConfiguredModelList> variants = new LinkedHashMap<>();

    public VariantBlockStateBuilder(Block block) { this.block = block; }

    public PartialState partialState() { return new PartialState(); }

    public VariantBlockStateBuilder forAllStates(Function<BlockState, ConfiguredModel[]> mapper) {
        return forAllStatesExcept(mapper);
    }

    public VariantBlockStateBuilder forAllStatesExcept(Function<BlockState, ConfiguredModel[]> mapper,
                                                       Property<?>... ignored) {
        Set<Property<?>> omitted = Set.copyOf(Arrays.asList(ignored));
        Set<Map<Property<?>, Comparable<?>>> seen = new LinkedHashSet<>();
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            PartialState partial = partialState();
            state.getValues().forEach(value -> {
                Property<?> property = value.property();
                if (!omitted.contains(property)) partial.values.put(property, value.value());
            });
            if (seen.add(Map.copyOf(partial.values))) partial.setModels(mapper.apply(state));
        }
        return this;
    }

    public JsonObject toJson() {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            long matches = variants.keySet().stream().filter(key -> matches(state, key)).count();
            if (matches != 1) throw new IllegalStateException("Expected one variant for " + state + ", found " + matches);
        }
        JsonObject entries = new JsonObject();
        variants.forEach((key, models) -> entries.add(key.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                .map(entry -> entry.getKey().getName() + "=" + valueName(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",")), models.toJson()));
        JsonObject result = new JsonObject();
        result.add("variants", entries);
        return result;
    }

    private static boolean matches(BlockState state, Map<Property<?>, Comparable<?>> key) {
        Map<Property<?>, Comparable<?>> values = getStateValues(state);
        return key.entrySet().stream().allMatch(entry -> entry.getValue().equals(values.get(entry.getKey())));
    }

    private static Map<Property<?>, Comparable<?>> getStateValues(BlockState state) {
        Map<Property<?>, Comparable<?>> values = new LinkedHashMap<>();
        state.getValues().forEach(value -> values.put(value.property(), value.value()));
        return values;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static String valueName(Property property, Comparable value) { return property.getName(value); }

    public final class PartialState {

        private final Map<Property<?>, Comparable<?>> values = new LinkedHashMap<>();

        public <T extends Comparable<T>> PartialState with(Property<T> property, T value) {
            if (!block.getStateDefinition().getProperties().contains(property) ||
                    !property.getPossibleValues().contains(value)) {
                throw new IllegalArgumentException("Invalid property/value for " + block + ": " + property + "=" + value);
            }
            if (values.putIfAbsent(property, value) != null) throw new IllegalArgumentException("Property set twice: " + property);
            return this;
        }

        public VariantBlockStateBuilder setModel(ModelFile model) {
            return setModels(ConfiguredModel.builder().modelFile(model).build());
        }

        public VariantBlockStateBuilder setModels(ConfiguredModel... models) {
            Map<Property<?>, Comparable<?>> key = Map.copyOf(values);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (matches(state, key) && variants.keySet().stream().anyMatch(existing -> matches(state, existing))) {
                    throw new IllegalArgumentException("Overlapping variants for " + state);
                }
            }
            variants.put(key, new ConfiguredModelList(models));
            return VariantBlockStateBuilder.this;
        }

        public ConfiguredModel.Builder<VariantBlockStateBuilder> modelForState() {
            return new ConfiguredModel.Builder<>(this::setModels);
        }
    }
}
