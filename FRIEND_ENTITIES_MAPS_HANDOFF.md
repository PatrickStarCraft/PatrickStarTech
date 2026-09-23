# Entities and Maps Port Handoff

## Checkout

- Branch: `1.20.1`
- Base: `b1daee2f206dd776a1a6fc181d2f6bebd271a16d` (latest locally available `1.20.1` integration commit at task start)
- Checkout: `C:\Users\georg\IdeaProjects\PatrickStarTech`
- The implementation was carried over from `codex/friend-entities-maps`; its working tree remains available as a reference copy.
- The previous `codex/friend-particles` worktree and its uncommitted changes remain separate and untouched.

## Tasks

### Task 1 — Boat entities

- Migrated `GTBoat` and `GTChestBoat` to the 26.2 `Boat`/`ChestBoat` constructors that require item suppliers.
- Added independent synchronized integer variant data on both entities. Retained the legacy `"Type"` string for saved rubber/treated variants and use the live type for boat and chest-boat drop items.
- Both save/load overrides now call `super`. `GTChestBoat` therefore retains vanilla chest inventory/loot-table persistence, interaction, removal, and contents-drop behavior. Removed old packet overrides; 26.2 `Entity#getAddEntityPacket(ServerEntity)` now supplies the standard tracking packet. Preserved factory constructors and position construction via `setInitialPos`.
- Exact Minecraft 26.2 sources inspected: `Boat`, `ChestBoat`, `AbstractBoat`, `AbstractChestBoat`, `SynchedEntityData`, `ValueInput`/`ValueOutput`, and `Entity`.
- Remaining adjacent integration request: `client/renderer/entity/GTBoatRenderer.java` is outside this assignment's shared rendering/model ownership. It must migrate from old `BoatRenderer(Context, boolean)`, `getModelWithLocation(Boat)`, and `ListModel<Boat>` APIs to 26.2 `AbstractBoatRenderer`/`BoatRenderState` or separate modern renderers, while selecting rubber/treated models and textures for both boat kinds. The current integrated compiler still reports errors in that renderer; changing the two owned entity classes alone does not restore variant visuals.

### Task 2 — Map integrations

- Migrated `ClientCacheManager` to 26.2 NBT loading with `NbtIo.readCompressed(Path, NbtAccounter.unlimitedHeap())`, retaining the former unbounded read behavior, and `Identifier.bySeparator` to preserve legacy `=`-separated dimension cache filenames.
- Updated `WaypointManager.WaypointKey` to compare dimensions by value (`Objects.equals`) instead of Java object identity. This makes toggling a waypoint work when equivalent `ResourceKey<Level>` values are reconstructed independently; `hashCode` was already value-based.
- FTB Chunks integration currently resolves `dev.ftb.mods:ftb-chunks-forge:2001.3.4`; Xaero dependencies resolve artifact ids explicitly suffixed `-forge-1.20.1` (Xaero Lib 1.1.0, World Map 1.40.11, Minimap 25.3.10); JourneyMap API is `1.20-1.9-SNAPSHOT`, with mod file 5789363. These are the versions currently configured in `gradle/forge.versions.toml`; no versions were changed.
- The remaining map errors are provider UI/render callback breaks, not just renamed Minecraft types. For example, the cached Xaero World Map `MapElementRenderer` contract is still based on `GuiGraphics` and `MultiBufferSource.BufferSource`, while 26.2 moved to the render-state pipeline; FTB map icons now require a different draw callback and its fluid widget receives `GuiGraphicsExtractor`; JourneyMap listeners and texture identifiers also differ. The repo does not configure verified 26.2-compatible artifacts for these optional mods, so adapting against the old binaries would be speculative and changing shared dependency versions is outside this ownership slice.
- Optional integration enablement and waypoint handlers were reviewed. FTB Chunks and JourneyMap waypoint handler source uses `ResourceKey<Level>` and `BlockPos`; provider renderers, screen widgets, and JourneyMap event API are the incompatible pieces requiring current provider artifacts and broader client rendering/API coordination.

### Task 3 — Additional 26.2 API migrations

