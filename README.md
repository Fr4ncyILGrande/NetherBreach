# NetherBreach

NetherBreach is a simple server plugin that lets players create a controlled opening in the Nether roof.

The plugin adds one special tool: the **Roof Perforator**. It is a glowing golden pickaxe designed exclusively for breaking bedrock in the highest five layers of the Nether. In a standard Nether world, this means bedrock between Y=123 and Y=127.

The Roof Perforator cannot break normal blocks, bedrock outside the Nether roof, or bedrock in other dimensions. It also cannot damage entities. This keeps the tool focused on one purpose without turning it into an overpowered pickaxe.

## How it works

By default, every Roof Perforator has **5 uses**. One use is consumed only after a valid Nether roof bedrock block is successfully removed.

After the fifth use, the pickaxe breaks and must be crafted again. It does not use normal Minecraft durability, so its durability bar cannot be repaired or manipulated.

The Roof Perforator cannot be:

- Enchanted
- Combined with enchanted books
- Repaired in an anvil
- Repaired with Mending
- Modified with a grindstone
- Upgraded with a smithing table
- Used in crafting recipes to restore its uses

Its enchanted appearance is only a visual glow. The item does not contain any real or hidden enchantments.

## Default crafting recipe



The resulting item is a glowing golden pickaxe named **Roof Perforator**.

## Configuration

Server owners can customize the plugin through `config.yml`.

You can change:

- The number of uses
- The pickaxe material
- The complete crafting recipe
- Item name and description
- Plugin messages
- Language behavior
- Whether crafting is enabled

After changing the configuration, use:

```text
/netherbreach reload
```

## Languages

NetherBreach includes complete English and Italian support.

By default, the plugin automatically checks each player’s Minecraft language:

- English players receive English messages and item descriptions.
- Italian players receive Italian messages and item descriptions.
- Unsupported languages use English as the fallback.

Server owners can also force English or Italian for everyone through the configuration.

## Commands

```text
/netherbreach give [player]
/netherbreach reload
```

Italian command alternatives are also available:

```text
/netherbreach dai [giocatore]
/netherbreach ricarica
```

Command aliases:

```text
/nb
/roofperforator
```

Before removing a bedrock block, NetherBreach sends a cancellable block-breaking event. This allows protection and claim plugins to prevent the action when necessary.
