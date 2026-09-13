package xd.firewolfik.hubxyeta.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.Collections;
import java.util.List;

public class PlayersCommand extends BaseCommand {

    public PlayersCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!requirePlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getMessageService().send(player, "messages.players-usage");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            toggleVisibility(player);
            return true;
        }

        return true;
    }

    private void toggleVisibility(Player player) {
        boolean currentlyHidden = plugin.getDatabaseManager().isHidePlayersEnabled(player.getUniqueId());
        boolean nextState = !currentlyHidden;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                if (nextState) {
                    player.hidePlayer(plugin, online);
                } else {
                    player.showPlayer(plugin, online);
                }
            }
        }

        plugin.getDatabaseManager().setHidePlayersEnabled(player.getUniqueId(), nextState);
        String msgKey = nextState ? "messages.players-hidden" : "messages.players-shown";
        plugin.getMessageService().send(player, msgKey);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("toggle"), args[0]);
        }
        return Collections.emptyList();
    }
}
