package xd.firewolfik.hubxyeta.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;

import java.util.Collections;
import java.util.List;

public class HubCommand extends BaseCommand {

    private static final List<String> SUB_COMMANDS = List.of("setspawn", "reload", "items");
    private static final List<String> ITEM_ACTIONS = List.of("give", "take");
    private static final List<String> HOTBAR_SLOTS = List.of("0", "1", "2", "3", "4", "5", "6", "7", "8");

    public HubCommand(Main plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "setspawn" -> handleSetSpawn(sender);
            case "reload" -> handleReload(sender);
            case "items" -> handleItems(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!requirePlayer(sender) || !requirePermission(sender, "hub.admin")) {
            return true;
        }

        Player player = (Player) sender;
        plugin.getConfigManager().saveLobbyLocation(player.getLocation());
        plugin.getMessageService().send(player, "messages.spawn-set");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!requirePermission(sender, "hub.admin")) {
            return true;
        }

        try {
            long startTime = System.currentTimeMillis();
            plugin.getMessageService().send(sender, "messages.reload-start");

            plugin.reloadPlugin();

            long duration = System.currentTimeMillis() - startTime;
            plugin.getMessageService().send(sender, "messages.reload-success");
            plugin.getMessageService().send(sender, "messages.reload-time", "%time%", String.valueOf(duration));
            plugin.getMessageService().send(sender, "messages.reload-items", "%items%",
                    String.valueOf(plugin.getItemsManager().getAllItems().size()));
            plugin.getMessageService().send(sender, "messages.reload-broadcasts", "%broadcasts%",
                    String.valueOf(plugin.getBroadcastManager().getBroadcasts().size()));

        } catch (Exception e) {
            plugin.getMessageService().send(sender, "messages.reload-error", "%error%",
                    e.getMessage() != null ? e.getMessage() : "Unknown");
            plugin.getLogger().severe("Ошибка перезагрузки конфигураций: " + e.getMessage());
        }

        return true;
    }

    private boolean handleItems(CommandSender sender, String[] args) {
        if (!requirePermission(sender, "hub.admin")) {
            return true;
        }

        if (args.length < 3) {
            sendItemsUsage(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            plugin.getMessageService().send(sender, "messages.player-not-found", "%player%", targetName);
            return true;
        }

        return switch (action) {
            case "give" -> handleGiveItem(sender, target, args);
            case "take" -> handleTakeItem(sender, target, args);
            default -> {
                sendItemsUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleGiveItem(CommandSender sender, Player target, String[] args) {
        if (args.length < 4) {
            sendItemsUsage(sender);
            return true;
        }

        String itemId = args[3];
        if (!plugin.getItemsManager().getAllItems().containsKey(itemId)) {
            sender.sendMessage(ComponentFormatter.format("&cПредмет " + itemId + " не найден!"));
            return true;
        }

        if (args.length >= 5) {
            Integer slot = parseSlot(args[4]);
            if (slot == null) {
                sendItemsUsage(sender);
                return true;
            }
            plugin.getItemsManager().giveItemToSlot(target, itemId, slot);
        } else {
            plugin.getItemsManager().giveItem(target, itemId);
        }
        return true;
    }

    private boolean handleTakeItem(CommandSender sender, Player target, String[] args) {
        if (args.length < 4) {
            sendItemsUsage(sender);
            return true;
        }

        Integer slot = parseSlot(args[3]);
        if (slot == null) {
            sendItemsUsage(sender);
            return true;
        }

        if (target.getInventory().getItem(slot) != null) {
            target.getInventory().setItem(slot, null);
        }
        return true;
    }

    private Integer parseSlot(String text) {
        try {
            int slot = Integer.parseInt(text);
            return (slot >= 0 && slot <= 35) ? slot : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendItemsUsage(CommandSender sender) {
        plugin.getMessageService().send(sender, "messages.items-usage-header");
        plugin.getMessageService().send(sender, "messages.items-usage-give");
        plugin.getMessageService().send(sender, "messages.items-usage-take");
    }

    private void sendHelp(CommandSender sender) {
        if (sender.hasPermission("hub.admin")) {
            plugin.getMessageService().send(sender, "messages.help-header");
            plugin.getMessageService().send(sender, "messages.help-setspawn");
            plugin.getMessageService().send(sender, "messages.help-reload");
            plugin.getMessageService().send(sender, "messages.help-items");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hub.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterPrefix(SUB_COMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("items")) {
            return filterPrefix(ITEM_ACTIONS, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("items")) {
            return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("items")) {
            if (args[1].equalsIgnoreCase("give")) {
                return filterPrefix(plugin.getItemsManager().getAllItems().keySet(), args[3]);
            }
            if (args[1].equalsIgnoreCase("take")) {
                return filterPrefix(HOTBAR_SLOTS, args[3]);
            }
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("items") && args[1].equalsIgnoreCase("give")) {
            return filterPrefix(HOTBAR_SLOTS, args[4]);
        }

        return Collections.emptyList();
    }
}
