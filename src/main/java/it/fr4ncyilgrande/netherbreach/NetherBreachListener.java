package it.fr4ncyilgrande.netherbreach;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumSet;
import java.util.Set;

/** Event boundary: only a PDC-identified Roof Perforator may break blocks. */
public final class NetherBreachListener implements Listener {

    private static final Set<InventoryType> ENCHANTMENT_INVENTORIES = EnumSet.of(
            InventoryType.ANVIL,
            InventoryType.ENCHANTING,
            InventoryType.GRINDSTONE,
            InventoryType.SMITHING
    );

    private final RoofPerforatorManager manager;

    public NetherBreachListener(RoofPerforatorManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLeftClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == Event.Result.DENY
                || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!manager.isRoofPerforator(item)) {
            return;
        }

        event.setCancelled(true);
        manager.tryBreak(event.getPlayer(), event.getClickedBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!manager.isRoofPerforator(item)) {
            return;
        }
        if (manager.isDispatchedBreak(player, event.getBlock())) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }
        event.setCancelled(true);
        manager.tryBreak(player, event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (manager.isRoofPerforator(event.getItem())) {
            event.setCancelled(true);
            manager.sanitizeInventory(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemMend(PlayerItemMendEvent event) {
        if (manager.isRoofPerforator(event.getItem())) {
            event.setCancelled(true);
            manager.sanitizeInventory(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEnchantItem(EnchantItemEvent event) {
        if (manager.isRoofPerforator(event.getItem())) {
            event.setCancelled(true);
            manager.sanitize(event.getItem(), event.getEnchanter());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (manager.isRoofPerforator(event.getItem())) {
            event.setCancelled(true);
            manager.sanitize(event.getItem(), event.getEnchanter());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (manager.isRoofPerforator(event.getInventory().getItem(0))
                || manager.isRoofPerforator(event.getInventory().getItem(1))) {
            event.setResult(null);
            sanitizeTopInventory(event.getInventory(), event.getView().getPlayer() instanceof Player player ? player : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (manager.isRoofPerforator(event.getInventory().getItem(0))
                || manager.isRoofPerforator(event.getInventory().getItem(1))) {
            event.setResult(null);
            sanitizeTopInventory(event.getInventory(), event.getView().getPlayer() instanceof Player player ? player : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (manager.isRoofPerforator(event.getInventory().getItem(0))
                || manager.isRoofPerforator(event.getInventory().getItem(1))
                || manager.isRoofPerforator(event.getInventory().getItem(2))) {
            event.setResult(null);
            sanitizeTopInventory(event.getInventory(), event.getView().getPlayer() instanceof Player player ? player : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRestrictedInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!ENCHANTMENT_INVENTORIES.contains(topInventory.getType())) {
            return;
        }

        Player player = event.getWhoClicked() instanceof Player clickedPlayer ? clickedPlayer : null;
        manager.sanitize(event.getCurrentItem(), player);
        manager.sanitize(event.getCursor(), player);

        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < topInventory.getSize();
        boolean incomingFromCursor = clickedTop && manager.isRoofPerforator(event.getCursor());
        boolean incomingFromShiftClick = !clickedTop && event.isShiftClick()
                && manager.isRoofPerforator(event.getCurrentItem());
        boolean incomingFromHotbar = clickedTop && isHotbarRoofPerforator(event, player);
        boolean protectedResult = clickedTop && event.getSlotType() == InventoryType.SlotType.RESULT
                && containsRoofPerforator(topInventory);

        if (incomingFromCursor || incomingFromShiftClick || incomingFromHotbar || protectedResult) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRestrictedInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        boolean touchesTopInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= 0 && slot < topInventory.getSize());
        if (ENCHANTMENT_INVENTORIES.contains(topInventory.getType())
                && touchesTopInventory && manager.isRoofPerforator(event.getOldCursor())) {
            event.setCancelled(true);
            manager.sanitize(event.getOldCursor(), event.getWhoClicked() instanceof Player player ? player : null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Player player = event.getView().getPlayer() instanceof Player craftingPlayer ? craftingPlayer : null;
        if (containsRoofPerforator(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                manager.sanitize(ingredient, player);
            }
            return;
        }
        if (!manager.isRoofPerforator(event.getInventory().getResult())
                || player == null) {
            return;
        }
        event.getInventory().setResult(manager.createItem(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraft(CraftItemEvent event) {
        if (containsRoofPerforator(event.getInventory().getMatrix())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRestrictedInventoryClose(InventoryCloseEvent event) {
        if (!ENCHANTMENT_INVENTORIES.contains(event.getView().getTopInventory().getType())
                || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        manager.sanitizeInventory(event.getView().getTopInventory(), player);
        manager.sanitizeInventory(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.sanitizeInventory(event.getPlayer());
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        manager.sanitizeInventory(event.getPlayer(),
                manager.resolveItemLanguage(event.getPlayer(), event.getLocale()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (manager.isRoofPerforator(item)) {
            manager.sanitize(item, event.getPlayer());
            event.getItemDrop().setItemStack(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (manager.isRoofPerforator(item)) {
            manager.sanitize(item, player);
            event.getItem().setItemStack(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && manager.sanitizeMainHand(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clearPlayerState(event.getPlayer());
    }

    private boolean isHotbarRoofPerforator(InventoryClickEvent event, Player player) {
        if (player == null) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            return manager.isRoofPerforator(inventory.getItemInOffHand());
        }
        int hotbarButton = event.getHotbarButton();
        if (hotbarButton < 0 || hotbarButton > 8) {
            return false;
        }
        return manager.isRoofPerforator(inventory.getItem(hotbarButton));
    }

    private boolean containsRoofPerforator(Inventory inventory) {
        return containsRoofPerforator(inventory.getContents());
    }

    private boolean containsRoofPerforator(ItemStack[] items) {
        for (ItemStack item : items) {
            if (manager.isRoofPerforator(item)) {
                return true;
            }
        }
        return false;
    }

    private void sanitizeTopInventory(Inventory inventory, Player player) {
        manager.sanitizeInventory(inventory, player);
    }

}
