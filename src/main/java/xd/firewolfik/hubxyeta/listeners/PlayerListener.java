package xd.firewolfik.hubxyeta.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.managers.ItemsManager;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final ConfigManager config;
    private final ItemsManager itemsManager;
    private final Map<Player, BukkitTask> actionBarTasks = new HashMap<>();

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.itemsManager = plugin.getItemsManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (config.isHideStream()) {
            event.setJoinMessage(null);
        }

        if (config.isFirstJoinMessageEnabled()) {
            if (!plugin.getDatabaseManager().isPlayerExists(player.getUniqueId())) {
                int playerNumber = plugin.getDatabaseManager().addPlayer(player.getUniqueId(), player.getName());

                if (playerNumber > 0) {
                    String message = plugin.getMessagesConfig().getString("messages.first-join-msg");
                    if (message != null) {
                        message = message
                                .replace("%player%", player.getName())
                                .replace("%number%", String.valueOf(playerNumber));

                        String finalMessage = ColorUtil.getInstance().translateColor(message);
                        Bukkit.broadcastMessage(finalMessage);
                    }
                }
            }
        }

        if (config.isHidePlayer()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> hidePlayersForPlayer(player));
        }

        if (config.isLobbyLocationSet()) {
            player.teleport(config.getSafeLobbyLocation());
        } else {
            plugin.getLogger().warning("Игрок " + player.getName() + " зашел, но спавн лобби не установлен!");
        }

        config.setupPlayer(player);

        itemsManager.giveAllItems(player);

        if (config.isClearChat()) {
            clearPlayerChat(player);
        }

        sendJoinMessages(player);

        sendJoinTitle(player);

        config.setupWorld(player.getWorld());

        if (config.isActionBarEnabled()) {
            startActionBar(player);
        }

        if (plugin.getDatabaseManager().isHidePlayersEnabled(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online != player) {
                        player.hidePlayer(plugin, online);
                    }
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (config.isHideStream()) {
            event.setQuitMessage(null);
        }

        if (config.isClearItems()) {
            player.getInventory().clear();
        }
    }

    private void hidePlayersForPlayer(Player newPlayer) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != newPlayer) {
                online.hidePlayer(plugin, newPlayer);
                newPlayer.hidePlayer(plugin, online);
            }
        }
    }

    private void clearPlayerChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
    }

    private void sendJoinMessages(Player player) {
        for (String message : config.getJoinMessages()) {
            if (!message.trim().isEmpty()) {
                String coloredMessage = ColorUtil.getInstance().translateColor(message);
                player.sendMessage(coloredMessage);
            }
        }
    }

    private void sendJoinTitle(Player player) {
        if (!config.getJoinTitle().isEmpty() || !config.getJoinSubtitle().isEmpty()) {
            String title = ColorUtil.getInstance().translateColor(config.getJoinTitle());
            String subtitle = ColorUtil.getInstance().translateColor(config.getJoinSubtitle());
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJumpVoid(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        double voidHeight = -64.0;

        if (event.getTo() != null && player.getLocation().getY() <= voidHeight) {
            player.getPassengers().forEach(player::removePassenger);

            if (config.isLobbyLocationSet()) {
                player.teleport(config.getSafeLobbyLocation());

                String message = plugin.getMessagesConfig().getString("messages.no-void");
                if (message != null) {
                    player.sendMessage(ColorUtil.getInstance().translateColor(message));
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (config.isDisableHunger()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        if (config.isDisableChat()) {
            event.setCancelled(true);
            String message = plugin.getMessagesConfig().getString("messages.disable-chat");
            if (message != null) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        if (config.isDisableDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        if (itemsManager.isLobbyItem(item)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursor = event.getCursor();
        if (itemsManager.isLobbyItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getType() == InventoryType.CRAFTING ||
                event.getInventory().getType() == InventoryType.WORKBENCH) {

            if (event.getHotbarButton() != -1) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (itemsManager.isLobbyItem(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (itemsManager.isLobbyItem(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (config.isAdminBypass() && player.hasPermission("hub.admin")) {
            return;
        }

        if (config.isDisablePickup()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (!config.isDisableMove()) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        String itemId = itemsManager.getLobbyItemId(item);

        if (itemId != null) {
            ItemsManager.LobbyItem lobbyItem = itemsManager.getLobbyItem(itemId);
            if (lobbyItem != null && event.getNewSlot() != lobbyItem.getSlot()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() != null && event.getTo().getY() < -64) {
            if (config.isLobbyLocationSet()) {
                event.getPlayer().teleport(config.getSafeLobbyLocation());
                String message = plugin.getMessagesConfig().getString("messages.no-void");
                if (message != null) {
                    event.getPlayer().sendMessage(ColorUtil.getInstance().translateColor(message));
                }
            }
        }
    }

    private void startActionBar(Player player) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    actionBarTasks.remove(player);
                    return;
                }

                String actionBarMessage = plugin.getMessagesConfig().getString("messages.action-bar");
                if (actionBarMessage == null || actionBarMessage.isEmpty()) {
                    plugin.getLogger().warning("Сообщение для Action Bar не найдено или пустое.");
                }

                String message = actionBarMessage
                        .replace("%player%", player.getName())
                        .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

                player.sendActionBar(ColorUtil.getInstance().translateColor(message));
            }
        }.runTaskTimer(plugin, 0L, 20);

        actionBarTasks.put(player, task);
    }

    public void stopAllActionBars() {
        actionBarTasks.values().forEach(BukkitTask::cancel);
        actionBarTasks.clear();
    }

    public void restartAllActionBars() {
        if (plugin.getConfig().getBoolean("action-bar.enabled")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                startActionBar(player);
            }
        }
    }
}