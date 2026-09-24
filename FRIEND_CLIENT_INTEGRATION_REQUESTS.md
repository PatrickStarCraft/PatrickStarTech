# Client Port Integration Requests

## Open requests

### 1. Register particle-count debug entry

- **File:** `src/main/java/com/gregtechceu/gtceu/client/ClientProxy.java` (or the existing client mod-bus listener it owns).
- **Current contract:** `GTParticleManager` exposes `registerDebugEntries(RegisterDebugEntriesEvent)` for 26.2; the former `CustomizeGuiOverlayEvent.DebugText` callback no longer exists.
- **Required change:** Subscribe to NeoForge `RegisterDebugEntriesEvent` on the client mod event bus and call `GTParticleManager.INSTANCE.registerDebugEntries(event)`. Preserve the back/front particle-count lines as implemented by the owned manager.
- **Evidence:** `FRIEND_PARTICLES_HANDOFF.md` records migration of the manager to `RegisterDebugEntriesEvent`; 26.2 NeoForge replaces the old debug overlay event. Read and verify the current `GTParticleManager` method before wiring.
- **Blocks:** debug behavior only; not client compilation/startup.
- **Owned dependency:** `client/particle/GTParticleManager.java`.

### 2. Register current boat model layer definitions

- **File:** `src/main/java/com/gregtechceu/gtceu/client/ClientProxy.java`, client model-layer registration method.
- **Current contract:** Registers `BoatModel::createBodyModel` for boat variants and uses the removed `net.minecraft.client.model.ChestBoatModel::createBodyModel` for chest variants.
- **Required change:** Keep registering `GTBoatRenderer.getBoatModelName(type)` with `BoatModel::createBoatModel`; register `GTBoatRenderer.getChestBoatModelName(type)` with `BoatModel::createChestBoatModel`. Continue registering the four rubber/treated × regular/chest layer IDs.
- **Evidence:** Patched Minecraft 26.2.0.88 source `net/minecraft/client/model/object/boat/BoatModel.java` has `createBoatModel()` and `createChestBoatModel()`; no `ChestBoatModel` class exists. The renderer now bakes these named layers and stores each corresponding model/texture in its extracted render state.
- **Blocks:** full client compilation and startup for the boat renderer; the renderer is otherwise migrated.
- **Owned dependency:** `client/renderer/entity/GTBoatRenderer.java`.

### 3. Remove obsolete block-model mixin registration

- **File:** `src/main/resources/gtceu.mixins.json`, entry `client.BlockModelMixin`.
- **Current contract:** The entry refers to the owned `BlockModelMixin`, which injected at `BlockModel.bakeFace` and read the old `BlockElementFace.texture` field.
- **Required change:** Remove the `client.BlockModelMixin` entry. The owned mixin class has been removed because target 26.2 `BlockModel` is an update interface and no longer contains `bakeFace`; vanilla cuboid model faces now pass through `UnbakedCuboidGeometry.bake` to `FaceBakery.bakeQuad`, where the surviving `FaceBakeryMixin` records `CuboidFace.texture()` on the baked quad.
- **Evidence:** Patched Minecraft 26.2.0.88 source `client/renderer/block/model/BlockModel.java` declares only `update(...)` and baking context; `client/resources/model/cuboid/UnbakedCuboidGeometry.java` resolves each `CuboidFace` material and calls `FaceBakery.bakeQuad(modelBaker, from, to, face, ...)`. `FaceBakeryMixin` targets that exact overload.
- **Blocks:** client startup/mixin application while the stale entry remains; Java compilation is unaffected.
- **Owned dependency:** `core/mixins/client/FaceBakeryMixin.java` preserves texture-slot metadata for target cuboid baking.

### 4. Remove obsolete render-state-shard accessor registration

- **File:** `src/main/resources/gtceu.mixins.json`, entry `client.RenderStateShardAccessor`.
- **Current contract:** The entry refers to an accessor for `RenderStateShard.name`; the owned accessor had no in-repository callers.
- **Required change:** Remove the `client.RenderStateShardAccessor` entry. Its owned Java class has been removed; current render descriptions use `RenderPipeline` and `RenderSetup` and do not inspect legacy render-state names.
- **Evidence:** Patched Minecraft 26.2.0.88 source has no `net.minecraft.client.renderer.RenderStateShard`; repository-wide search found no callers beyond the accessor declaration. The migrated `GTRenderTypes` registers named target pipelines directly.
- **Blocks:** client startup/mixin application while the stale entry remains; Java compilation is unaffected.
- **Owned dependency:** the completed `client/renderer/GTRenderTypes.java` migration no longer needs this accessor.

