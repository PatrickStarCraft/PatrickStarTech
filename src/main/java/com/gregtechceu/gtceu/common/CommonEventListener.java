package com.gregtechceu.gtceu.common;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.BlockAttributes;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.cosmetics.CapeRegistry;
import com.gregtechceu.gtceu.api.cosmetics.event.RegisterGTCapesEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.data.medicalcondition.Symptom;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.data.ArmorMovementItemData;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.common.capability.LocalizedHazardSavedData;
import com.gregtechceu.gtceu.common.capability.MedicalConditionTracker;
import com.gregtechceu.gtceu.common.capability.WorldIDSaveData;
import com.gregtechceu.gtceu.common.commands.GTCommands;
import com.gregtechceu.gtceu.common.commands.HazardCommands;
import com.gregtechceu.gtceu.common.commands.MedicalConditionCommands;
import com.gregtechceu.gtceu.common.cosmetics.GTCapes;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.item.armor.IJetpack;
import com.gregtechceu.gtceu.common.item.armor.QuarkTechSuite;
import com.gregtechceu.gtceu.common.item.behavior.ToggleEnergyConsumerBehavior;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SPacketSendWorldID;
import com.gregtechceu.gtceu.common.network.packets.SPacketSyncBedrockOreVeins;
import com.gregtechceu.gtceu.common.network.packets.SPacketSyncFluidVeins;
import com.gregtechceu.gtceu.common.network.packets.SPacketSyncOreVeins;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketAddHazardZone;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketRemoveHazardZone;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketSyncLevelHazards;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.loader.BedrockFluidLoader;
import com.gregtechceu.gtceu.data.loader.BedrockOreLoader;
import com.gregtechceu.gtceu.data.loader.GTOreLoader;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;
import com.gregtechceu.gtceu.integration.map.WaypointManager;
import com.gregtechceu.gtceu.integration.map.cache.server.ServerCache;
import com.gregtechceu.gtceu.utils.TaskHandler;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import com.mojang.datafixers.util.Either;

import java.util.function.UnaryOperator;
import java.util.List;

@net.neoforged.fml.common.EventBusSubscriber(modid = GTCEu.MOD_ID)
public class CommonEventListener {

    private static final Identifier STEP_ASSIST_MODIFIER = GTCEu.id("step_assist");

    @SubscribeEvent
    public static void registerCapes(RegisterGTCapesEvent event) {
        GTCapes.registerGTCapes(event);
        GTCapes.giveDevCapes(event);
    }