- Migrated `BreweryLogic` from removed private/global potion-mix access to the level-scoped brewing API. Preserved water-cell handling, representative registered potion recipes, concrete brewing recipes, and duplicate avoidance; removed the obsolete `PotionBrewingAccessor` mixin.
- Migrated `ValueTransformers` packet encoding to current registry, block-state, identifier, item-stack, fluid-stack, and component codecs while retaining their serialized values. Updated `FacadeCoverRecipe` to `CraftingInput`, current recipe metadata and placement APIs, and emitted its stateless special recipe through the runtime data-pack recipe generator.
- Ported foam sand-use handling to `useItemOn`, retained bright-sky petrification behavior, and updated the daytime recipe condition. Ported placeholder identifier/client-side APIs and preserved absent placeholder data as an empty compound.
- Updated conditional tick scheduling to the event loop's `schedule` API; retained the no-op stacked-content helper without an obsolete override annotation. Updated fluid-handler optional access, Robot Arm NBT defaults, biome precipitation with sea level, and fluid-sprout chunk post-processing.
- Updated block-position sync deserialization to read the legacy `X`/`Y`/`Z` compound form, and migrated `MonitorGroupTransformer` legacy fields into the current block-position codec while preserving its saved-data upgrade path.
- Migrated bounded integer and float provider codecs to 26.2 `IntProviders`/`FloatProviders` without changing their configured ranges. Updated pipe notifications and loot-parameter access, the item collector's server-side entity removal, steam boiler recipe enumeration and ingredient holders, and Creative/Quantum Chest stack comparison. Also updated Canner fluid-handler optionals, machine-trait/virtual-entry NBT reads, and the exhaust-shape helper spelling.
- The recipe database warning path now imports the target built-in registries. The facade special-recipe JSON is emitted through `GeneratedRecipe` with the same id and stateless serializer.
- Migrated pattern error variants from `Codec` to dispatchable `MapCodec` forms and switched text serialization to `ComponentSerialization.CODEC`; updated the part-ability tooltip to use the target ItemStack name API. Corrected the Drum Machine nullability annotation import.
- Ported 26.2 `InsideBlockEffectApplier` callbacks for framed material, pipe, cable, fluid-pipe, and explosive blocks, forwarding the original effect collector and precision flag when delegating to the frame state. Updated the forming press custom mold check to use `DataComponents.CUSTOM_NAME`.
- Updated the multiblock first-tick structure check to schedule through the server scheduler API. Preserved `LocalizedHazardSavedData` block positions in their existing `X`/`Y`/`Z` compound form by reading and writing those coordinates directly after the old `NbtUtils` helpers were removed.
- Updated `GTRegistryArgument` to parse command identifiers through the current `Identifier.read(StringReader)` API, matching the other command parsers in the project.
- Updated recipe generation for the two-parameter `ItemProviderEntry` API and used `DOUGH.asStack(count)` to disambiguate the holder-backed recipe output. Migrated `/place_vein` failures to Brigadier's dynamic command exception and chunk persistence marking to `LevelChunk.markUnsaved()`.
- Migrated symptom attribute modifiers to 26.2 holder-based attributes while preserving the public legacy UUID/raw-attribute overloads; used a separately named effect-holder factory to avoid `DeferredHolder`/`Supplier` overload ambiguity.
- Migrated bedrock-fluid, bedrock-ore, and GT ore loaders to `SimpleJsonResourceReloadListener<JsonElement>`, the current JSON codec and file-to-id converter, retaining the same resource folder and per-entry parsing.
- Migrated `GTDynamicDataPack` to current pack-location and metadata-section APIs; retained the pack name/source and data-pack metadata behavior.
- Updated the lightning-rod recipe outputs to the unaffected state from 26.2's weathering collection, preserving plain lightning rods.
- Migrated `FisherMachine` to server reloadable loot registries and the current fishing-bobber entity type. Updated `PumpMachine` to current bucket-pickup wrapper and fluid-state access, retaining pickup checks and drain behavior.
- Migrated `ManagedSyncBlockEntity` to `ValueInput`/`ValueOutput`, preserving its flat persisted NBT, old saved-data read compatibility, and byte-array client update payload.
- Migrated furnace-recipe conversion to 26.2's `SmeltingRecipe` accessors and `GTRecipeBuilder.buildRawRecipe`; this conversion is only registered for vanilla smelting in the current recipe-type table.
- Split data generation across 26.2's separate client/server gather events. Kept the sound provider on the client event, and biome, datapack registry, damage-tag, and loot providers on the server event. Removed the obsolete `ExistingFileHelper` constructor arguments from the owned biome/damage tag providers.
- Migrated the dynamic pack repository source to `PackLocationInfo`, `Pack.ResourcesSupplier`, `Pack.readPackMetadata`, and `PackSelectionConfig`, retaining required status, pack position/source, title, resources, and hidden-pack metadata.
- Updated HPCA coolant tag iteration to use the current fluid-holder value accessor; drain order and amounts are unchanged.
- Updated the HPCA component tooltip extraction to pass 26.2's `Item.TooltipContext.EMPTY` and a null player, preserving the prior context-free tooltip request.
- Updated early configuration checks to use the current FML loader instance for production state and the loading mod list. Its single remaining diagnostic is the data-generation launch-mode check, left unresolved pending a verified 26.2 equivalent.
- Migrated the block, fluid, and entity tag loaders to the configured Registrate `Impl` providers and 26.2 `ResourceKey` tag entries. Updated tall-flower and tinted-glass block tags to their current common-tag names while retaining the corresponding members and recipe behavior.
- Ported `GTPoisonEffect` to the 26.2 server-side effect callback and damage API, preserving its damage cadence, amount, and health floor. Updated `LargeBoilerMachine` to the current two-axis mouse-scroll callback while retaining its vertical scroll behavior.
- Updated `EnvironmentalHazardCleanerTrait` to use the current `ChunkPos.x()`/`z()` accessors when checking and retrieving a loaded chunk before notifying tracking players; this retains the loaded-chunk guard and notification behavior.
- Removed the obsolete `Blender` callback from `VeinedVeinGenerator`'s `DensityFunction.FunctionContext`; Minecraft 26.2's context carries only block coordinates, while Blender is no longer part of that interface. The vein generation calculation and its coordinate offsets remain unchanged.
- `GTRecipeTransformer` still needs a client-side legacy recipe lookup migration: 26.2 clients expose recipe display/property data rather than the former full `RecipeManager`. `AdjacentBlockCondition`/`AdjacentFluidCondition` also still need a tag-holder migration that retains tag binding across data reloads. These were left unchanged rather than replacing old behavior with a snapshot or empty tag.

