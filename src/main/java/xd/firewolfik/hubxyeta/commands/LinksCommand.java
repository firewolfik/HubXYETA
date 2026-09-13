package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;

import java.util.List;

public class LinksCommand extends BaseCommand {

    public LinksCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        List<String> links = plugin.getMessageService().getRawList("messages.links-msg");
        if (links == null || links.isEmpty()) {
            player.sendMessage(ComponentFormatter.format("&cТекст не настроен. Проверьте messages.yml."));
            return true;
        }

        for (String line : links) {
            String parsed = plugin.getPlaceholderUtil().apply(player, line);
            player.sendMessage(ComponentFormatter.format(parsed));
        }

        return true;
    }
}
