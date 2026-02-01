package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public BroadcastCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            String message = plugin.getMessagesConfig().getString("messages.only-players");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("toggle"))) {
            boolean currentStatus = plugin.getDatabaseManager().isBroadcastsEnabled(player.getUniqueId());

            boolean newStatus = !currentStatus;
            plugin.getDatabaseManager().setBroadcastsEnabled(player.getUniqueId(), newStatus);

            String messageKey = newStatus ? "messages.broadcasts-enabled" : "messages.broadcasts-disabled";
            String message = plugin.getMessagesConfig().getString(messageKey);
            if (message != null) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            }
        } else {
            String message = plugin.getMessagesConfig().getString("messages.broadcasts-usage");
            if (message != null) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            } else {
                player.sendMessage(ColorUtil.getInstance().translateColor("&cИспользование: &e/broadcast [toggle]"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("toggle".startsWith(input)) {
                completions.add("toggle");
            }
        }
        return completions;
    }
}