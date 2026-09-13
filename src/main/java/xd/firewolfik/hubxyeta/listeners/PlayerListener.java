package xd.firewolfik.hubxyeta.listeners;

import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

public class PlayerListener extends PlayerConnectionListener {

    public PlayerListener(Main plugin) {
        super(plugin);
    }

    public boolean hasActionBar(Player player) {
        return player != null && Main.getPlugin(Main.class)
                .getActionBarManager().isRunning(player.getUniqueId());
    }

    public void pauseActionBar(Player player) {
        if (player != null) {
            Main.getPlugin(Main.class)
                    .getActionBarManager().pause(player.getUniqueId());
        }
    }

    public void resumeActionBar(Player player) {
        if (player != null) {
            Main.getPlugin(Main.class)
                    .getActionBarManager().resume(player.getUniqueId());
        }
    }

    public void stopAllActionBars() {
        Main.getPlugin(Main.class).getActionBarManager().stopAll();
    }

    public void restartAllActionBars() {
        Main.getPlugin(Main.class).getActionBarManager().restartAll();
    }
}
