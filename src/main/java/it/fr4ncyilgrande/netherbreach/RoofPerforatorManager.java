package it.fr4ncyilgrande.netherbreach;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Manages tool identity, recipe, configuration and controlled block breaking. */
public final class RoofPerforatorManager {

    private static final int DEFAULT_USES = 5;
    private static final int ROOF_LAYER_DEPTH = 5;

    private final NetherBreach plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey usesKey;
    private final NamespacedKey recipeKey;
    private final Set<BlockKey> dispatchedBreaks = new LinkedHashSet<>();
    private final Map<UUID, Long> lastFeedbackNanos = new HashMap<>();

    private int maxUses = DEFAULT_USES;
    private Material itemMaterial = Material.GOLDEN_PICKAXE;

    public RoofPerforatorManager(NetherBreach plugin) {
        this.plugin = plugin;
        identityKey = new NamespacedKey(plugin, "roof_perforator");
        usesKey = new NamespacedKey(plugin, "uses_remaining");
        recipeKey = new NamespacedKey(plugin, "roof_perforator");
    }

    public void reloadValues() {
        FileConfiguration config = plugin.getConfig();
        maxUses = positiveInt(config, "uses", DEFAULT_USES, 1, 1_000_000);

        String configuredMaterial = config.getString("item.material", "GOLDEN_PICKAXE");
        Material parsed = parseMaterial(configuredMaterial);
        if (parsed == null || !isPickaxe(parsed)) {
            plugin.warning("Invalid item.material: GOLDEN_PICKAXE will be used.",
                    "Il valore item.material non è valido: verrà usato GOLDEN_PICKAXE.");
            itemMaterial = Material.GOLDEN_PICKAXE;
        } else {
            itemMaterial = parsed;
        }
    }

    public void registerRecipe() {
        try {
            Optional<ShapedRecipe> recipe = buildRecipe();
            if (recipe.isEmpty()) {
                plugin.warning("The recipe was not registered because its configuration is invalid.",
                        "La ricetta non è stata registrata perché la configurazione non è valida.");
                return;
            }
            if (!Bukkit.addRecipe(recipe.get())) {
                plugin.warning("The Roof Perforator recipe could not be registered.",
                        "Impossibile registrare la ricetta del Roof Perforator.");
                return;
            }
            plugin.info("Roof Perforator recipe registered.", "Ricetta del Roof Perforator registrata.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            plugin.warning("Invalid recipe: " + exception.getMessage(),
                    "Ricetta non valida: " + exception.getMessage());
        }
    }

