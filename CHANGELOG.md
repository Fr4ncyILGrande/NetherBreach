# Changelog

## 1.0.0

- First public release of NetherBreach for Minecraft 26.2 and Java 25, compatible with Paper, Purpur and Spigot.
- Added the Roof Perforator as a glowing `GOLDEN_PICKAXE` with no real enchantments, 5 default uses and a configurable use counter.
- Added a configurable recipe, automatic or forced English/Italian localization, and the MIT License.
- Fixed roof detection to use the Nether's logical height and accept Y=123–127 in a standard world rather than the incorrect Y=251–255 range.
- Prevented use-count restoration by combining Roof Perforators in crafting.
- Explicitly blocked Mending and use of the tool as an ingredient in any recipe.
- Hardened anvil, grindstone, smithing and inventory handling for drag, shift-click, number keys and off-hand swaps.
- Normalized externally modified items, including amount, vanilla damage, enchantments, language and use counter.
- Made item verification atomic during block breaking, including amount and metadata checks after protection events.
- Added safe fallbacks for invalid configuration values without disabling the plugin.
- Added automated roof and localization tests; `plugin.yml` now receives its version from Maven automatically.
