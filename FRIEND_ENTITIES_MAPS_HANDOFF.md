# Entities and Maps Port Handoff

## Checkout

- Branch: `codex/friend-entities-maps`
- Base: `b1daee2f206dd776a1a6fc181d2f6bebd271a16d` (latest locally available `1.20.1` integration commit at task start)
- Worktree: `C:\Users\georg\.codex\worktrees\friend-entities-maps\PatrickStarTech`
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

## Verification

- Baseline: `compileJava -PportDiagnostics --max-workers=1 --console=plain --offline`; `build/friend-entities-maps-baseline.log`; **2,794 errors**, including 18 in each boat entity and 57 under `integration/map`.
- After boat batch: same command; `build/friend-entities-maps-boats.log`; **2,757 errors**, with zero diagnostics in either owned boat entity. Map package still has 57 diagnostics.
- After cache batch: same command; `build/friend-entities-maps-cache.log`; **2,754 errors**. `ClientCacheManager`, `GTBoat`, and `GTChestBoat` have zero diagnostics; map package has 54 diagnostics. Three errors disappeared with the cache API migration.
- Attempted final integrated compile after the waypoint equality correction, but this execution context can no longer read the cached JDK's `conf/security/java.security` or write the existing worktree's `build` directory. Earlier integrated compile after all cache edits succeeded as above; the final equality-only change is a Java standard-library call and was not recompiled in this constrained context.
- `git diff --check` passed after the cache change; rerun against the final waypoint edit when a writable execution context is available.
- Tests/runtime checks are pending; do not claim gameplay behavior without running the game.

## Shared Ownership

- Do not edit tool/item/capability implementations, `GTBoatItem`, proxy/shared listeners, shared map dependencies or any particle/rendering files. Record precise requests here if required.
- To finish map rendering integrations, coordinate updated, locally resolvable 26.2 builds of Xaero Minimap/World Map, FTB Chunks, and JourneyMap API/mod; then port the provider renderer and widget contracts against those verified artifacts. JourneyMap event listener API appears to be routed through its API/plugin lifecycle rather than NeoForge's generic event bus and needs confirmation from the matching 26.2 API sources.
- Project `AI_POLICY.md` requires disclosure of AI-assisted code and human review before contribution.