    @SubscribeEvent
    public static void tickPlayerHazards(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // update the tracker every second, including clearing everything when the config changes.
        if (player.tickCount % 20 != 0) {
            return;
        }

        MedicalConditionTracker tracker = GTCapabilityHelper.getMedicalConditionTracker(player);
        if (tracker == null) {
            return;
        }
        if (!ConfigHolder.INSTANCE.gameplay.hazardsEnabled) {
            for (MedicalCondition medicalCondition : tracker.getMedicalConditions().keySet()) {
                tracker.removeMedicalCondition(medicalCondition);
            }
            return;
        }

        var inventory = player.getCapability(Capabilities.Item.ENTITY);
        if (inventory == null) {
            return;
        }

        tracker.tick();

        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack stack = inventory.getResource(i).toStack(inventory.getAmountAsInt(i));
            Either<Material, MaterialEntry> hazardMaterial = HazardProperty.getValidHazardMaterial(stack);
            if (hazardMaterial == null) {
                continue;
            }

            var material = hazardMaterial.map(UnaryOperator.identity(), MaterialEntry::material);

            HazardProperty property = material.getProperty(PropertyKey.HAZARD);
            if (property.hazardTrigger.protectionType().isProtected(player)) {
                // entity has proper safety equipment, so damage it per material every 5 seconds.
                property.hazardTrigger.protectionType().damageEquipment(player, 1);
                // don't progress this material condition if entity is protected
                continue;
            }
            hazardMaterial.ifLeft(m -> tracker.progressRelatedCondition(m, stack.getCount()));
            hazardMaterial.ifRight(m -> tracker.progressRelatedCondition(m, stack.getCount()));
        }
    }

    @SubscribeEvent
    public static void onMobEffectEvent(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = player.getItemBySlot(EquipmentSlot.HEAD);
            if (item.is(GTItems.QUANTUM_HELMET.asItem()) && GTCapabilityHelper.getElectricItem(item) != null) {
                IElectricItem helmet = GTCapabilityHelper.getElectricItem(item);
                MobEffectInstance effect = event.getEffectInstance();
                int cost = QuarkTechSuite.potionRemovalCost.getOrDefault(effect.getEffect(), -1);
                if (cost != -1) {
                    cost = cost * (effect.getAmplifier() + 1);
                    if (helmet.canUse(cost)) {
                        helmet.discharge(cost, helmet.getTier(), true, false, false);
                        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        ItemStack usedItem = event.getItem();
        if (usedItem.get(DataComponents.FOOD) == null) {
            return;
        }
        MedicalConditionTracker tracker = GTCapabilityHelper.getMedicalConditionTracker(player);
        if (tracker == null) {
            return;
        }

        var hazardMaterial = HazardProperty.getValidHazardMaterial(usedItem);
        if (hazardMaterial == null) {
            return;
        }

        var material = hazardMaterial.map(UnaryOperator.identity(), MaterialEntry::material);

        HazardProperty property = material.getProperty(PropertyKey.HAZARD);
        if (property.hazardTrigger == HazardProperty.HazardTrigger.CONSUMPTION) {
            hazardMaterial.ifLeft(m -> tracker.progressRelatedCondition(m, 1));
            hazardMaterial.ifRight(m -> tracker.progressRelatedCondition(m, 1));
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        var machine = MetaMachine.getMachine(event.getLevel(), event.getPos());
        if (machine != null)
            event.setCanceled(machine.onLeftClick(event.getEntity(), event.getHand(), event.getFace()));
    }

    @SubscribeEvent
    public static void onBreakEvent(BreakBlockEvent event) {
        var machine = MetaMachine.getMachine(event.getLevel(), event.getPos());
        if (machine != null) {
            if (!MachineOwner.canBreakOwnerMachine(event.getPlayer(), machine)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        GTCommands.register(event.getDispatcher(), event.getBuildContext());
        MedicalConditionCommands.register(event.getDispatcher(), event.getBuildContext());
        HazardCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddServerReloadListenersEvent event) {
        GTRegistries.updateFrozenRegistry(event.getRegistryAccess());

        event.addListener(GTCEu.id("ore_loader"), new GTOreLoader());
        event.addListener(GTCEu.id("bedrock_fluid_loader"), new BedrockFluidLoader());
        event.addListener(GTCEu.id("bedrock_ore_loader"), new BedrockOreLoader());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TaskHandler.onTickUpdate(serverLevel);
            if (ConfigHolder.INSTANCE.gameplay.environmentalHazards) {
                EnvironmentalHazardSavedData.getOrCreate(serverLevel).tick();
                LocalizedHazardSavedData.getOrCreate(serverLevel).tick();
            }
        }
    }

    @SubscribeEvent
    public static void worldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            WaypointManager.updateDimension(event.getLevel());
        } else if (event.getLevel() instanceof ServerLevel serverLevel) {
            ServerCache.instance.maybeInitWorld(serverLevel);
        }
    }

    @SubscribeEvent
    public static void worldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TaskHandler.onWorldUnLoad(serverLevel);
            // MultiblockWorldSavedData.getOrCreate(serverLevel).releaseExecutorService();
            ServerCache.instance.invalidateWorld(serverLevel);
        } else if (event.getLevel().isClientSide()) {
            ClientCacheManager.saveCaches();
        }
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        ServerLevel mainLevel = event.getServer().overworld();
        WorldIDSaveData.init(mainLevel);
        CapeRegistry.registerToServer(mainLevel);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ServerCache.instance.clear();
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        /*
         * var levels = event.getServer().getAllLevels();
         * for (var level : levels) {
         * if (!level.isClientSide()) {
         * MultiblockWorldSavedData.getOrCreate(level).releaseExecutorService();
         * }
         * }
         */
    }

    @SubscribeEvent
    public static void onPlayerJoinServer(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            GTNetwork.sendToPlayer(serverPlayer, new SPacketSendWorldID());

            if (ConfigHolder.INSTANCE.gameplay.environmentalHazards) {
                if (serverPlayer.level() instanceof ServerLevel level) {
                    var data = EnvironmentalHazardSavedData.getOrCreate(level);
                    GTNetwork.sendToPlayer(serverPlayer, new SPacketSyncLevelHazards(data.getHazardZones()));
                }
            }
            CapeRegistry.detectNewCapes(serverPlayer);
            CapeRegistry.loadCurrentCapesOnLogin(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            // if player == null, the /reload command was ran. sync to all players.
            GTNetwork.sendToAll(new SPacketSyncOreVeins(GTRegistries.ORE_VEINS.registry()));
            GTNetwork.sendToAll(new SPacketSyncFluidVeins(GTRegistries.BEDROCK_FLUID_DEFINITIONS.registry()));
            GTNetwork.sendToAll(new SPacketSyncBedrockOreVeins(GTRegistries.BEDROCK_ORE_DEFINITIONS.registry()));
        } else {
            // else it's a player logging in. sync to only that player.
            GTNetwork.sendToPlayer(player, new SPacketSyncOreVeins(GTRegistries.ORE_VEINS.registry()));
            GTNetwork.sendToPlayer(player,
                    new SPacketSyncFluidVeins(GTRegistries.BEDROCK_FLUID_DEFINITIONS.registry()));
            GTNetwork.sendToPlayer(player,
                    new SPacketSyncBedrockOreVeins(GTRegistries.BEDROCK_ORE_DEFINITIONS.registry()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityLivingFallEvent(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.fallDistance < 3.2f)
                return;

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

            if (boots.is(CustomTags.STEP_BOOTS) && boots.getItem() instanceof ArmorComponentItem armor) {
                armor.getArmorLogic().damageArmor(player, boots, player.damageSources().fall(),
                        (int) (player.fallDistance - 1.2f), EquipmentSlot.FEET);
                player.fallDistance = 0;
                event.setCanceled(true);
            } else if (chest.getItem() instanceof ArmorComponentItem armor &&
                    armor.getArmorLogic() instanceof IJetpack jetpack &&
                    jetpack.canUseEnergy(chest, jetpack.getEnergyPerUse()) &&
                    player.fallDistance >= player.getHealth() + 3.2f) {
                        IJetpack.performEHover(chest, player);
                        player.fallDistance = 0;
                        event.setCanceled(true);
                    }
        }
    }

    @SubscribeEvent
    public static void playerTickEvent(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (armorStack.getItem() instanceof ArmorComponentItem armor) {
                armor.onArmorTick(armorStack, player.level(), player);
            }
        }
        if (!player.level().isClientSide()) {
            var speedAttrib = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttrib == null) return;
            var speedMod = speedAttrib.getModifier(BlockAttributes.BLOCK_SPEED_BOOST);

            float speedBoost = 0.0f;
            if (!player.onGround() || player.isInWater() || player.isCrouching()) {
                speedBoost = 0.0f;
            } else {
                var state = player.level().getBlockState(player.getOnPos());
                if (state.is(CustomTags.VERY_FAST_WALKABLE_BLOCKS)) {
                    speedBoost = 0.6f; // value that is added to the base MC speed
                } else if (state.is(CustomTags.FAST_WALKABLE_BLOCKS)) {
                    speedBoost = 0.25f; // slower to walk on studs
                } else if (state.is(CustomTags.SLOW_WALKABLE_BLOCKS)) {
                    speedBoost = -0.20f; // slower on frames
                }
            }
            if (speedMod != null) {
                if (speedBoost == speedMod.amount()) {
                    return;
                } else {
                    speedAttrib.removeModifier(BlockAttributes.BLOCK_SPEED_BOOST);
                }
            } else {
                if (speedBoost == 0.0f) return;
            }
            if (speedBoost != 0.0f) {
                speedAttrib.addTransientModifier(
                        new AttributeModifier(BlockAttributes.BLOCK_SPEED_BOOST, speedBoost,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    @SubscribeEvent
    public static void stepAssistHandler(EntityTickEvent.Pre event) {
        float magicStepHeight = 1.0023f;
        if (event.getEntity() == null || !(event.getEntity() instanceof Player player)) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        boolean stepAssistEnabled = !player.isCrouching() && boots.is(CustomTags.STEP_BOOTS) &&
                ArmorMovementItemData.shouldApplyStepAssist(boots);
        updateStepAssistModifier(player, stepAssistEnabled, magicStepHeight);
    }

    private static void updateStepAssistModifier(Player player, boolean enabled, float targetHeight) {
        var stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) return;

        AttributeModifier current = stepHeight.getModifier(STEP_ASSIST_MODIFIER);
        if (!enabled) {
            if (current != null) stepHeight.removeModifier(STEP_ASSIST_MODIFIER);
            return;
        }

        double heightWithoutStepAssist = stepHeight.getValue() - (current == null ? 0.0 : current.amount());
        double neededIncrease = targetHeight - heightWithoutStepAssist;
        if (neededIncrease <= 0.0) {
            if (current != null) stepHeight.removeModifier(STEP_ASSIST_MODIFIER);
            return;
        }

        if (current == null || current.amount() != neededIncrease) {
            if (current != null) stepHeight.removeModifier(STEP_ASSIST_MODIFIER);
            stepHeight.addTransientModifier(new AttributeModifier(STEP_ASSIST_MODIFIER, neededIncrease,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @SubscribeEvent
    public static void onEntityDie(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            MedicalConditionTracker tracker = GTCapabilityHelper.getMedicalConditionTracker(player);
            if (tracker == null) {
                return;
            }
            for (MedicalCondition condition : tracker.getMedicalConditions().keySet()) {
                tracker.removeMedicalCondition(condition);
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySpawn(FinalizeSpawnEvent event) {
        Mob entity = event.getEntity();
        Difficulty difficulty = entity.level().getDifficulty();
        if (difficulty == Difficulty.HARD && entity.getRandom().nextFloat() <= 0.03f) {
            if (entity instanceof Zombie zombie && ConfigHolder.INSTANCE.tools.nanoSaber.zombieSpawnWithSabers) {
                ItemStack itemStack = GTItems.NANO_SABER.get().getInfiniteChargedStack();
                ToggleEnergyConsumerBehavior.setItemActive(itemStack, true);
                entity.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
                zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLevelChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ConfigHolder.INSTANCE.gameplay.environmentalHazards) {
            return;
        }

        if (event.getEntity().level() instanceof ServerLevel newLevel) {
            var data = EnvironmentalHazardSavedData.getOrCreate(newLevel);
            GTNetwork.sendToPlayer((ServerPlayer) event.getEntity(), new SPacketSyncLevelHazards(data.getHazardZones()));
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ChunkPos pos = event.getPos();
        ServerPlayer player = event.getPlayer();
        var data = EnvironmentalHazardSavedData.getOrCreate(event.getLevel());

        var zone = data.getZoneByPos(pos);
        if (zone != null) {
            GTNetwork.sendToPlayer(player, new SPacketAddHazardZone(pos, zone));
        }
    }

    @SubscribeEvent
    public static void onChunkUnWatch(ChunkWatchEvent.UnWatch event) {
        ChunkPos pos = event.getPos();
        ServerPlayer player = event.getPlayer();
        var data = EnvironmentalHazardSavedData.getOrCreate(event.getLevel());

        var zone = data.getZoneByPos(pos);
        if (zone != null) {
            GTNetwork.sendToPlayer(player, new SPacketRemoveHazardZone(pos));
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!event.getSlot().isArmor()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (!event.getFrom().isEmpty() && event.getFrom().getItem() instanceof ArmorComponentItem armor) {
            armor.getArmorLogic().onUnequip(player);
        }
        if (!event.getTo().isEmpty() && event.getTo().getItem() instanceof ArmorComponentItem armor) {
            armor.getArmorLogic().onEquip(player);
        }
    }

    @SubscribeEvent
    public static void modifyBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        for (EquipmentSlot slot : List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
                EquipmentSlot.HEAD)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorComponentItem componentItem)) {
                continue;
            }
            if (!(componentItem.getArmorLogic() instanceof IJetpack jetpack) || !jetpack.removeMiningSpeedPenalty()) {
                continue;
            }
            // undo flight mining speed debuff
            if (!player.onGround()) {
                event.setNewSpeed(event.getNewSpeed() * 5);
            }
            // and also underwater debuff
            if (player.isEyeInFluid(NeoForgeMod.WATER_TYPE.value()) &&
                    EnchantmentHelper.getEnchantmentLevel(
                            player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.AQUA_AFFINITY), player) == 0) {
                event.setNewSpeed(event.getNewSpeed() * 5);
            }
        }

        MedicalConditionTracker tracker = GTCapabilityHelper.getMedicalConditionTracker(player);
        if (tracker == null || !ConfigHolder.INSTANCE.gameplay.hazardsEnabled) {
            return;
        }
        var attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        var miningFatigue = attackSpeed == null ? null : attackSpeed.getModifier(Symptom.SYMPTOM_MINING_FATIGUE_ID);
        if (miningFatigue != null) {
            float miningFatigueModifier = (float) miningFatigue.amount();
            // mimic how AttributeInstance handles MULTIPLY_BASE modifiers
            event.setNewSpeed(event.getNewSpeed() + event.getNewSpeed() * miningFatigueModifier);
        }
    }

}
