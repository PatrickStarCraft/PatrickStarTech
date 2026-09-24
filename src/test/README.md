# Game tests and focused checks

## Minecraft/NeoForge 26.2 status

Minecraft 26.2 no longer provides the old reflection-based `GameTestRegistry` path. GT's current minimal registration uses NeoForge's `RegisterGameTestsEvent`, vanilla `FunctionGameTestInstance`, and a registered `TestFunctionLoader` in [GTGameTestRegistration.java](java/com/gregtechceu/gtceu/gametest/GTGameTestRegistration.java).

The representative test is registered as `gtceu:my_test`, calls `ExampleTest.myTest`, and uses `gtceu:empty` from `data/gtceu/structures/empty.nbt`. The generated identifier used by the former Forge registration has not yet been confirmed, so this is an explicit new mapping rather than a claimed identity. The current server task is `runGameTestServer`.

This is only a minimal registration slice. The remaining gameplay suite contains **183 legacy `@GameTest` annotations across 29 holder classes**. Their Forge-era discovery/holder annotations have not been migrated; do not assume these tests are currently registered or runnable. Pure NBT predicate assertions from six old GameTest methods were moved into the executable standalone NBT check. Ranged-fluid, ranged-item, and selected item-map-key contracts now run in focused JUnit suites, while world-dependent recipe GameTests remain pending. The old annotation-based examples in this document have therefore been removed rather than presented as current instructions.

The representative classes compiled against the configured 26.2 APIs in an isolated test source set. The attempted server launch stopped during ModularUI host initialization because its `common.WorldLoaderMixin` injection matched 0 of 1 targets. The event registration and test callback did not execute, so GameTest runtime registration remains unverified. The normal root server task also depends on the currently failing production compilation.

## Writing and registering a test

Use `GameTestHelper` in a callback with assertions and world actions appropriate to the behavior. Preserve structure fixtures under `src/test/resources/data/gtceu/structures/`. Register test identifiers, structures, batches, timeout and setup metadata through the current `RegisterGameTestsEvent` and target-version GameTest APIs. Keep the explicit registration close to the callback inventory so identifiers and fixture paths can be reviewed together.

Before migrating a group, compile its registration against the configured 26.2 sources and confirm the test server reaches discovery and reports at least one executed test. Preserve the old fixture, timeout, batch setup, assertions, and cleanup semantics. If runtime is blocked, label the group as source-migrated or compiled only; do not count it as passed.

`gametest/util/TestUtils.java` contains shared gameplay-test helpers. Review its behavior against current `GameTestHelper` APIs as test groups are migrated; its presence alone does not imply that old test annotations are discovered.

## Focused checks

The `scripts/check-port-*.ps1` scripts run selected JUnit or standalone checks using real configured Minecraft/NeoForge types and selected production sources. They do not load the complete GT mod or replace GameTests. The shared JUnit runner propagates a failed Gradle command and rejects missing or zero-test XML reports. `scripts/check-port-fluid-ingredients.ps1` exercises real ranged-fluid matching, collapse, network transport, malformed identifier decoding, and fluid lookup-key classes; its isolated fixtures disable optional KubeJS tag resolution. The recipe/persistence group also checks item-handler filtering, simulation, extraction, and clear semantics; `FluidHandlerList` filtered fill/drain simulation and multi-tank transfer limits; `IOFluidHandlerList` combined tank indices and direction filters; and fluid component preservation. Its IO enum uses a test-only `GTCEu.id` fixture; the handlers under test are the selected production classes. Script output states the selection and count; a zero-test selection is an error. Read each script for its source inclusion and isolation limits before relying on a result.

`scripts/check-port-fluid-storage.ps1` runs the selected real `FilteredFluidResourceHandler` and `ThermalFluidResourceHandler` with NeoForge's actual item access, component values, fluid resources, and transactions. This selected-source host supplies a test-only GT data-component holder and small tag/identifier fixtures because it does not load the GT registry. These checks do not verify registered item capabilities.

`scripts/check-port-quantum-fluid.ps1` runs the production `QuantumFluidResourceHandler` against NeoForge's actual transfer and transaction APIs. It covers simulation and nested/root rollback, stack-count protection, retained fluid/item components, malformed saved fluid, int-sized chunks, and capacity arithmetic through `Long.MAX_VALUE`. It does not load the registered quantum-tank capability.

