package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.api.capability.IThermalFluidHandlerItemStack;
import com.gregtechceu.gtceu.common.data.GTDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.TransferPreconditions;

import org.jetbrains.annotations.Nullable;

/**
 * Transactional thermal fluid storage for item containers, with lazy migration from legacy {@code Fluid} custom data.
 */
public final class ThermalFluidResourceHandler extends ItemAccessFluidHandler
        implements IThermalFluidHandlerItemStack {

    private static final String LEGACY_FLUID_KEY = "Fluid";
    private static final String LEGACY_FLUID_NAME_KEY = "FluidName";
    private static final String LEGACY_AMOUNT_KEY = "Amount";
    private static final String LEGACY_TAG_KEY = "Tag";

    private final int maxFluidTemperature;
    private final boolean gasProof;
    private final boolean acidProof;
    private final boolean cryoProof;
    private final boolean plasmaProof;
    private final boolean allowPartialFill;

    public ThermalFluidResourceHandler(ItemAccess access, int capacity, int maxFluidTemperature, boolean gasProof,
                                       boolean acidProof, boolean cryoProof, boolean plasmaProof,
                                       boolean allowPartialFill) {
        super(access, GTDataComponents.FLUID_CONTENT.get(), capacity);
        this.maxFluidTemperature = maxFluidTemperature;
        this.gasProof = gasProof;
        this.acidProof = acidProof;
        this.cryoProof = cryoProof;
        this.plasmaProof = plasmaProof;
        this.allowPartialFill = allowPartialFill;
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (!accessResource.is(validItem)) {
            return FluidResource.EMPTY;
        }
        if (isComponentExplicitlyPresent(accessResource)) {
            return super.getResourceFrom(accessResource, index);
        }
        FluidStack legacyStack = getLegacyFluid(accessResource);
        return legacyStack.isEmpty() ? super.getResourceFrom(accessResource, index) : FluidResource.of(legacyStack);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        if (!accessResource.is(validItem)) {
            return 0;
        }
        if (isComponentExplicitlyPresent(accessResource)) {
            return super.getAmountFrom(accessResource, index);
        }
        FluidStack legacyStack = getLegacyFluid(accessResource);
        return legacyStack.isEmpty() ? super.getAmountFrom(accessResource, index) : legacyStack.getAmount();
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemResource updated = super.update(accessResource, index, newResource, newAmount);
        CustomData customData = accessResource.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(LEGACY_FLUID_KEY)) {
            return updated;
        }

        CompoundTag root = customData.copyTag();
        root.remove(LEGACY_FLUID_KEY);
        return updated.with(DataComponents.CUSTOM_DATA, root.isEmpty() ? null : CustomData.of(root));
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        // The old simple (full-only) handler did not consult canFillFluidType.
        return super.isValid(index, resource) &&
                (!allowPartialFill || resource.isEmpty() || canFillFluidType(resource.toStack(1)));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (allowPartialFill) {
            return super.insert(index, resource, amount, transaction);
        }

        java.util.Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int itemCount = itemAccess.getAmount();
        long required = (long) capacity * itemCount;
        if (itemCount == 0 || required > Integer.MAX_VALUE || amount < required ||
                getAmountFrom(itemAccess.getResource(), index) != 0 || !isValid(index, resource)) {
            return 0;
        }

        try (Transaction subTransaction = Transaction.open(transaction)) {
            int inserted = super.insert(index, resource, (int) required, subTransaction);
            if (inserted == required) {
                subTransaction.commit();
                return inserted;
            }
        }
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (allowPartialFill) {
            return super.extract(index, resource, amount, transaction);
        }

        java.util.Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        int itemCount = itemAccess.getAmount();
        long required = (long) capacity * itemCount;
        if (itemCount == 0 || required > Integer.MAX_VALUE || amount < required ||
                getAmountFrom(itemAccess.getResource(), index) != capacity) {
            return 0;
        }

        try (Transaction subTransaction = Transaction.open(transaction)) {
            int extracted = super.extract(index, resource, (int) required, subTransaction);
            if (extracted == required) {
                subTransaction.commit();
                return extracted;
            }
        }
        return 0;
    }

    @Override
    public int getMaxFluidTemperature() {
        return maxFluidTemperature;
    }

    @Override
    public boolean isGasProof() {
        return gasProof;
    }

    @Override
    public boolean isAcidProof() {
        return acidProof;
    }

    @Override
    public boolean isCryoProof() {
        return cryoProof;
    }

    @Override
    public boolean isPlasmaProof() {
        return plasmaProof;
    }

    private boolean isComponentExplicitlyPresent(ItemResource accessResource) {
        return accessResource.getComponentsPatch().getPatch(component) != null;
    }

    private FluidStack getLegacyFluid(ItemResource accessResource) {
        CustomData customData = accessResource.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return FluidStack.EMPTY;
        }

        CompoundTag root = customData.copyTag();
        CompoundTag legacy = root.getCompound(LEGACY_FLUID_KEY).orElse(null);
        if (legacy == null) {
            return FluidStack.EMPTY;
        }

        Identifier fluidId = Identifier.tryParse(legacy.getStringOr(LEGACY_FLUID_NAME_KEY, ""));
        int amount = legacy.getIntOr(LEGACY_AMOUNT_KEY, 0);
        if (fluidId == null || amount <= 0) {
            return FluidStack.EMPTY;
        }

        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
        if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }

        FluidStack stack = new FluidStack(fluid, amount);
        @Nullable CompoundTag legacyFluidData = legacy.getCompound(LEGACY_TAG_KEY).orElse(null);
        if (legacyFluidData != null && !legacyFluidData.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyFluidData));
        }
        return stack;
    }
}
