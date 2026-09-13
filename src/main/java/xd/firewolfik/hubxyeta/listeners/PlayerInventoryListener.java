package xd.firewolfik.hubxyeta.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.managers.ItemsManager;

public class PlayerInventoryListener implements Listener {

    private final Main plugin;
    private final ConfigManager config;
    private final ItemsManager itemsManager;

    public PlayerInventoryListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.itemsManager = plugin.getItemsManager();
    }

    private boolean canBypass(Player player) {
        return config.isAdminBypass() && player.hasPermission("hub.admin");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String itemId = itemsManager.getLobbyItemId(item);
        if (itemId != null) {
            event.setCancelled(true);
            itemsManager.executeItemActions(player, itemId);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (config.isPixelBattleWorld(player.getWorld())) {
            return;
        }

        if (canBypass(player)) {
            return;
        }

        if (config.isDisableMove()) {
            event.setCancelled(true);
            return;
        }

        if (itemsManager.isLobbyItem(event.getCurrentItem()) || itemsManager.isLobbyItem(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (event.getHotbarButton() != -1) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (itemsManager.isLobbyItem(hotbarItem)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (config.isPixelBattleWorld(player.getWorld())) {
            return;
        }

        if (canBypass(player)) {
            return;
        }

        if (config.isDisableMove()) {
            event.setCancelled(true);
            return;
        }

        if (itemsManager.isLobbyItem(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (itemsManager.isLobbyItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
