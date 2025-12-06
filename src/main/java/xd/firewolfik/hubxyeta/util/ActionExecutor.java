package xd.firewolfik.hubxyeta.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.List;

public class ActionExecutor {

    private final Main plugin;

    public ActionExecutor(Main plugin) {
        this.plugin = plugin;
    }

    public void executeActions(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        for (String action : actions) {
            executeAction(player, action);
        }
    }

    public void executeAction(Player player, String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        String parsedAction = action.replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%NL%", "\n");

        if (parsedAction.startsWith("[MSG] ")) {
            String message = parsedAction.substring(6);
            player.sendMessage(ColorUtil.getInstance().translateColor(message));

        } else if (parsedAction.startsWith("[MM] ")) {
            String message = parsedAction.substring(5);
            player.sendMessage(ColorUtil.getInstance().translateColor(message));

        } else if (parsedAction.startsWith("[BROADCAST] ")) {
            String message = parsedAction.substring(12);
            Bukkit.broadcastMessage(ColorUtil.getInstance().translateColor(message));

        } else if (parsedAction.startsWith("[TITLE] ")) {
            String titleData = parsedAction.substring(8);
            String[] parts = titleData.split(";");
            String title = parts.length > 0 ? ColorUtil.getInstance().translateColor(parts[0]) : "";
            String subtitle = parts.length > 1 ? ColorUtil.getInstance().translateColor(parts[1]) : "";
            player.sendTitle(title, subtitle, 10, 70, 20);

        } else if (parsedAction.startsWith("[ACTIONBAR] ")) {
            String message = parsedAction.substring(12);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ColorUtil.getInstance().translateColor(message)));

        } else if (parsedAction.startsWith("[PLAYER] ")) {
            String command = parsedAction.substring(9);
            Bukkit.dispatchCommand(player, command);

        } else if (parsedAction.startsWith("[CONSOLE] ")) {
            String command = parsedAction.substring(10);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        } else if (parsedAction.startsWith("[CONNECT] ")) {
            String server = parsedAction.substring(10);
            plugin.getLogger().info("Attempting to connect " + player.getName() + " to server: " + server);

        } else if (parsedAction.equals("[TELEPORT_TO_SPAWN]")) {
            if (plugin.getConfigManager() != null && plugin.getConfigManager().isLobbyLocationSet()) {
                player.teleport(plugin.getConfigManager().getSafeLobbyLocation());
            }

        } else if (parsedAction.equals("[HIDE_PLAYERS]")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player) {
                    player.hidePlayer(plugin, online);
                }
            }

        } else if (parsedAction.equals("[SHOW_PLAYERS]")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player) {
                    player.showPlayer(plugin, online);
                }
            }

        } else if (parsedAction.startsWith("[SOUND] ")) {
            String soundData = parsedAction.substring(8);
            String[] parts = soundData.split(";");
            if (parts.length >= 3) {
                try {
                    Sound sound = Sound.valueOf(parts[0].toUpperCase());
                    float volume = Float.parseFloat(parts[1]);
                    float pitch = Float.parseFloat(parts[2]);
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (Exception e) {
                    plugin.getLogger().warning("Неверный формат звука: " + soundData);
                }
            }
        }
    }
}