package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.util.List;

public class LinksCommand implements CommandExecutor {

    private final Main plugin;
    private final ColorUtil colorUtil;

    public LinksCommand(Main plugin) {
        this.plugin = plugin;
        this.colorUtil = ColorUtil.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String onlyPlayers = plugin.getMessagesConfig().getString("messages.only-players", "&cТолько для игроков.");
            sender.sendMessage(colorUtil.translateColor(onlyPlayers));
            return true;
        }

        Player player = (Player) sender;

        List<String> links = plugin.getMessagesConfig().getStringList("messages.links-msg");

        if (links.isEmpty()) {
            player.sendMessage(colorUtil.translateColor("&cТекст не настроен. Проверьте messages.yml."));
            return true;
        }

        for (String line : links) {
            player.sendMessage(colorUtil.translateColor(line));
        }

        return true;
    }
}