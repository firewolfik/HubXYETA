package xd.firewolfik.hubxyeta.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderUtil {

    private static final String PAPI = "PlaceholderAPI";

    private final Main plugin;
    private boolean papiErrorLogged;

    public PlaceholderUtil(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isPlaceholderApiEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(PAPI);
    }

    public String apply(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = applyBuiltIn(player != null ? player.getName() : null, text);

        if (player != null && isPlaceholderApiEnabled()) {
            result = applyPlaceholderApi(player, result);
        }

        return result;
    }

    public List<String> apply(Player player, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines != null ? lines : new ArrayList<>();
        }

        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(apply(player, line));
        }
        return result;
    }

    public String applyForSender(CommandSender sender, String text) {
        if (sender instanceof Player player) {
            return apply(player, text);
        }

        if (text == null || text.isEmpty()) {
            return text;
        }

        return applyBuiltIn(sender != null ? sender.getName() : null, text);
    }

    private String applyBuiltIn(String playerName, String text) {
        String result = text;

        if (playerName != null) {
            result = result.replace("%player%", playerName);
        }

        return result
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%prfx%", getPrefix())
                .replace("%NL%", "\n");
    }

    private String applyPlaceholderApi(Player player, String text) {
        try {
            String result = PapiHook.setPlaceholders(player, text);
            return result != null ? result : text;
        } catch (Throwable t) {
            if (!papiErrorLogged) {
                papiErrorLogged = true;
                plugin.getLogger().warning("Ошибка обработки плейсхолдеров: " + t);
            }
            return text;
        }
    }

    private String getPrefix() {
        FileConfiguration messages = plugin.getMessagesConfig();
        if (messages == null) {
            return "";
        }
        return messages.getString("messages.prefix", "");
    }
}
