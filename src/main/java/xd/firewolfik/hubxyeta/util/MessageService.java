package xd.firewolfik.hubxyeta.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.io.File;
import java.util.List;

public class MessageService {

    private final Main plugin;
    private FileConfiguration config;

    public MessageService(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getRaw(String path) {
        return config.getString(path);
    }

    public String getRaw(String path, String def) {
        return config.getString(path, def);
    }

    public List<String> getRawList(String path) {
        return config.getStringList(path);
    }

    public Component getComponent(String path, CommandSender sender, String... replacements) {
        String raw = getRaw(path);
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        if (replacements != null && replacements.length >= 2) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                String target = replacements[i];
                String replacement = replacements[i + 1];
                if (target != null && replacement != null) {
                    raw = raw.replace(target, replacement);
                }
            }
        }

        String parsed = plugin.getPlaceholderUtil().applyForSender(sender, raw);
        return ComponentFormatter.format(parsed);
    }

    public void send(CommandSender sender, String path, String... replacements) {
        Component component = getComponent(path, sender, replacements);
        if (component != null && !component.equals(Component.empty())) {
            sender.sendMessage(component);
        }
    }

    public void sendList(CommandSender sender, String path) {
        List<String> lines = getRawList(path);
        if (lines == null || lines.isEmpty()) {
            return;
        }

        Player player = (sender instanceof Player p) ? p : null;
        for (String line : lines) {
            String parsed = player != null
                    ? plugin.getPlaceholderUtil().apply(player, line)
                    : plugin.getPlaceholderUtil().applyForSender(sender, line);
            sender.sendMessage(ComponentFormatter.format(parsed));
        }
    }
}
