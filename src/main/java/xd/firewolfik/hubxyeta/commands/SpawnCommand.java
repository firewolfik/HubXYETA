package xd.firewolfik.hubxyeta.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("hub.admin")) {
            Player target = Bukkit.getPlayerExact(args[0]);

            if (target == null) {
                String message = plugin.getMessagesConfig().getString("messages.player-not-found");
                if (message != null) {
                    message = message.replace("%player%", args[0]);
                    sender.sendMessage(ColorUtil.getInstance().translateColor(message));
                }
                return true;
            }

            if (!plugin.getConfigManager().isLobbyLocationSet()) {
                String message = plugin.getMessagesConfig().getString("messages.no-spawn");
                sender.sendMessage(ColorUtil.getInstance().translateColor(message));
                return true;
            }

            target.teleport(plugin.getConfigManager().getSafeLobbyLocation());

            String targetMessage = plugin.getMessagesConfig().getString("messages.spawn-teleported");
            if (targetMessage != null) {
                target.sendMessage(ColorUtil.getInstance().translateColor(targetMessage));
            }

            String adminMessage = plugin.getMessagesConfig().getString("messages.spawn-teleported-admin");
            if (adminMessage != null) {
                adminMessage = adminMessage.replace("%player%", target.getName());
                sender.sendMessage(ColorUtil.getInstance().translateColor(adminMessage));
            }

            return true;
        }

        if (!(sender instanceof Player player)) {
            String message = plugin.getMessagesConfig().getString("messages.only-players");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        if (!plugin.getConfigManager().isLobbyLocationSet()) {
            String message = plugin.getMessagesConfig().getString("messages.no-spawn");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        player.teleport(plugin.getConfigManager().getSafeLobbyLocation());
        String message = plugin.getMessagesConfig().getString("messages.spawn");
        sender.sendMessage(ColorUtil.getInstance().translateColor(message));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && sender.hasPermission("hub.admin")) {
            String input = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}