package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.util.ArrayList;
import java.util.Arrays;
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

        if (args.length == 0) {
            String message = plugin.getMessagesConfig().getString("messages.broadcast-usage");
            if (message != null) {
                sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            }
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "on" -> {
                plugin.getDatabaseManager().setBroadcastsEnabled(player.getUniqueId(), true);
                String message = plugin.getMessagesConfig().getString("messages.broadcast-enabled");
                if (message != null) {
                    player.sendMessage(ColorUtil.getInstance().translateColor(message));
                }
            }
            case "off" -> {
                plugin.getDatabaseManager().setBroadcastsEnabled(player.getUniqueId(), false);
                String message = plugin.getMessagesConfig().getString("messages.broadcast-disabled");
                if (message != null) {
                    player.sendMessage(ColorUtil.getInstance().translateColor(message));
                }
            }
            default -> {
                String message = plugin.getMessagesConfig().getString("messages.broadcast-usage");
                if (message != null) {
                    player.sendMessage(ColorUtil.getInstance().translateColor(message));
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = Arrays.asList("on", "off");
            String input = args[0].toLowerCase();

            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}