### 5. Remove obsolete vanilla-variant deserializer mixin registration

- **File:** `src/main/resources/gtceu.mixins.json`, entry `client.VariantDeserializerMixin`.
- **Current contract:** The entry targets the removed `net.minecraft.client.renderer.block.model.Variant.Deserializer` to add a `gtceu:z` field to ordinary blockstate variants.
- **Required change:** Remove the `client.VariantDeserializerMixin` entry. The owned mixin source has been removed. Ordinary generated blockstate variants now encode `x`, `y`, and `z` through Minecraft 26.2's native `Variant.SimpleModelState` codec; the separate `gtceu:z` field remains handled by `VariantState.Deserializer` for custom machine-model payloads.
- **Evidence:** Patched Minecraft 26.2.0.88 source `client/renderer/block/dispatch/Variant.java` defines the native `x`, `y`, and `z` quadrant fields and `MAP_CODEC`; `BlockStateModel.Unbaked.ELEMENT_CODEC` uses `Variant.MAP_CODEC`. Current `GTBlockstateProvider` documents this split, `ConfiguredModel` emits native `z`, and `MachineModelBuilder` emits `gtceu:z` only for its custom payload. Repository search found no resource JSON using the legacy mixin property.
- **Blocks:** client startup/mixin application while the stale target entry remains; Java compilation is unaffected.
- **Owned dependency:** `client/util/VariantRotationHelpers.java` now uses target `Quadrant`/`BlockModelRotation` orientation composition for custom machine-model rotations.

### 6. Migrate the ModernFix dynamic-model callback

- **File:** `src/main/java/com/gregtechceu/gtceu/integration/modernfix/GTModernFixIntegration.java`, `onBakedModelLoad`.
- **Current contract:** The integration implements the old per-model callback with `BakedModel`, `UnbakedModel`, `ModelState`, and `Material`, then dispatches `AssetEventListener.BakedModelReplacement`. Those 1.20 model types and the listener interface no longer exist in the target path.
- **Required change:** Use the exact ModernFix version's supported dynamic-resource hook for 26.2 `BlockStateModel` results and invoke the corresponding client replacement listeners, or remove the obsolete integration callback and let the supported NeoForge `ModelEvent.ModifyBakingResult` path run. Do not disable ModernFix dynamic resources or connected textures as a workaround. Keep the opt-out in `ModelEventHelper` only while an equivalent dynamic-resource path is provided.
- **Evidence:** `build/friend-client-port-model-events-verify.log` shows six errors in this unowned file. NeoForge 26.2.0.88 `ModelEvent.ModifyBakingResult` exposes a `ModelBakery.BakingResult` whose block model registry is `Map<BlockState, BlockStateModel>`; `GTModernFixIntegration` still uses the removed 1.20 callback signature and removed `AssetEventListener.BakedModelReplacement` type.
- **Blocks:** full compilation; connected-texture replacement when ModernFix dynamic resources are enabled.
- **Owned dependency:** `client/util/ModelEventHelper.java` now wraps target `BlockStateModel`s in the normal NeoForge model-bake event and exposes `AssetEventListener.BlockStateModelReplacement`.

### 7. Replace the legacy block-highlight event call

- **File:** `src/main/java/com/gregtechceu/gtceu/client/ClientEventListener.java`, `onBlockHighlightEvent`.
- **Current contract:** The listener subscribes to the removed `RenderHighlightEvent.Block`, passes its `MultiBufferSource` to `BlockHighlightRenderer.renderBlockHighlight`, and depends on the old stage callback contract.
- **Required change:** Remove this obsolete `RenderHighlightEvent.Block` subscription and call. The owned `BlockHighlightRenderer` now subscribes to `ExtractBlockOutlineRenderStateEvent`, snapshots the selected tool/cover/pipe geometry and texture IDs, and adds a target `CustomBlockOutlineRenderer`; keep the vanilla block outline additive by not canceling it. Do not re-add the old renderer call, which would duplicate the custom overlays.
- **Evidence:** Exact NeoForge 26.2.0.88 `ExtractBlockOutlineRenderStateEvent` provides target level/position/state/hit data and accepts custom outline renderers. `CustomBlockOutlineRenderer` requires extracted data only and receives `SubmitNodeCollector` at submission. Patched MC `LevelExtractor` stores these callbacks in `BlockOutlineRenderState`; `LevelRenderer.submitBlockOutline` invokes them before vanilla outline submission. The owned renderer captures only immutable vertices, UV/color values, and texture identifiers. The 26.2 sources no longer contain `RenderHighlightEvent.Block` or `MultiBufferSource`.
- **Blocks:** full compilation while the stale event/type and method call remain; the old listener currently fails independently on removed event APIs.
- **Owned dependency:** `client/renderer/BlockHighlightRenderer.java`.

