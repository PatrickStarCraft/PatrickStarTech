# Friend Particles Port Handoff

## Checkout

- Branch: `codex/friend-particles`
- Base: `b492fae9b85684d77f6d4ebe343912f9650d836f` (verified clean integration HEAD)
- Worktree: `C:\Users\georg\.codex\worktrees\friend-particles\PatrickStarTech`
- Integration: changes and checkpoint were copied onto the clean `1.20.1` working tree at the same base commit on 2026-09-23; left uncommitted per the user's earlier instruction.
- Scope changes so far: `MufflerParticle.java`, `HazardParticle.java`, `GTParticleManager.java`, this checkpoint.

## Completed

- Migrated MufflerParticle and HazardParticle from removed `TextureSheetParticle`/`ParticleRenderType` APIs to the exact 26.2 `SingleQuadParticle` API and its extracted quad render state.
- Updated provider methods to accept the target `RandomSource` argument.
- Retained the old sprite animation, opaque particle layer, quad-size curves, randomized colors, velocities, lifetime behavior, tick updates, collision behavior, and render-facing mode (the standard XYZ camera-facing quad).
- Muffler keeps its custom age/fade tick behavior; Hazard keeps superclass tick behavior and its non-colliding movement.
- Exact source checked: `build/moddev/artifacts/minecraft-patched-26.2.0.88-sources.jar`. `SingleQuadParticle` provides `extract`, sprite lookup and `Layer.OPAQUE`; `CampfireSmokeParticle` is the comparable vanilla smoke lifecycle reference; provider source confirms the new signature.
- Existing asset/provider wiring was reviewed and remains intact: `ClientProxy` registers both through `registerSpriteSet`; `assets/gtceu/particles/hazard.json` uses `minecraft:generic_0`, and `muffler.json` keeps the `minecraft:big_smoke_0` through `_11` sequence.

## Verification

- Baseline: `compileJava -PportDiagnostics --offline --no-parallel --max-workers=1 --console=plain`; log `build/friend-particles-baseline.log`; compile ran and reported **2,867 errors**.
- After Muffler: same integrated offline command; log `build/friend-particles-muffler.log`; **2,823 errors**, no MufflerParticle diagnostics.
- After Muffler + Hazard: same command; log `build/friend-particles-muffler-hazard.log`; **2,795 errors**, no MufflerParticle or HazardParticle diagnostics.
- Migrated the particle-count debug display in `GTParticleManager` from removed `CustomizeGuiOverlayEvent.DebugText` to a 26.2 `RegisterDebugEntriesEvent` entry. The coordinating code must wire this mod-bus event to `GTParticleManager.INSTANCE.registerDebugEntries`.
- Latest integrated compile: same command; log `build/friend-particles-final.log`; **2,794 errors**. Latest diagnostics under `client/particle`: 18 in `GTOverheatParticle.java`, 9 in `GTParticleManager.java`; none in MufflerParticle or HazardParticle.
- No tests or runtime/visual verification run. Full compilation remains blocked by unrelated project-wide port errors.
- `git diff --check` passes.

## Integration Boundary / Next Steps

- 26.2 NeoForge `RenderLevelStageEvent` is split into typed stages and no longer supplies the old stage/camera/frustum/partial-tick accessors. Custom geometry is submitted with `SubmitCustomGeometryEvent` and `SubmitNodeCollector`.
- `GTParticleManager` still batches `BufferBuilder` callbacks through removed `Tesselator`, RenderSystem depth calls, and old stage event accessors. Its renderer contract is coupled to `IRenderSetup` and `EffectRenderContext`.
- `GTOverheatParticle` bloom and normal passes still use the removed `BufferBuilder`/direct OpenGL draw setup. Supporting bloom end-to-end needs coordinated migration of shared `IRenderSetup`, `IBloomEffect`, `BloomHandler`/`BloomRenderer`, and `EffectRenderContext` to current render submission/pipelines. These files are outside this task's ownership.
- Precise integration requests: (1) migrate `IRenderSetup` and `IBloomEffect` away from `BufferBuilder` callbacks to current pipeline/custom-geometry submission, then update `BloomHandler`, `BloomRenderer`, and `EffectRenderContext` to provide camera/frustum/delta data and both normal/bloom passes; keep GTOverheat's shape, temperature colors, alpha, depth, and emission behavior. (2) Add a `RegisterDebugEntriesEvent` listener in `ClientProxy` that calls `GTParticleManager.INSTANCE.registerDebugEntries`, since 26.2 removed `CustomizeGuiOverlayEvent.DebugText`; the entry preserves particle back/front counts.
- Do not replace the overheat effect with a placeholder. Next: coordinate the shared render API migration described above; then finish adapting the particle manager and overheat geometry to that API. Preserve the existing bloom, depth, shape, temperature color and smoke behaviors.

## Human Review

Project `AI_POLICY.md` requires disclosure of AI-assisted code and human understanding/review before contribution. The two particle files need human review before integration.