`scripts/check-port-item-ingredients.ps1` runs the real ranged item ingredient helpers, `MapIngredientTypeManager`, `MapIngredientFunction`, `DataComponentItemStackMapIngredient`, and `ItemStackMapIngredient` against NeoForge 26.2. It checks wrapper-to-`DataComponentIngredient` dispatch, partial/exhaustive matching, exact `CUSTOM_DATA` equality, item identity, key symmetry, and count-insensitive sized/ranged keys. A narrow capability fixture supplies the content-class boundary. CommonProxy registration and `RecipeDB.find` are not exercised. The isolated source list also supplies compile-only siblings for unrelated circuit, NBT, and fluid-container branches.

`scripts/check-port-overclocking.ps1` runs production overclock arithmetic for standard voltage and duration limits, one-tick recipes, sub-tick EU/t and parallel behavior, and heating-coil discount steps. Its machine/recipe modifier fixtures are compile-only; it does not cover recipe application or in-world machine timing and energy use. The machine GameTests in `OverclockLogicTest` remain required when the server host can launch.

`scripts/check-port-spoil-context.ps1` checks detached nested NBT ownership and SpoilContext serialization. `scripts/check-port-spoilable-stack.ps1` additionally runs the actual `SpoilableItemStack`, `ItemStackData`, and `SpoilContext` sources against real Minecraft item/custom-data/NBT classes. The selected host uses compile-only capability, tooltip, and GT adapter fixtures; it does not execute spoil timing against a live server, transformations, or registered capabilities.

`scripts/check-port-plunger.ps1` executes the actual `PlungerBehavior` transactional sink against NeoForge's real item access and transaction classes. A test-only `ToolHelper` counter verifies that durability is requested only after root commit and is not requested on rollback or extraction; this is not a test of the real damage helper or in-world block interaction.

`scripts/check-port-portable-scanner-mode.ps1` runs the shared production `ScannerModeData` and `ItemStackData` against real Minecraft item/component/NBT classes. Both portable and prospector scanner behaviors use it. The checks cover default mode, ordinal wraparound, malformed negative ordinals, unrelated custom-data retention, and copy isolation. They do not load scanner energy, UI, or world-reporting behavior.

`scripts/check-port-enum-ordinal.ps1` checks production fallback and cycling for persisted enum ordinals. The helper is used by common cover configuration restore and paste paths, but the selected host does not instantiate or execute those cover interactions.

The memory-card paste handler now validates that the clicked block implements `ICopyable` before extracting required items. Its source is compile-checked in the current offline diagnostic, but player inventory and block-interaction behavior still needs the real server-side GameTest host.

`scripts/check-port-jetpack-fuel.ps1` executes the production fuel-selection helper with the real `FluidIngredient` and NeoForge `FluidStack` types. It covers reuse, replacement, empty-fluid clearing, and clearing stale matches when the current fluid is unaccepted or underfilled. It does not load the registered tank capability, recipe reload, or armor tick behavior.

`scripts/check-port-recipe-db.ps1` executes production `RecipeDB` and `Branch` with production map-ingredient dispatch and item/component lookup keys. It checks public input lookup and predicates, multiple input order, missing inputs, component matching with exact `CUSTOM_DATA` equality, `CompoundIngredient` alternatives, `IntersectionIngredient` candidate lookup, and direct/tag-intersection lookup using a temporary 26.2 `MappedRegistry`. Intersections index the union of their child keys as candidates; the caller-supplied validity predicate is responsible for the all-children match, as it is in normal RecipeDB lookup. Small recipe/category and capability fixtures supply only the registry-backed GT payload contracts; recipe loading, `CommonProxy` registration, and the full `RecipeHelper` machine check are not executed. The separate item-ingredient script still tests map dispatch and key equality without calling `RecipeDB.find`.

`scripts/check-port-magnet-state.ps1` executes `MagnetStateData` and `ItemStackData` against actual 26.2 item/component/NBT types. It covers activation toggles, unrelated data preservation, copied-stack independence, and a zero-count noncanonical stack carrying a stale active flag. It does not load magnet energy use, filter UI, or entity pickup.

`scripts/check-port-tool-mode.ps1` checks persisted tool mode ordinal fallback and cycling through `OrdinalModeData`. Wrench action filtering, player messages, and item-magnet entity filtering are not covered by that helper-only suite.

Useful command when the root build can compile:

```powershell
.\gradlew.bat runGameTestServer --offline --max-workers=1 --console=plain
```

Use the focused script matching the behavior for isolated checks. A passing focused suite is evidence only for its selected code and assertions.