### 8. Replace the legacy AABB-highlight stage call

- **File:** `src/main/java/com/gregtechceu/gtceu/client/ClientEventListener.java`, `onRenderLevelStageEvent`.
- **Current contract:** The shared stage listener calls `AABBHighlightRenderer.INSTANCE.tick(poseStack, bufferSource, camera)` after block entities. The owned renderer no longer has this immediate-render method because 26.2 no longer exposes the old stage/camera/buffer contract.
- **Required change:** Remove the AABB renderer import and the `AFTER_BLOCK_ENTITIES` call. `AABBHighlightRenderer` now extracts its timed highlight list through `ExtractLevelRenderStateEvent` and submits the same thick, blinking, camera-relative outline geometry through `SubmitCustomGeometryEvent`; it owns those subscriptions directly. Keep the `PatternPreviewRenderer` call until its independent migration is complete.
- **Evidence:** Patched Minecraft 26.2.0.88 `LevelRenderState` provides frame-local render data through `ContextKey`; NeoForge 26.2.0.88 `ExtractLevelRenderStateEvent`/`SubmitCustomGeometryEvent` expose extraction and deferred geometry submission. `GTRenderTypes.blockHighlightQuads()` keeps the prior no-depth, position-color overlay pipeline, and the new `PoseStack.Pose` overload in `RenderBufferHelper` emits the same 12 thick AABB edges.
- **Blocks:** full compilation while the stale `tick` call remains; the shared listener also has numerous unrelated 1.20 stage-event diagnostics.
- **Owned dependency:** `client/renderer/AABBHighlightRenderer.java` and `client/util/RenderBufferHelper.java`.

### 9. Remove the legacy pattern-preview stage call

- **File:** `src/main/java/com/gregtechceu/gtceu/client/ClientEventListener.java`, `onRenderLevelStageEvent`.
- **Current contract:** The shared stage listener invokes `PatternPreviewRenderer.INSTANCE.draw(...)` with the removed `RenderLevelStageEvent.Stage`, camera, and `MultiBufferSource.BufferSource` API. The owned renderer no longer exposes this method and now subscribes to target extraction/submission events itself.
- **Required change:** Remove the obsolete `onRenderLevelStageEvent` method once request 8's AABB call is removed; this method contains only the two migrated preview/highlight dispatches. Keep the separate `PatternPreviewRenderer.clientTick()` lifecycle call and all unrelated listener methods.
- **Evidence:** NeoForge 26.2.0.88 `ExtractLevelRenderStateEvent` runs after vanilla extraction and supplies the active `ClientLevel`, frustum, delta tracker, and `LevelRenderState`; `SubmitCustomGeometryEvent` supplies that state and the collector. Patched MC `BlockModelRenderState` and NeoForge `submitMultiLayerBlockModel` carry contextual model parts, tint layers, render flags, and translucent ordering; `BlockEntityRenderDispatcher` extracts per-frame immutable render states. The owned preview now uses these APIs and has zero diagnostics in the integrated compile.
- **Blocks:** full compilation while the stale `draw` call and removed stage/buffer types remain in the shared listener.
- **Owned dependency:** `client/renderer/PatternPreviewRenderer.java`.

### 10. Emit target dynamic pipe block-state models

