package xd.firewolfik.hubxyeta.listeners;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

import java.util.HashMap;
import java.util.Map;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final ConfigManager config;
    private final ItemsManager itemsManager;

    // Храним таски для каждого игрока
    private final Map<Player, BukkitTask> actionBarTasks = new HashMap<>();

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.itemsManager = plugin.getItemsManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Скрытие сообщения о входе
        if (config.isHideStream()) {
            event.setJoinMessage(null);
        }

        // Скрытие игроков
        if (config.isHidePlayer()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> hidePlayersForPlayer(player));
        }

        // Телепорт в лобби
        if (config.isLobbyLocationSet()) {
            player.teleport(config.getSafeLobbyLocation());
        } else {
            plugin.getLogger().warning("Игрок " + player.getName() + " зашел, но спавн лобби не установлен!");
        }

        // Настройка игрока
        config.setupPlayer(player);

        // Выдача предметов
        itemsManager.giveAllItems(player);

        // Очистка чата
        if (config.isClearChat()) {
            clearPlayerChat(player);
        }

        // Сообщения при входе
        sendJoinMessages(player);

        // Title при входе
        sendJoinTitle(player);

        // Настройка мира
        config.setupWorld(player.getWorld());

        // Action Bar - запускаем автоматически для всех игроков
        if (config.isActionBarEnabled()) {
            startActionBar(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Скрытие сообщения о выходе
        if (config.isHideStream()) {
            event.setQuitMessage(null);
        }

        // Очистка предметов
        if (config.isClearItems()) {
            player.getInventory().clear();
        }

        // Остановка Action Bar таски для этого игрока
        stopActionBar(player);
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

        // Получаем высоту войда из конфига (по умолчанию -64)
        double voidHeight = -64.0;

        if (event.getTo() != null && player.getLocation().getY() <= voidHeight) {
            // Убираем всех пассажиров игрока (если есть)
            player.getPassengers().forEach(player::removePassenger);

            // Телепортируем в лобби
            if (config.isLobbyLocationSet()) {
                player.teleport(config.getSafeLobbyLocation());

                // Отправляем сообщение о войде с цветами
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
        if (config.isDisableChat()) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String message = plugin.getMessagesConfig().getString("messages.disable-chat");
            if (message != null) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (config.isDisableDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        // Проверяем, является ли предмет предметом лобби
        if (itemsManager.isLobbyItem(item)) {
            event.setCancelled(true);
            return;
        }

        // Проверяем предмет в курсоре
        ItemStack cursor = event.getCursor();
        if (itemsManager.isLobbyItem(cursor)) {
            event.setCancelled(true);
            return;
        }

        // Запрещаем перенос лобби предметов в крафт
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
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && config.isDisablePickup()) {
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
        // Останавливаем предыдущую таску если она есть
        stopActionBar(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    actionBarTasks.remove(player);
                    return;
                }

                String message = plugin.getMessagesConfig().getString("messages.action-bar");
                if (message != null && !message.isEmpty()) {
                    // Заменяем плейсхолдеры
                    message = message.replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()));
                    message = ColorUtil.getInstance().translateColor(message);

                    // Отправляем action bar сообщение
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(message));
                }
            }
        }.runTaskTimerAsynchronously(plugin,0L, 5L);

        // Сохраняем таску для этого игрока
        actionBarTasks.put(player, task);
    }

    private void stopActionBar(Player player) {
        BukkitTask task = actionBarTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAllActionBars() {
        actionBarTasks.values().forEach(BukkitTask::cancel);
        actionBarTasks.clear();
    }
    public void restartAllActionBars() {
        if (config.isActionBarEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                startActionBar(player);
            }
        }
    }
}