## Verification

- Baseline: `compileJava -PportDiagnostics --max-workers=1 --console=plain --offline`; `build/friend-entities-maps-baseline.log`; **2,794 errors**, including 18 in each boat entity and 57 under `integration/map`.
- After boat batch: same command; `build/friend-entities-maps-boats.log`; **2,757 errors**, with zero diagnostics in either owned boat entity. Map package still has 57 diagnostics.
- After cache batch: same command; `build/friend-entities-maps-cache.log`; **2,754 errors**. `ClientCacheManager`, `GTBoat`, and `GTChestBoat` have zero diagnostics; map package has 54 diagnostics. Three errors disappeared with the cache API migration.
- Verification on the requested `1.20.1` checkout: `build/friend-entities-maps-1.20.1.log`; **2,678 errors**. `GTBoat`, `GTChestBoat`, `ClientCacheManager`, and `WaypointManager` each have zero diagnostics; all 54 map diagnostics remain in optional provider renderers/widgets/listeners using the pinned 1.20.1 APIs. `GTBoatRenderer` still has 8 errors and remains outside this rendering ownership slice.
- Latest integrated offline compile on `1.20.1`: `build/friend-entities-maps-material-block.log`; **2,571 errors**. Ported members compile cleanly; `PipeBlock` still has unrelated appearance/shape/interaction API diagnostics. The map package still has 54 provider API diagnostics, and `GTBoatRenderer` still has 8 renderer API diagnostics.
- Latest integrated offline compile after the scheduler and hazard saved-data updates: `build/friend-entities-maps-hazard-nbt.log`; **2,564 errors**. `MultiblockControllerMachine` and `LocalizedHazardSavedData` no longer have compiler diagnostics. The two adjacent recipe conditions still have one removed `getOrCreateTag` API error apiece; they remain unresolved pending a reload-safe named-tag holder migration.
- Latest integrated offline compile after the registry command parser update: `build/friend-entities-maps-command-parser.log`; **2,561 errors**. The updated command parser also compiles cleanly.
- Latest integrated offline compile after recipe generation and `/place_vein` updates: `build/friend-entities-maps-place-vein.log`; **2,555 errors**. `GTCommands`, `MetaTileEntityLoader`, and `MiscRecipeLoader` compile cleanly.
- Latest integrated offline compile after tag-provider migrations: `build/friend-entities-maps-tag-appender.log`; **2,544 errors**. `BlockTagLoader`, `FluidTagLoader`, and `EntityTypeTagLoader` now compile cleanly. One `VanillaStandardRecipes` diagnostic remains for the removed stained-glass-pane item tag; the new common pane tag also includes clear panes, so it was not substituted and recipe behavior broadened.
- Latest integrated offline compile after the poison-effect and boiler-scroll migrations: `build/friend-entities-maps-boiler-scroll.log`; **2,541 errors**. `GTPoisonEffect` and `LargeBoilerMachine` compile cleanly. The pane-tag diagnostic remains unresolved because the current common tag includes clear panes.
- Latest integrated offline compile after the `ChunkPos` accessor migration: `build/friend-entities-maps-chunkpos.log`; **2,537 errors**. `EnvironmentalHazardCleanerTrait` compiles cleanly; `EnvironmentalHazardSavedData` already uses the current accessors. No other edits were made in the related saved-data path.
- Latest integrated offline compile after removing the obsolete density-function context callback: `build/friend-entities-maps-density-context.log`; **2,536 errors**. `VeinedVeinGenerator` no longer has diagnostics. No tests or gameplay checks were run.
- Verified the currently preserved working tree against that 2,536-error compile; `build/friend-entities-maps-current.log` reports **2,485 errors** after the medical-condition, reload-listener, pack recipe, and machine/sync batches described above.
- Latest integrated offline compile after the recipe and data-generation API migrations: `build/friend-entities-maps-datagen-recipe2.log`; **2,472 errors**. `GTRecipeType`, `RecipeManagerHandler`, `DataGenerators`, `BiomeTagsLoader`, and `DamageTagsLoader` have no diagnostics.
- Latest integrated offline compile after the pack repository-source migration: `build/friend-entities-maps-packsource.log`; **2,469 errors**. `GTPackSource` has no diagnostics.
- Latest integrated offline compile after the HPCA holder and early-loader access updates: `build/friend-entities-maps-loader-access.log`; **2,465 errors**. `HPCAMachine` has no diagnostics; `GTEarlyConfig` has one remaining diagnostic for the unverified data-generation launch-mode check.
- Latest integrated offline compile after the HPCA tooltip-context migration: `build/friend-entities-maps-hpca-tooltip2.log`; **2,464 errors**. `HPCAMachine` has no diagnostics.
- Remaining inspected core diagnostics include machine owner permission levels (the old numeric thresholds do not map directly to current permission sets without changing config semantics), named recipe-tag holder rebinding across reloads, and machine fluid-pickup wrappers. These require explicit compatibility designs before editing. The largest remaining error clusters are rendering/model, item/capability, and optional integration APIs and remain within the shared ownership exclusions below.
- The final offline integrated compile was run with access to the configured cached JDK and completed compilation, failing only because of existing port errors elsewhere. Tests/gameplay were not run.
- `git diff --check` passes in the `1.20.1` checkout.
- Tests/runtime checks are pending; do not claim gameplay behavior without running the game.

## Shared Ownership

- Do not edit tool/item/capability implementations, `GTBoatItem`, proxy/shared listeners, shared map dependencies or any particle/rendering files. Record precise requests here if required.
- To finish map rendering integrations, coordinate updated, locally resolvable 26.2 builds of Xaero Minimap/World Map, FTB Chunks, and JourneyMap API/mod; then port the provider renderer and widget contracts against those verified artifacts. JourneyMap event listener API appears to be routed through its API/plugin lifecycle rather than NeoForge's generic event bus and needs confirmation from the matching 26.2 API sources.
- Project `AI_POLICY.md` requires disclosure of AI-assisted code and human review before contribution.