- **Files:** `src/main/java/com/gregtechceu/gtceu/data/model/builder/PipeModelBuilder.java` and the shared pipe blockstate/model data-generation path (currently invoked from `client/model/pipe/PipeModel.java`).
- **Current contract:** The generated model file uses the old `{"loader":"gtceu:pipe", ...}` custom geometry loader. `PipeModelLoader` returns `UnbakedPipeModel`, and `BakedPipeModel` selects center, directional connection, and restrictor models using live block state/model data.
- **Required change:** Serialize and register a target 26.2 `CustomUnbakedBlockStateModel` (or equivalent target blockstate definition) through `RegisterBlockStateModels`; its baked `BlockStateModel` must select pipe parts from `collectParts(level, pos, state, random, output)`. Preserve center/face/restrictor choices, rotations, weighted variants, model dependencies, and item/non-world behavior. Keep model data reload-safe.
- **Evidence:** Exact NeoForge 26.2.0.88 sources define `RegisterBlockStateModels`, `CustomUnbakedBlockStateModel`, and `DynamicBlockStateModel`; patched MC `BlockStateModel` collects target `BlockStateModelPart`s. Target `UnbakedGeometry.bake(...)` produces a static `QuadCollection` and cannot by itself retain the current neighbor-sensitive pipe model selection.
- **Blocks:** full compile for the owned `PipeModelLoader`/`UnbakedPipeModel`/`BakedPipeModel` files and correct connected pipe visuals/startup.
- **Owned dependency:** `client/model/pipe/PipeModel.java`, `PipeModelLoader.java`, `UnbakedPipeModel.java`, and `BakedPipeModel.java`.

### 11. Replace the legacy lamp item renderer hookup

- **Files:** `src/main/java/com/gregtechceu/gtceu/common/item/LampBlockItem.java#initializeClient` and `src/main/java/com/gregtechceu/gtceu/common/data/GTBlocks.java` lamp/borderless-lamp item model registrations.
- **Current contract:** `LampBlockItem.initializeClient` returns the removed `BlockEntityWithoutLevelRenderer` through `IClientItemExtensions.getCustomRenderer`. `GTBlocks` generates lamp item models with the legacy `CustomItemRendererWrapperModel` loader. `LampItemRenderer` reads `ItemStackData`, selects among eight lamp block states, renders each block-model pass, and applies the item foil buffer.
- **Required change:** Replace the old hook and generated item model with the target special-item-model path: register a `SpecialModelRenderer.Unbaked` codec on `RegisterSpecialModelRendererEvent`, and use a `minecraft:special`/`ItemModelUtils.specialModel` definition for both lamp families. Keep all eight `ItemStackData` state combinations, the base item model's transforms/tints, and enchanted foil rendering.
- **Evidence:** Exact patched Minecraft 26.2.0.88 sources define `SpecialModelRenderer`, `SpecialModelWrapper`, and `ItemModelUtils.specialModel`; NeoForge 26.2.0.88 defines `RegisterSpecialModelRendererEvent` and `IClientItemExtensions` without `getCustomRenderer`. The old owned `LampItemRenderer` diagnostics are confined to removed custom-renderer/model APIs.
- **Blocks:** current full compilation through the common `LampBlockItem` hookup and lamp item rendering/variant visuals.
- **Owned dependency:** `client/renderer/item/LampItemRenderer.java`.

### 12. Provide a target machine model and block-entity renderer bridge

- **Files:** `src/main/java/com/gregtechceu/gtceu/client/ClientProxy.java#onRegisterModelLoaders`; the shared machine model/renderer registration path; `src/main/java/com/gregtechceu/gtceu/api/machine/MetaMachine.java#getRenderBoundingBox`.
- **Current contract:** `ClientProxy` registers `MachineModelLoader` as a legacy `IGeometryLoader`. It produces `MachineModel`, which combines baked machine-state variants, multipart selectors, covers, and dynamic renders in one legacy `BakedModel`/`BlockEntityRenderer` object. `MetaMachine.getRenderBoundingBox` retrieves the baked model for its block state and expects it to implement `IBlockEntityRendererBakedModel`.
- **Required change:** Register the target machine `CustomUnbakedBlockStateModel` codec through `RegisterBlockStateModels`, and connect its baked `BlockStateModel`/parts to machine-state selection using worker-safe model data. Move dynamic machine rendering and render bounds to a registered target `BlockEntityRenderer` with extracted immutable render state; provide a target item-rendering route for the machine item models. Include covers, cover text, monitor panels/placeholders, and Fusion Reactor bloom in the extracted-state submission path: current `IDynamicCoverRenderer`, `IMonitorRenderer`, and `IPlaceholderRenderer` callbacks use the removed `MultiBufferSource`, while `FusionRingRender` still attaches a bloom ticket to the live machine. Preserve monitor/image quads and text, cover-face orientation, Fusion Reactor visibility/fade, and distinct light-ring/bloom outputs. Do not query a live `MetaMachine` from target chunk-meshing workers or silently drop these render features.
- **Evidence:** Patched Minecraft 26.2.0.88 `BlockStateModel`/`BlockStateModelPart` collect target parts, while NeoForge 26.2.0.88 `CustomUnbakedBlockStateModel`, `RegisterBlockStateModels`, extracted `BlockEntityRenderer`, and `SubmitNodeCollector` replace the old model/renderer drawing entry points. NeoForge's `BlockStateModelExtension.collectParts` documentation says this runs on a meshing worker and calls for block-entity data from the passed level's model-data snapshot. The current `MetaMachine.getRenderBoundingBox` still looks up a `BakedModel`; latest integrated compile `build/friend-client-port-growing-plant-lambda-fix-verify.log` reports 27 errors in `MachineModel`, 26 in `MachineModelLoader`, 14 in `UnbakedMachineModel`, 39 in `MultiPartBakedModel`, six in `GrowingPlantRender`, and seven in `FusionRingRender`. Monitor, placeholder, and cover callbacks still expose removed `MultiBufferSource` signatures.
- **Blocks:** owned machine/multipart model compilation, machine block-entity rendering, and correct machine/item/cover visuals until the target association and data path are provided.
- **Owned dependency:** `client/model/machine/MachineModel.java`, `UnbakedMachineModel.java`, `MachineModelLoader.java`, `client/model/machine/multipart/**`, `client/renderer/BlockEntityWithBERModelRenderer.java`, `client/renderer/cover/IDynamicCoverRenderer.java`, `client/renderer/monitor/IMonitorRenderer.java`, `client/renderer/placeholder/QuadPlaceholderRenderer.java`, and `client/renderer/machine/impl/FusionRingRender.java`. The shared placeholder callback in `api/placeholder/IPlaceholderRenderer.java` and Fusion Reactor machine ticket field remain outside this assignment.

