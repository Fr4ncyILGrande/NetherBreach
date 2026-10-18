# NetherBreach


NetherBreach is a Minecraft 26.2 plugin built exclusively against the Bukkit/Spigot API. It adds the **Roof Perforator**, a golden pickaxe designed solely to perforate Nether roof bedrock. It supports Paper, Purpur and Spigot 26.2.

## Behavior

- The Roof Perforator is a glowing `GOLDEN_PICKAXE` with 5 uses by default. It breaks after its fifth valid use and must then be crafted again.
- Server owners can change the use count with `uses` and the pickaxe material with `item.material` in `plugins/NetherBreach/config.yml`. Recognized items are aligned with the configured material whenever the plugin sanitizes them.
- It works only in the Nether, within the five layers below the dimension's logical height (Y=123–127 in a standard Nether), and only on `BEDROCK`.
- It cannot break other blocks or bedrock elsewhere, damage entities, produce bedrock drops, or use vanilla durability.
- Enchanting, enchanted books, Mending, anvil, grindstone, crafting repairs and smithing are blocked. Any externally added enchantment is removed at the next safe plugin boundary.
- The glint is visual only and uses Minecraft's native item glint. There is no real or hidden enchantment.
- A `PersistentDataContainer` marker in the `netherbreach` namespace identifies the item; its name and lore are not used as identity.
- The plugin contains no telemetry, metrics, updater, HTTP client, socket or runtime third-party dependency.

## Languages

English is the default language and Italian is included as a complete translation. The `language` section controls localization:

- `mode: auto` follows each player's English or Italian client locale. Unsupported locales use `fallback`.
- `mode: en` or `mode: it` forces English or Italian server-wide, including console messages.
- `fallback: en` is the default for the console, unsupported locales and items without a holder.
- `item-mode: holder` updates item name and lore to the holder's language. `item-mode: server` always uses the fallback language while `mode` is `auto`.

Messages and item text can be edited under `messages.en`, `messages.it`, `item.en` and `item.it`. An item stored in a shared container uses the fallback language until a player receives or handles it.

## Default recipe

```text
[Netherite scrap] [Diamond block] [Netherite scrap]
[     Empty      ] [  Blaze rod  ] [     Empty      ]
[     Empty      ] [  Blaze rod  ] [     Empty      ]
```

The shape and ingredients are configurable under `crafting`. Run `/netherbreach reload` after changing the configuration.

## Commands

- `/netherbreach give [player]` — permission `netherbreach.give`
- `/netherbreach reload` — permission `netherbreach.admin`

Italian subcommand aliases `dai` and `ricarica` are also accepted. Command aliases are `/nb` and `/roofperforator`.

## Building

Minecraft 26.2 requires Java 25 or newer. With Maven installed, run:

```bash
mvn -B clean verify
```

The JAR is generated at `target/NetherBreach-1.0.0.jar`. Copy it into the server's `plugins/` directory.

## GitHub

Upload the complete project contents, including `.github`, `src`, `pom.xml`, `LICENSE` and the documentation, to the repository root. On every push, GitHub Actions runs the tests with Java 25 and publishes the JAR as a workflow artifact.

## Compatibility

The project compiles against `org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT` and uses Bukkit/Spigot types only. Paper and Purpur provide the same API surface. Before changing a block, NetherBreach emits a cancellable `BlockBreakEvent` so protection plugins can prevent the operation.

## Ownership and license

Owner/author: **Fr4ncyILGrande**. Licensed under the MIT License. See [LICENSE](LICENSE).

