# Electric item capability migration

AI-assisted implementation and tests by OpenAI Codex. Human review is required before contribution under AI_POLICY.md.

## Implemented slice

- `ElectricItemCapabilities.ELECTRIC_ITEM` is the context-free NeoForge item capability `gtceu:electric_item`. `GTCapability.CAPABILITY_ELECTRIC_ITEM` aliases the same instance. Helpers return the nullable handler directly, without `LazyOptional`.
- CommonProxy now handles NeoForge's `RegisterCapabilitiesEvent`. Its electric registration scans the registered items, including addon implementations of GT interfaces. Electric tools receive their existing zero-base-capacity, tier-specific handler (capacity comes from stack data); component items resolve the first attached ElectricStats. Non-electric component items return null.
- Every query creates an ElectricItem view over the supplied stack, not a copy. Charge stays in the component-backed ElectricItemData layer. No handler state or lookup result is cached across inventory replacement. Retained handlers belong to the original stack and must not be reused for another slot's replacement stack.
- Removed ElectricStats' obsolete Forge provider interface and the electric branch of the tools' old initCapabilities path. Fluid/tool-behavior providers remain pending; they were not replaced with empty stubs.
- Extracted the existing voltage table into GTVoltages to let the handler initialize without configuration/bootstrap dependencies. GTValues.V aliases the exact same mutable array; tier values and public array behavior are unchanged.

## Verification

Run:

```powershell
./scripts/check-port-item-components.ps1 -JavaHome 'C:/Program Files/Java/jdk-25.0.4' -SkipAssets
```

19 tests pass: 15 existing component tests plus four capability tests. The latter use a real RegisterCapabilitiesEvent (constructed reflectively because its constructor is package-private), vanilla item fixtures, NeoForge provider dispatch and the actual ElectricItem implementation. They cover live-stack writes, independent copies, subsequent component edits, simulation, tier and rate restrictions, capacity, empty/unsupported/multi-count stacks, external-discharge restrictions and a simulated-then-committed transfer conserving total charge. The transfer test orchestrates the two real handlers; it does not execute ElectricStats' inventory tick.

The isolated loader does not load GT's CommonProxy, tool/armor hierarchy or registry scan. Those integration paths are source-wired but not runtime-verified. The explicit asset-skipping option is for headless tests only, not proof of valid game resources.

Full diagnostic compilation reports **9,468 errors** and still fails. No diagnostics name the new capability or registration classes. ElectricStats retains its two existing EmptyHandler diagnostics. Logs: `build/electric-capability-tests.log` and `build/electric-capability-compile.log`. Error counts include cascades and do not measure completion.

## Remaining capability work

- GTCapability's other Forge declarations and legacy type-registration method are still unported. The NeoForge event handler only registers electric providers so far; there is no claim that the other capabilities function.
- Block energy, inventory and fluid providers need side/cover filtering and explicit invalidation when the exposed handler changes, with tests for replacement/removal and caching.
- Forge energy compatibility, transactional item/fluid transfer, other item providers, entity medical data and dispatcher mixins remain separate migrations.
- Addons formerly implementing the Forge component provider for electric items must register a NeoForge provider, or migrate to ElectricStats. The old provider signature is not a compatibility bridge.
- Full-mod compilation, client/server startup, equipment ticking, machine transfers and world-save migration remain unverified.
