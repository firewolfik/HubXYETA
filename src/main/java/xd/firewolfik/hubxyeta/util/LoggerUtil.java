package xd.firewolfik.hubxyeta.util;

import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.logging.Level;

public class LoggerUtil {

    private final Main plugin;

    public LoggerUtil(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Логирует информационное сообщение
     */
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Логирует предупреждение
     */
    public void warning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Логирует ошибку
     */
    public void error(String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * Логирует ошибку с исключением
     */
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }
}