### 13. Replace the removed named bloom render-type contract

- **Files:** target client model/render registration and any shared resource/datagen path that exposes the public `gtceu:bloom` render-type name; current legacy registration is `src/main/java/com/gregtechceu/gtceu/client/bloom/BloomEventListeners.java#ModBus.registerNamedRenderTypes`.
- **Current contract:** `RegisterNamedRenderTypesEvent` maps the name `bloom` to block and entity render types, with a cutout fallback in safe mode. The target 26.2 API has no named-render-type registration event; `BlockModelRenderState` submits model parts through built-in cutout/translucent paths, while custom `RenderPipeline`s are registered independently. No owned render-type alias was found in current model JSON, but packs using the existing public name still rely on it.
- **Required change:** Supply an explicit 26.2 model/resource registration path that routes `gtceu:bloom` block and entity content into the existing bloom output pipeline when active and into the prior safe cutout behavior otherwise. Preserve the existing PNG metadata-driven bloom extraction path as well. Do not remove the registration or replace it with a no-op merely to clear the three current `BloomEventListeners` compiler errors.
- **Evidence:** The exact patched Minecraft 26.2.0.88 and NeoForge 26.2.0.88 source archives contain `RegisterRenderPipelinesEvent`, `BlockStateModelPart`, and `BlockModelRenderState`, but no `RegisterNamedRenderTypesEvent` or equivalent named-render-type registry. Patched `ChunkSectionLayer` is a fixed enum of `SOLID`, `CUTOUT`, and `TRANSLUCENT`; NeoForge `AddSectionGeometryEvent.SectionRenderingContext` can submit additional geometry only into one of those layers. Patched `LevelRenderer` has no `renderChunkLayer(RenderType, ...)` method for the old owned accessor. The custom bloom `OutputTarget` pipeline exists, but static section meshes have no current bridge to that target. Latest integrated compile `build/friend-client-port-growing-plant-lambda-fix-verify.log` continues to report the removed event and old `RenderType.cutoutMipped`/Forge entity fallback.
- **Blocks:** full compilation and compatibility for block/entity models that use the named bloom type; the in-tree PNG metadata bloom path remains separate.
- **Owned dependency:** `client/bloom/BloomEventListeners.java`, `client/renderer/GTRenderTypes.java`, and the bloom block geometry path.

### 14. Remove the obsolete LevelRenderer mixin registration

