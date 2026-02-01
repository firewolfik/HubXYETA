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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HubCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public HubCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setspawn" -> {
                return handleSetSpawn(sender);
            }
            case "reload" -> {
                return handleReload(sender);
            }
            case "items" -> {
                return handleItems(sender, args);
            }
            default -> {
                sendHelpMessage(sender);
                return true;
            }
        }
    }

    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            String message = plugin.getMessagesConfig().getString("messages.only-players");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        if (!player.hasPermission("hub.admin")) {
            String message = plugin.getMessagesConfig().getString("messages.no-permission");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        plugin.getConfigManager().saveLobbyLocation(player.getLocation());
        String message = plugin.getMessagesConfig().getString("messages.spawn-set");
        sender.sendMessage(ColorUtil.getInstance().translateColor(message));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("hub.admin")) {
            String message = plugin.getMessagesConfig().getString("messages.no-permission");
            sender.sendMessage(ColorUtil.getInstance().translateColor(message));
            return true;
        }

        try {
            long startTime = System.currentTimeMillis();

            String startMessage = plugin.getMessagesConfig().getString("messages.reload-start");
            sender.sendMessage(ColorUtil.getInstance().translateColor(startMessage));

            plugin.reloadPlugin();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String successMessage = plugin.getMessagesConfig().getString("messages.reload-success");
            sender.sendMessage(ColorUtil.getInstance().translateColor(successMessage));

            String timeMessage = plugin.getMessagesConfig().getString("messages.reload-time");
            if (timeMessage != null) {
                timeMessage = timeMessage.replace("%time%", String.valueOf(duration));
                sender.sendMessage(ColorUtil.getInstance().translateColor(timeMessage));
            }

            String itemsMessage = plugin.getMessagesConfig().getString("messages.reload-items");
            if (itemsMessage != null) {
                itemsMessage = itemsMessage.replace("%items%", String.valueOf(plugin.getItemsManager().getAllItems().size()));
                sender.sendMessage(ColorUtil.getInstance().translateColor(itemsMessage));
            }

            String broadcastsMessage = plugin.getMessagesConfig().getString("messages.reload-broadcasts");
            if (broadcastsMessage != null) {
                broadcastsMessage = broadcastsMessage.replace("%broadcasts%", String.valueOf(plugin.getBroadcastManager().getBroadcasts().size()));
                sender.sendMessage(ColorUtil.getInstance().translateColor(broadcastsMessage));
            }

        } catch (Exception e) {
            String errorMessage = plugin.getMessagesConfig().getString("messages.reload-error");
            if (errorMessage != null) {
                errorMessage = errorMessage.replace("%error%", e.getMessage());
                sender.sendMessage(ColorUtil.getInstance().translateColor(errorMessage));
            } else {
                sender.sendMessage("§cОшибка при перезагрузке: " + e.getMessage());
            }
            plugin.getLogger().severe("Ошибка перезагрузки конфигураций: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleItems(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hub.admin")) {
            return true;
        }

        if (args.length < 3) {
            return true;
        }

        String action = args[1].toLowerCase();
        String targetName = args[2];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            return true;
        }

        switch (action) {
            case "give" -> {
                if (args.length < 4) {
                    return true;
                }

                String itemId = args[3];
                if (!plugin.getItemsManager().getAllItems().containsKey(itemId)) {
                    return true;
                }

                if (args.length >= 5) {
                    int customSlot;
                    try {
                        customSlot = Integer.parseInt(args[4]);
                        if (customSlot < 0 || customSlot > 35) {
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        return true;
                    }

                    plugin.getItemsManager().giveItemToSlot(target, itemId, customSlot);
                } else {
                    plugin.getItemsManager().giveItem(target, itemId);
                }
            }

            case "take" -> {
                if (args.length < 4) {
                    return true;
                }

                int slot;
                try {
                    slot = Integer.parseInt(args[3]);
                    if (slot < 0 || slot > 35) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    return true;
                }

                if (target.getInventory().getItem(slot) == null) {
                    return true;
                }

                target.getInventory().setItem(slot, null);
            }

            default -> {
                return true;
            }
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        if (sender.hasPermission("hub.admin")) {
            String helpHeaderMessage = plugin.getMessagesConfig().getString("messages.help-header");
            if (helpHeaderMessage != null)
                sender.sendMessage(ColorUtil.getInstance().translateColor(helpHeaderMessage));

            String helpSetspawn = plugin.getMessagesConfig().getString("messages.help-setspawn");
            if (helpSetspawn != null) {
                sender.sendMessage(ColorUtil.getInstance().translateColor(helpSetspawn));
            }

            String helpReload = plugin.getMessagesConfig().getString("messages.help-reload");
            if (helpReload != null) {
                sender.sendMessage(ColorUtil.getInstance().translateColor(helpReload));
            }

            String helpItems = plugin.getMessagesConfig().getString("messages.help-items");
            if (helpItems != null) {
                sender.sendMessage(ColorUtil.getInstance().translateColor(helpItems));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();

            if (sender.hasPermission("hub.admin")) {
                subCommands.addAll(Arrays.asList("setspawn", "reload", "items"));
            }

            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("items")) {
            if (sender.hasPermission("hub.admin")) {
                List<String> actions = Arrays.asList("give", "take");
                String input = args[1].toLowerCase();
                for (String action : actions) {
                    if (action.startsWith(input)) {
                        completions.add(action);
                    }
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("items")) {
            if (sender.hasPermission("hub.admin")) {
                String input = args[2].toLowerCase();
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("items")) {
            if (sender.hasPermission("hub.admin")) {
                if (args[1].equalsIgnoreCase("give")) {
                    String input = args[3].toLowerCase();
                    completions.addAll(plugin.getItemsManager().getAllItems().keySet().stream()
                            .filter(itemId -> itemId.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                } else if (args[1].equalsIgnoreCase("take")) {
                    completions.addAll(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8"));
                }
            }
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("items") && args[1].equalsIgnoreCase("give")) {
            if (sender.hasPermission("hub.admin")) {
                completions.addAll(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8"));
            }
        }

        return completions;
    }
}