# Code audit

NetherBreach was reviewed to remove unnecessary execution paths and keep its behavior limited to the Roof Perforator's intended purpose.

## Implemented safeguards

- Item identity uses PDC key `netherbreach:roof_perforator`; remaining uses are stored under `netherbreach:uses_remaining`.
- Breaking is allowed only for `BEDROCK` in the Nether, within the five layers below the dimension's logical height.
- All other block breaking, entity damage and vanilla durability loss are blocked while using the tool.
- Broken bedrock never produces a drop.
- Every enchantment is removed; enchanting, Mending, anvil, grindstone, smithing and crafting-repair paths are blocked.
- Native item glint is enabled as a visual effect only, without real enchantments.
- A cancellable `BlockBreakEvent` is emitted before the block changes so protection plugins can deny the operation.
- The block and pickaxe are validated again after the event to prevent inconsistent changes when another plugin modifies state.
- Messages and item metadata are localized in English and Italian. The client locale is read locally and is never transmitted or logged.
- Locale changes update items in the player's inventory without changing their internal use counter.
- Shift-click, drag, number keys and off-hand swapping cannot insert the tool into restricted interfaces.
- Malformed configuration values fall back safely without preventing plugin startup.
- No network request, telemetry, metrics, updater or additional runtime library is present.
