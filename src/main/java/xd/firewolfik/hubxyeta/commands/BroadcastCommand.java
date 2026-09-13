package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;

import java.util.Collections;
import java.util.List;

public class BroadcastCommand extends BaseCommand {

    public BroadcastCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            boolean current = plugin.getDatabaseManager().isBroadcastsEnabled(player.getUniqueId());
            boolean next = !current;
            plugin.getDatabaseManager().setBroadcastsEnabled(player.getUniqueId(), next);

            String key = next ? "messages.broadcasts-enabled" : "messages.broadcasts-disabled";
            plugin.getMessageService().send(player, key);
            return true;
        }

        String usage = plugin.getMessageService().getRaw("messages.broadcasts-usage");
        if (usage != null && !usage.isEmpty()) {
            plugin.getMessageService().send(player, "messages.broadcasts-usage");
        } else {
            player.sendMessage(ComponentFormatter.format("&cИспользование: &e/broadcast [toggle]"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("toggle"), args[0]);
        }
        return Collections.emptyList();
    }
}
