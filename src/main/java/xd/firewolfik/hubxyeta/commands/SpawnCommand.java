package xd.firewolfik.hubxyeta.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.Collections;
import java.util.List;

public class SpawnCommand extends BaseCommand {

    public SpawnCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("hub.admin")) {
            return teleportOther(sender, args[0]);
        }

        if (!requirePlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        if (!ensureLobbySet(player)) {
            return true;
        }

        player.teleport(plugin.getConfigManager().getSafeLobbyLocation());
        plugin.getMessageService().send(player, "messages.spawn");
        return true;
    }

    private boolean teleportOther(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.getMessageService().send(sender, "messages.player-not-found", "%player%", targetName);
            return true;
        }

        if (!ensureLobbySet(sender)) {
            return true;
        }

        target.teleport(plugin.getConfigManager().getSafeLobbyLocation());
        plugin.getMessageService().send(target, "messages.spawn-teleported");
        plugin.getMessageService().send(sender, "messages.spawn-teleported-admin", "%player%", target.getName());
        return true;
    }

    private boolean ensureLobbySet(CommandSender sender) {
        if (plugin.getConfigManager().isLobbyLocationSet()) {
            return true;
        }
        plugin.getMessageService().send(sender, "messages.no-spawn");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("hub.admin")) {
            return filterPrefix(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                    args[0]
            );
        }
        return Collections.emptyList();
    }
}
