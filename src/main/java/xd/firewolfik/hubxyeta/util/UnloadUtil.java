package xd.firewolfik.hubxyeta.util;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

public class UnloadUtil {

    private final Main plugin;

    public UnloadUtil(Main plugin) {
        this.plugin = plugin;
    }


    public void unloadPlayerConfig() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                player.getInventory().clear();

                player.getActivePotionEffects().forEach(effect ->
                        player.removePotionEffect(effect.getType())
                );

                player.setHealth(20.0);
                player.setFoodLevel(20);
                player.setSaturation(20.0f);

                player.setGameMode(GameMode.SURVIVAL);

                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other != player) {
                        player.showPlayer(plugin, other);
                        other.showPlayer(plugin, player);
                    }
                }

                plugin.getLogger().info("Состояние игрока " + player.getName() + " сброшено");

            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при сбросе состояния игрока " + player.getName() + ": " + e.getMessage());
            }
        }
    }
}