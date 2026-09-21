# Generated recipe boundary — 26.2

AI-assisted implementation and tests by OpenAI Codex. Human review is required by AI_POLICY.md before contribution.

## Why this representation

GT constructs recipes at runtime and feeds JSON resources to GTDynamicDataPack. Its generator callbacks previously used Minecraft's removed FinishedRecipe interface. GeneratedRecipe is now a GT-owned immutable JSON resource artifact, not a substitute implementation of a removed Minecraft class and not a runtime Recipe implementation.

The artifact holds the recipe ID and JSON, plus an optional advancement ID/JSON pair. Construction and access defensively copy JSON. Advancement ID/data must either both be present or both absent. The serializer-based factory snapshots builder output immediately and writes the selected registered serializer's ID into the outer `type` field. Deferred mutation of a builder after build() no longer changes an already-built artifact.

All in-repository generator callbacks, addon recipe hooks, builder save methods, the recipe filter and the dynamic pack now use Consumer<GeneratedRecipe>. The six custom builder implementations produce artifacts directly; there are no anonymous FinishedRecipe implementations left. Existing recipe-ID prefixes are preserved. Addons must update their callback type and use artifact accessors instead of the old interface methods.

## Native builders and resources

GeneratedRecipeOutput implements the actual 26.2 RecipeOutput interface. It encodes vanilla recipes and their unlock advancements with explicit registry context and NeoForge conditional codecs, preserving conditions on both resources. VanillaRecipeHelper's smithing builder uses this adapter with the GT built-in registry provider and a ResourceKey<Recipe<?>>.

The adapter attaches recipe advancements to vanilla's existing `minecraft:recipes/root`. Explicit requests to generate a replacement root fail with an UnsupportedOperationException rather than being silently discarded; the existing smithing path does not request one. A future standalone generator requiring its own root needs an advancement-only output sink.

Runtime pack paths and debug-dump directories now use singular `recipe/` and `advancement/`, matching the target version. The former plural paths would not expose those resources to modern loaders.

## Verification

Fresh full-compilation baseline at c6a3c877a: **9,292 errors**. Final diagnostic compile after these changes: **8,887 errors**, a reduction of 405. Compilation still fails; counts include cascading errors.

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-25.0.4'
./gradlew.bat compileJava -PportDiagnostics --max-workers=1 --console=plain
./scripts/check-port-recipes.ps1 -JavaHome $env:JAVA_HOME
./scripts/check-port-item-components.ps1 -JavaHome $env:JAVA_HOME -SkipAssets
```

- Recipe suite: **19 passing tests**, including four new tests for defensive snapshots, advancement pairing, serializer dispatch/resource paths, an actual vanilla smithing-builder recipe/advancement codec round trip, and conditions on both outputs. The harness restores normal ModularUI metadata afterward.
- Component/capability suite: **23 passing tests**, reused from the Gradle cache for unchanged test inputs in this pass.
- Logs: `build/under9000-baseline.log`, `build/under9000-final.log`, `build/under9000-recipe-tests.log`, `build/under9000-capability-tests.log`.

## Remaining work

This ports the generator-to-pack boundary, not every recipe body. Custom builders still contain obsolete ingredient JSON methods, old stack/NBT serialization and other API mismatches. Those must be migrated to real ingredient and component-aware stack codecs. Existing energy/fluid crafting recipes also need their remaining component/API changes. Full GT recipe registration, dynamic-pack metadata, reload behavior, complete data generation, game startup and gameplay have not passed.

The focused tests use the real game and NeoForge codecs with minimal vanilla item component fixtures; they do not load GT's unfinished registry/bootstrap or execute its full generator catalog. Do not interpret the lower compiler count as a playable port or a completion percentage.
