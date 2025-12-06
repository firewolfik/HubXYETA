package xd.firewolfik.hubxyeta.util;

import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.logging.Level;

public class LoggerUtil {

    private final Main plugin;

    public LoggerUtil(Main plugin) {
        this.plugin = plugin;
    }
    public void info(String message) {
        plugin.getLogger().info(message);
    }
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }
    public void error(String message) {
        plugin.getLogger().severe(message);
    }
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}