- **File:** `src/main/resources/gtceu.mixins.json`, entry `client.LevelRendererMixin`.
- **Current contract:** The owned `LevelRendererMixin` wrapped the 1.20 `LevelRenderer.renderHitOutline` call and drew colored material/tier/wire outlines plus AoE block outlines. That target method no longer exists in 26.2.
- **Required change:** Remove the `client.LevelRendererMixin` entry. AoE block-breaking stages now append immutable `BlockBreakingRenderState` snapshots from `ExtractLevelRenderStateEvent`; colored material/tier/wire and AoE outlines now use `ExtractBlockOutlineRenderStateEvent` and `CustomBlockOutlineRenderer`. Keep the independent bloom `client.bloom.LevelRendererMixin` entry.
- **Evidence:** Patched Minecraft 26.2.0.88 `LevelRenderer` has `submitBlockOutline` and `submitBlockDestroyAnimation`, not `renderHitOutline`; its outline path invokes `BlockOutlineRenderState.customRenderers()` before vanilla submission, and its break path submits `LevelRenderState.blockBreakingRenderStates`. NeoForge 26.2.0.88 event sources confirm both extraction contracts. The replacement renderers hold copied positions/shapes/colors/progress and no live level/entity references.
- **Blocks:** client startup/mixin application while the obsolete `renderHitOutline` injector remains; Java compilation is unaffected.
- **Owned dependency:** `client/renderer/ColoredAoEBlockOutlineRenderer.java`, `client/renderer/AoEBlockBreakingRenderer.java`, and removal of `core/mixins/client/LevelRendererMixin.java`.

### 15. Resolve the pinned 1.20.1 Embeddium bloom integration

- **Files:** `gradle/forge.versions.toml` (`embeddium` version and module), plus the shared optional-integration/mixin registration if no 26.2-compatible Embeddium build exists.
- **Current contract:** The project targets Minecraft/NeoForge 26.2 but compiles the bloom mixins against `org.embeddedt:embeddium-1.20.1:0.3.31-beta.53+mc1.20.1`. Both `client.bloom.normal.embeddium.BlockRendererMixin` and `client.bloom.safemode.embeddium.BlockRendererMixin` target the 1.20.1 `BlockRenderer` descriptor. Their `BakedQuadView` is an Embeddium interface, not a Minecraft 26.2 `BakedQuad`; `ModelQuadView` exposes geometry, sprite, light, and normals, but no target material-emission value.
- **Required change:** Supply a 26.2-compatible Embeddium API/artifact and update both mixins against its exact source and bytecode contracts, including a supported way to preserve per-quad bloom metadata and emission. If no such build is available, provide a compatible optional-integration implementation/registration plan that keeps bloom behavior and does not load 1.20.1 mixins into 26.2. Do not add an unchecked cast to `BakedQuad` or infer material emission from packed lighting.
- **Evidence:** `gradle/forge.versions.toml` pins the module above. Its only cached source jar is `embeddium-1.20.1-0.3.31-beta.53+mc1.20.1-sources.jar`; `BakedQuadView` extends `ModelQuadView`, which has no `materialInfo()` or emission accessor. The current exact owned diagnostics include invalid `BakedQuadView` to `BakedQuad` casts and removed 1.20 `VertexConsumer` calls. No 26.2 Embeddium artifact/source was found in the local Gradle cache.
- **Blocks:** compilation of both optional Embeddium bloom mixins; correct bloom behavior and mixin application whenever Embeddium is present.
- **Owned dependency:** `core/mixins/client/bloom/normal/embeddium/BlockRendererMixin.java` and `core/mixins/client/bloom/safemode/embeddium/BlockRendererMixin.java`.

### 16. Register the growing-stem fruit accessor

- **File:** `src/main/resources/gtceu.mixins.json`, client mixins list.
- **Current contract:** The new owned `client.StemBlockAccessorMixin` exposes `StemBlock`'s private `fruit` `ResourceKey<Block>` so recipe-progress previews can render the configured fruit after the stem reaches its mature stage. The shared mixin configuration does not yet list this accessor.
- **Required change:** Add `client.StemBlockAccessorMixin` to the client mixin list and retain the existing client mixin package/configuration conventions.
- **Evidence:** Patched Minecraft 26.2.0.88 `StemBlock` stores `fruit` as a private final `ResourceKey<Block>` used by its codec and provides no public getter. The owned preview resolves that key from the block registry and returns the fruit's default state. The accessor and preview compile without diagnostics other than the preview's unrelated removed `MultiBufferSource` callback signature.
- **Blocks:** Modded stem fruit preview behavior until the accessor is applied; Java compilation is unaffected.
- **Owned dependency:** `client/renderer/machine/impl/GrowingPlantRender.java` and `core/mixins/client/StemBlockAccessorMixin.java`.

## Resolved

None.
