package xd.firewolfik.hubxyeta.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.managers.DatabaseManager.PlayerProfile;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;

import java.time.Duration;

public class PlayerConnectionListener implements Listener {

    private final Main plugin;
    private final ConfigManager config;

    public PlayerConnectionListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (config.isHideStream()) {
            event.joinMessage(null);
        }

        PlayerProfile profile = plugin.getDatabaseManager().loadOrRegisterPlayer(player.getUniqueId(), player.getName());

        if (config.isFirstJoinMessageEnabled() && profile != null && profile.isNew()) {
            int playerNumber = profile.getPlayerNumber();
            if (playerNumber > 0) {
                String firstJoinRaw = plugin.getMessageService().getRaw("messages.first-join-msg");
                if (firstJoinRaw != null && !firstJoinRaw.isEmpty()) {
                    String formatted = plugin.getPlaceholderUtil().apply(player,
                            firstJoinRaw.replace("%number%", String.valueOf(playerNumber)));
                    Bukkit.broadcast(ComponentFormatter.format(formatted));
                }
            }
        }

        if (config.isHidePlayer()) {
            hideAllPlayersGlobally(player);
        } else {
            if (profile != null && profile.isHidePlayers()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(player)) {
                            player.hidePlayer(plugin, online);
                        }
                    }
                }, 5L);
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && plugin.getDatabaseManager().isHidePlayersEnabled(online.getUniqueId())) {
                    online.hidePlayer(plugin, player);
                }
            }
        }

        if (config.isLobbyLocationSet()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(config.getSafeLobbyLocation());
                }
            });
        } else {
            plugin.getLogger().warning("Игрок " + player.getName() + " зашел, но спавн лобби не установлен!");
        }

        config.setupPlayer(player);
        plugin.getItemsManager().giveAllItems(player);
        player.getInventory().setHeldItemSlot(config.getSelectedSlot());

        if (config.isClearChat()) {
            clearPlayerChat(player);
        }

        sendJoinMessages(player);
        sendJoinTitle(player);
        config.setupWorld(player.getWorld());

        if (config.isActionBarEnabled() && !config.isPixelBattleWorld(player.getWorld())) {
            plugin.getActionBarManager().start(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (config.isHideStream()) {
            event.quitMessage(null);
        }

        if (config.isClearItems()) {
            player.getInventory().clear();
        }

        plugin.getActionBarManager().stop(player.getUniqueId());
        plugin.getDatabaseManager().unloadPlayer(player.getUniqueId());
    }

    private void hideAllPlayersGlobally(Player newPlayer) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(newPlayer)) {
                online.hidePlayer(plugin, newPlayer);
                newPlayer.hidePlayer(plugin, online);
            }
        }
    }

    public void restorePlayerVisibility(Player player) {
        if (config.isHidePlayer()) {
            hideAllPlayersGlobally(player);
        }

        if (plugin.getDatabaseManager().isHidePlayersEnabled(player.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    player.hidePlayer(plugin, online);
                }
            }
        }
    }

    public void resetPlayerVisibility() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) {
                    player.showPlayer(plugin, other);
                }
            }
        }
    }

    private void clearPlayerChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage(Component.empty());
        }
    }

    private void sendJoinMessages(Player player) {
        for (String message : config.getJoinMessages()) {
            if (message != null && !message.isBlank()) {
                player.sendMessage(ComponentFormatter.format(plugin.getPlaceholderUtil().apply(player, message)));
            }
        }
    }

    private void sendJoinTitle(Player player) {
        String title = config.getJoinTitle();
        String subtitle = config.getJoinSubtitle();
        if (title.isEmpty() && subtitle.isEmpty()) {
            return;
        }

        player.showTitle(Title.title(
                ComponentFormatter.format(plugin.getPlaceholderUtil().apply(player, title)),
                ComponentFormatter.format(plugin.getPlaceholderUtil().apply(player, subtitle)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        ));
    }
}
