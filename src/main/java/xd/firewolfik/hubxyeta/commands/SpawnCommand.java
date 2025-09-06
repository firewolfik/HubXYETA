package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

public class SpawnCommand implements CommandExecutor {

    private final Main plugin;

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
}