    public void removeRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }

    public ItemStack createItem() {
        return createItem(null);
    }

    public ItemStack createItem(Player holder) {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("The configured pickaxe material does not support item metadata.");
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(identityKey, PersistentDataType.BYTE, (byte) 1);
        data.set(usesKey, PersistentDataType.INTEGER, maxUses);
        applyCleanMeta(meta, maxUses, plugin.resolveItemLanguage(holder));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRoofPerforator(ItemStack item) {
        if (item == null || item.getType().isAir() || !isPickaxe(item.getType())) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    /** Removes every enchantment and rebuilds the item's logical state. */
    public void sanitize(ItemStack item) {
        sanitize(item, (Player) null);
    }

    public void sanitize(ItemStack item, Player holder) {
        if (!isRoofPerforator(item)) {
            return;
        }
        int uses = getUses(item);
        item.setType(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(identityKey, PersistentDataType.BYTE, (byte) 1);
        data.set(usesKey, PersistentDataType.INTEGER, uses);
        applyCleanMeta(meta, uses, plugin.resolveItemLanguage(holder));
        item.setItemMeta(meta);
        item.setAmount(1);
    }

    public int getUses(ItemStack item) {
        if (!isRoofPerforator(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer uses = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        if (uses == null) {
            return maxUses;
        }
        return Math.max(0, Math.min(uses, maxUses));
    }

    public boolean isAllowedRoofBlock(Block block) {
        if (block == null || block.getWorld().getEnvironment() != World.Environment.NETHER) {
            return false;
        }
        if (block.getType() != Material.BEDROCK) {
            return false;
        }
        World world = block.getWorld();
        return isWithinRoofLayer(block.getY(), world.getMinHeight(), world.getMaxHeight(),
                world.getLogicalHeight());
    }

    public void tryBreak(Player player, Block block) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isRoofPerforator(item)) {
            return;
        }
        sanitize(item, player);
        player.getInventory().setItemInMainHand(item);

        TargetStatus status = targetStatus(block);
        if (status != TargetStatus.ALLOWED) {
            sendTargetMessage(player, status);
            return;
        }

        int uses = getUses(item);
        if (uses <= 0) {
            player.getInventory().setItemInMainHand(null);
            plugin.sendMessage(player, "messages.item-consumed");
            return;
        }

        ItemStack transactionItem = item.clone();
        BlockKey key = BlockKey.of(player, block);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        breakEvent.setDropItems(false);
        dispatchedBreaks.add(key);
        try {
            Bukkit.getPluginManager().callEvent(breakEvent);
        } finally {
            dispatchedBreaks.remove(key);
        }

        if (breakEvent.isCancelled()) {
            sendFeedback(player, "messages.protected");
            return;
        }
        if (targetStatus(block) != TargetStatus.ALLOWED) {
            return;
        }
        ItemStack currentItem = player.getInventory().getItemInMainHand();
        if (!isRoofPerforator(currentItem) || !currentItem.equals(transactionItem)) {
            // Another plugin changed the item during the event. Abort to avoid
            // inconsistent use consumption or block breaking.
            return;
        }

        block.setType(Material.AIR, true);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5),
                12, 0.25, 0.25, 0.25, Material.BEDROCK.createBlockData());

        int remaining = uses - 1;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(null);
            plugin.sendMessage(player, "messages.item-consumed");
            return;
        }

        updateUses(currentItem, remaining, player);
        player.getInventory().setItemInMainHand(currentItem);
        plugin.sendMessage(player, "messages.block-broken", "{uses}", Integer.toString(remaining));
    }

    public boolean isDispatchedBreak(Player player, Block block) {
        return dispatchedBreaks.contains(BlockKey.of(player, block));
    }

    public boolean sanitizeMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isRoofPerforator(item)) {
            return false;
        }
        sanitize(item, player);
        player.getInventory().setItemInMainHand(item);
        return true;
    }

    public void sanitizeInventory(Player player) {
        sanitizeInventory(player, plugin.resolveItemLanguage(player));
    }

    public void sanitizeInventory(Player player, PluginLanguage language) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isRoofPerforator(item)) {
                sanitize(item, language);
                inventory.setItem(slot, item);
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (isRoofPerforator(cursor)) {
            sanitize(cursor, language);
            player.setItemOnCursor(cursor);
        }
    }

    public void sanitizeInventory(Inventory inventory, Player holder) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isRoofPerforator(item)) {
                sanitize(item, holder);
                inventory.setItem(slot, item);
            }
        }
    }

    public PluginLanguage resolveItemLanguage(Player holder, String clientLocale) {
        return plugin.resolveItemLanguage(holder, clientLocale);
    }

    public void sanitize(ItemStack item, PluginLanguage language) {
        if (!isRoofPerforator(item)) {
            return;
        }
        int uses = getUses(item);
        item.setType(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(identityKey, PersistentDataType.BYTE, (byte) 1);
        data.set(usesKey, PersistentDataType.INTEGER, uses);
        applyCleanMeta(meta, uses, language);
        item.setItemMeta(meta);
        item.setAmount(1);
    }

    public void clearPlayerState(Player player) {
        lastFeedbackNanos.remove(player.getUniqueId());
    }

    private TargetStatus targetStatus(Block block) {
        if (block == null || block.getWorld().getEnvironment() != World.Environment.NETHER) {
            return TargetStatus.WRONG_DIMENSION;
        }
        if (block.getType() != Material.BEDROCK) {
            return TargetStatus.WRONG_BLOCK;
        }
        return isAllowedRoofBlock(block) ? TargetStatus.ALLOWED : TargetStatus.WRONG_HEIGHT;
    }

    private void sendTargetMessage(Player player, TargetStatus status) {
        if (!canSendFeedback(player)) {
            return;
        }
        switch (status) {
            case WRONG_DIMENSION -> plugin.sendMessage(player, "messages.wrong-dimension");
            case WRONG_BLOCK -> plugin.sendMessage(player, "messages.wrong-block");
            case WRONG_HEIGHT -> plugin.sendMessage(player, "messages.wrong-height");
            case ALLOWED -> { }
        }
    }

    private void sendFeedback(Player player, String path) {
        if (canSendFeedback(player)) {
            plugin.sendMessage(player, path);
        }
    }

    private boolean canSendFeedback(Player player) {
        long now = System.nanoTime();
        Long previous = lastFeedbackNanos.get(player.getUniqueId());
        if (previous != null && now - previous < 500_000_000L) {
            return false;
        }
        lastFeedbackNanos.put(player.getUniqueId(), now);
        return true;
    }

    private void updateUses(ItemStack item, int uses, Player holder) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, uses);
        applyCleanMeta(meta, uses, plugin.resolveItemLanguage(holder));
        item.setItemMeta(meta);
    }

    private void applyCleanMeta(ItemMeta meta, int uses, PluginLanguage language) {
        meta.setDisplayName(colorize(plugin.localizedItemName(language)));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.localizedItemLore(language)) {
            lore.add(colorize(line.replace("{uses}", Integer.toString(uses))));
        }
        meta.setLore(lore);

        for (org.bukkit.enchantments.Enchantment enchantment : Set.copyOf(meta.getEnchants().keySet())) {
            meta.removeEnchant(enchantment);
        }
        meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setUnbreakable(false);
        // The glint is visual only: it neither adds nor simulates an enchantment.
        meta.setEnchantmentGlintOverride(true);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
    }

    private Optional<ShapedRecipe> buildRecipe() {
        FileConfiguration config = plugin.getConfig();
        List<String> configuredShape = config.getStringList("crafting.recipe.shape");
        if (configuredShape.size() != 3 || configuredShape.stream().anyMatch(row -> row == null || row.length() != 3)) {
            plugin.warning("crafting.recipe.shape must contain exactly three rows of three characters.",
                    "crafting.recipe.shape deve contenere esattamente tre righe da tre caratteri.");
            return Optional.empty();
        }

        ConfigurationSection ingredients = config.getConfigurationSection("crafting.recipe.ingredients");
        if (ingredients == null) {
            plugin.warning("The crafting.recipe.ingredients section is missing.",
                    "Manca la sezione crafting.recipe.ingredients.");
            return Optional.empty();
        }

        Set<Character> symbols = new LinkedHashSet<>();
        for (String row : configuredShape) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ') {
                    symbols.add(symbol);
                }
            }
        }
        if (symbols.isEmpty()) {
            plugin.warning("The recipe shape cannot be empty.", "La forma della ricetta non può essere vuota.");
            return Optional.empty();
        }

        Map<Character, Material> materials = new java.util.LinkedHashMap<>();
        for (char symbol : symbols) {
            String materialName = ingredients.getString(Character.toString(symbol));
            Material material = parseMaterial(materialName);
            if (material == null || material.isAir() || !material.isItem()) {
                plugin.warning("Invalid material for symbol '" + symbol + "': " + materialName,
                        "Materiale non valido per il simbolo '" + symbol + "': " + materialName);
                return Optional.empty();
            }
            materials.put(symbol, material);
        }

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createItem());
        recipe.shape(configuredShape.toArray(String[]::new));
        materials.forEach(recipe::setIngredient);
        return Optional.of(recipe);
    }

    private int positiveInt(FileConfiguration config, String path, int fallback, int min, int max) {
        int value = config.getInt(path, fallback);
        if (value < min || value > max) {
            plugin.warning(path + " must be between " + min + " and " + max
                            + "; " + fallback + " will be used.",
                    "Il valore " + path + " deve essere compreso tra " + min + " e " + max
                            + "; verrà usato " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private Material parseMaterial(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Material.matchMaterial(value.trim());
        }
    }

    private boolean isPickaxe(Material material) {
        return material != null && material.name().endsWith("_PICKAXE");
    }

    static boolean isWithinRoofLayer(int y, int minHeight, int maxHeight, int logicalHeight) {
        int roofExclusive = Math.min(maxHeight, logicalHeight);
        if (roofExclusive <= minHeight) {
            return false;
        }
        int roofTop = roofExclusive - 1;
        int roofBottom = Math.max(minHeight, roofTop - ROOF_LAYER_DEPTH + 1);
        return y >= roofBottom && y <= roofTop;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private enum TargetStatus {
        ALLOWED,
        WRONG_DIMENSION,
        WRONG_BLOCK,
        WRONG_HEIGHT
    }

    private record BlockKey(UUID world, UUID player, int x, int y, int z) {
        private static BlockKey of(Player player, Block block) {
            return new BlockKey(block.getWorld().getUID(), player.getUniqueId(),
                    block.getX(), block.getY(), block.getZ());
        }
    }
}
