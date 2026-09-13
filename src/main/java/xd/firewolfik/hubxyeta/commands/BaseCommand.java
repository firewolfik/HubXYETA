package xd.firewolfik.hubxyeta.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final Main plugin;

    protected BaseCommand(Main plugin) {
        this.plugin = plugin;
    }

    protected boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        plugin.getMessageService().send(sender, "messages.only-players");
        return false;
    }

    protected boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        plugin.getMessageService().send(sender, "messages.no-permission");
        return false;
    }

    protected List<String> filterPrefix(Collection<String> items, String prefix) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        String lower = prefix.toLowerCase();
        return items.stream()
                .filter(item -> item.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
