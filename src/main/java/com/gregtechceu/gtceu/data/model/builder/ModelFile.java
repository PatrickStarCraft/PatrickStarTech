package com.gregtechceu.gtceu.data.model.builder;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/** A model resource reference used by GT's data model builders. */
public abstract class ModelFile {
    private final Identifier location;

    protected ModelFile(Identifier location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    public Identifier getLocation() {
        return location;
    }

    public String getLocationString() {
        return location.toString();
    }

    public abstract void assertExists();

    public static class ExistingModelFile extends ModelFile {
        private final ModelFileHelper helper;

        public ExistingModelFile(Identifier location, ModelFileHelper helper) {
            super(location);
            this.helper = Objects.requireNonNull(helper, "helper");
        }

        public ExistingModelFile(String location, ModelFileHelper helper) {
            this(Identifier.parse(location), helper);
        }

        @Override
        public void assertExists() {
            if (!helper.exists(getLocation(), ModelFileHelper.ResourceType.MODEL)) {
                throw new IllegalStateException("Model " + getLocation() + " does not exist");
            }
        }
    }

    /** A model reference that is deliberately not checked against the input resources. */
    public static class UncheckedModelFile extends ModelFile {
        public UncheckedModelFile(Identifier location) {
            super(location);
        }

        public UncheckedModelFile(String location) {
            this(Identifier.parse(location));
        }

        @Override
        public void assertExists() {}
    }
}
