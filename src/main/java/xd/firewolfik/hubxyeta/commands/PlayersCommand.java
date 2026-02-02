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

public class PlayersCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public PlayersCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length == 0) {
            String usageMessage = plugin.getMessagesConfig().getString("messages.players-usage");
            if (usageMessage != null && !usageMessage.isEmpty()) {
                player.sendMessage(ColorUtil.getInstance().translateColor(usageMessage));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            togglePlayerVisibility(player);
            return true;
        }

        return true;
    }

    private void togglePlayerVisibility(Player player) {
        boolean currentlyHidden = plugin.getDatabaseManager().isHidePlayersEnabled(player.getUniqueId());

        if (currentlyHidden) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player) {
                    player.showPlayer(plugin, online);
                }
            }

            plugin.getDatabaseManager().setHidePlayersEnabled(player.getUniqueId(), false);

            String message = plugin.getMessagesConfig().getString("messages.players-shown");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            }


        } else {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player) {
                    player.hidePlayer(plugin, online);
                }
            }

            plugin.getDatabaseManager().setHidePlayersEnabled(player.getUniqueId(), true);

            String message = plugin.getMessagesConfig().getString("messages.players-hidden");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
            }

        }
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