package xd.firewolfik.hubxyeta.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.config.ConfigManager;

public class PlayerProtectionListener implements Listener {

    private final Main plugin;
    private final ConfigManager config;

    public PlayerProtectionListener(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    private boolean canBypass(Player player) {
        return config.isAdminBypass() && player.hasPermission("hub.admin");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJumpVoid(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        double voidHeight = -64.0;

        if (event.getTo() != null && player.getLocation().getY() <= voidHeight) {
            player.getPassengers().forEach(player::removePassenger);

            if (config.isLobbyLocationSet()) {
                player.setFallDistance(0.0f);
                player.setVelocity(new Vector(0, 0, 0));
                player.teleport(config.getSafeLobbyLocation());
                plugin.getMessageService().send(player, "messages.no-void");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (config.isDisableDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (config.isDisableHunger()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (canBypass(player)) {
            return;
        }

        if (config.isDisableChat()) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "messages.disable-chat");
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (canBypass(event.getPlayer())) {
            return;
        }

        if (config.isDisableDrop()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (canBypass(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canBypass(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (canBypass(player)) {
            return;
        }

        if (config.isDisablePickup()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (config.isPixelBattleWorld(event.getPlayer().getWorld())) {
            return;
        }
        event.setCancelled(true);
    }
}
