# DeepSeek Flash: bounded porting work

AI-assisted handoff written by OpenAI Codex. Follow AI_POLICY.md; disclose the tools used and obtain human review before contributing.

## Your assignment

Continue repetitive, behavior-preserving migrations for PatrickStarTech's Minecraft 26.2 / NeoForge 26.2.0.88 port. Work in small verified batches. Do not redesign shared systems or try to make the entire mod compile by removing functionality.

Read repository instructions, PORTING-26.2.md, docs/porting/item-components.md and docs/porting/electric-capabilities.md first. Actual checked-out source and dependency artifacts take precedence over historical notes. Do not assume an old Minecraft API still exists or that a similar name has the same semantics.

## Coordination before editing

- The default branch is named `1.20.1`, despite containing the 26.2 port.
- Work on the user's `deepseek` branch in a **separate worktree or clone**. Two agents switching branches in the same directory are not isolated.
- At handoff creation, electric-capability work is uncommitted in the main checkout. Do not reset, stash, stage, commit, clean, or overwrite someone else's changes. A worktree created from a commit will not contain those uncommitted changes. Ask the user to provide a committed baseline when a task needs them.
- Inspect status, recent commits and your diff before each batch. Do not merge into or push to `1.20.1`. Commit only your own files when authorized; report commit IDs for review.
- Maintain your notes in `docs/porting/deepseek-flash-progress.md`, not in the shared root porting document.

## Task queue

### 1. Compiler-guided mechanical cleanup

Establish the current diagnostic compile baseline, then group errors by root cause. Pick one proven replacement family and change at most about 10 files in the first batch.

Good candidates, only where still present:

- Unused imports of removed classes. Confirm there are no live references; mentions inside comments alone do not require imports. For example, NanoMuscleSuite had an unused ArmorItem import at handoff creation.
- Moved classes with genuinely equivalent contracts, outside the reserved areas below.
- Simple field-to-accessor replacements where the actual target source confirms equivalent behavior.
- NBT primitive reads with explicit defaults matching the original implementation, outside ItemStack migration and reserved files. Preserve numeric conversion and malformed/missing-value behavior.

Do not blindly replace `net.minecraftforge` with `net.neoforged`. Many APIs were redesigned or removed. Before applying a repeated edit, record the old behavior, the target API and the evidence that they match.

### 2. Remaining armor custom-data consumers

After the first cleanup batch is verified, inspect these candidates for a narrowly scoped item-data migration:

- AdvancedNanoMuscleSuite
- AdvancedQuarkTechSuite
- Jetpack
- PowerlessJetpack

Check their current source before assuming work remains. Limit edits to reads and writes of **GT-owned item custom state**. Use the established ItemStackData, ElectricItemData, NightVisionItemData and ArmorMovementItemData helpers where their contracts match. Do not modify tick policy, movement, effects, capability providers, fluid handling, rendering or energy-transfer algorithms.

Critical rules:

- `ItemStackData.read` returns a detached snapshot. Mutating it does not persist anything.
- Write through update/updateCompound or the typed helper setters. Migrate every writer before replacing any accessor that formerly returned live mutable NBT.
- Merge only the fields being changed. Writing back a whole snapshot after an energy discharge can restore stale charge and duplicate energy.
- Preserve defaults, numeric widths and conversions. Some settings have intentionally different absent-value defaults in toggles versus movement handlers.
- Vanilla-owned durability, enchantments, names, unbreakability and block-entity payloads require dedicated component decisions. Do not move them into CUSTOM_DATA.
- Block/entity save NBT and recipe/network payloads are not automatically item custom data.
- If the change requires a new shared abstraction or edits to reserved files, stop that candidate and document the dependency. Move to another safe task.

Add focused tests for any nontrivial state conversion. Use actual Minecraft/NeoForge classes, not fake API stubs. Avoid editing the shared test harness while the capability contributor is using it; place proposed tests in a separate test class and coordinate harness inclusion.

## Reserved: do not edit without explicit coordination

- Capability APIs, providers, registration, helpers, caches, dispatcher mixins and transfer algorithms; machine, cable, pipe and cover capability behavior.
- GTCapability, GTCapabilityHelper, ElectricItemCapabilities, ElectricItemCapabilityRegistration, ElectricItem, ElectricStats, GTValues, GTVoltages, IGTTool, IComponentItem and CommonProxy.
- The shared item-data helpers under `api/item/data`, existing component test classes, and `scripts/check-port-item-components.ps1` / `scripts/port-item-components.init.gradle`.
- Recipe/ingredient architecture, recipe serialization, networking and packet registration.
- Rendering, model loading, EMI integration and ModularUI internals.
- Build configuration, dependency versions, Java/Lombok versions and global formatting configuration.
- Other contributors' active files. If ownership is unclear, ask before modifying them.

You may inspect these areas to understand callers. Inspection is not permission to modify them.

## Verification loop

1. Record your starting commit and working-tree state.
2. Read the actual old and target APIs. Cached source jars are useful; the local ignored `build/read-port-source.ps1` helper may exist, but is not guaranteed in another clone. Otherwise inspect the installed source artifacts or official documentation.
3. Implement a small, coherent batch. No broad regex replacement without reviewing every resulting diff.
4. Run the diagnostic compile and applicable isolated checks. Compare errors in touched files and affected callers, not only the total.
5. Inspect the diff for changed behavior and run `git diff --check`.
6. Log results and outstanding risks. Then start the next batch.

Example full diagnostic command, after setting JAVA_HOME to the installed Java 25:

```powershell
./gradlew.bat compileJava -PportDiagnostics --max-workers=1 --console=plain *> build/deepseek-flash-compile.log
```

Existing focused checks (inspect their prerequisites first):

```powershell
./scripts/check-port-recipes.ps1 -JavaHome $env:JAVA_HOME
./scripts/check-port-valueproviders.ps1 -JavaHome $env:JAVA_HOME
./scripts/check-port-nbt-predicates.ps1 -JavaHome $env:JAVA_HOME
./scripts/check-port-item-components.ps1 -JavaHome $env:JAVA_HOME -SkipAssets
```

The last option explicitly bypasses game assets for headless unit checks; it is not an asset validation. Do not run multiple builds or test harnesses concurrently in the same worktree. Do not treat a cached result from a different checkout as your verification.

Last main-checkout results at handoff: 19 component/electric-capability tests passed; full compilation failed with 9,468 errors. Those results include uncommitted capability work and may not match your branch. Earlier merged item-component work reported 9,482 errors. Re-establish your own baseline. Error totals include cascades and are not a completion percentage.

## Never do these to silence errors

- Remove features, return dummy values, disable integrations, comment out implementations or weaken tests.
- Add manual getters/setters merely because Lombok-generated methods are missing. Determine the actual cause first; annotation-processing failures can cascade.
- Replace methods based only on name similarity or convert nullable/optional return values without checking semantics.
- Claim a successful full port because isolated tests pass or one file has no diagnostics.
- Commit generated logs, build outputs, local cache configuration, secrets or another contributor's work.

## Handoff after each batch

Report branch/base/commit IDs; files changed; old-to-new API mappings with evidence; preserved semantics; exact commands and results; before/after diagnostics; tests added; and remaining runtime risks. State clearly whether tests were actually run. Keep architectural questions separate from safe mechanical work so the other contributor can address them without untangling unrelated edits.

Start with task 1. Continue through safe batches; if only architectural or reserved work remains, provide a concrete blocker list rather than expanding scope.
