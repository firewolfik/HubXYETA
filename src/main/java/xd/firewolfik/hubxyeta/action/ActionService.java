package xd.firewolfik.hubxyeta.action;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;
import xd.firewolfik.hubxyeta.util.RegistryUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionService {

    private final Main plugin;
    private final Map<String, Action> actions = new HashMap<>();
    private final Map<String, Sound> soundCache = new ConcurrentHashMap<>();

    public ActionService(Main plugin) {
        this.plugin = plugin;
        registerDefaultActions();
    }

    public void register(String tag, Action action) {
        actions.put(tag.toUpperCase(), action);
    }

    private void registerDefaultActions() {
        register("[MSG]", (player, arg) ->
                player.sendMessage(ComponentFormatter.format(arg)));

        register("[MM]", (player, arg) ->
                player.sendMessage(ComponentFormatter.formatMiniMessage(arg)));

        register("[BROADCAST]", (player, arg) ->
                Bukkit.broadcast(ComponentFormatter.format(arg)));

        register("[TITLE]", (player, arg) -> {
            String[] parts = arg.split(";", 2);
            String title = parts.length > 0 ? parts[0] : "";
            String subtitle = parts.length > 1 ? parts[1] : "";
            player.showTitle(Title.title(
                    ComponentFormatter.format(title),
                    ComponentFormatter.format(subtitle),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
            ));
        });

        register("[ACTIONBAR]", (player, arg) -> {
            boolean wasRunning = plugin.getActionBarManager().isRunning(player.getUniqueId());
            if (wasRunning) {
                plugin.getActionBarManager().pause(player.getUniqueId());
            }

            player.sendActionBar(ComponentFormatter.format(arg));

            if (wasRunning) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        plugin.getActionBarManager().resume(player.getUniqueId()), 60L);
            }
        });

        register("[PLAYER]", (player, arg) ->
                Bukkit.dispatchCommand(player, arg));

        register("[CONSOLE]", (player, arg) ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), arg));

        register("[CONNECT]", (player, arg) -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(arg.trim());
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        });

        register("[TELEPORT_TO_SPAWN]", (player, arg) -> {
            if (plugin.getConfigManager().isLobbyLocationSet()) {
                player.teleport(plugin.getConfigManager().getSafeLobbyLocation());
            }
        });

        register("[HIDE_PLAYERS]", (player, arg) -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    player.hidePlayer(plugin, online);
                }
            }
            plugin.getDatabaseManager().setHidePlayersEnabled(player.getUniqueId(), true);
            plugin.getMessageService().send(player, "messages.players-hidden");
        });

        register("[SHOW_PLAYERS]", (player, arg) -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) {
                    player.showPlayer(plugin, online);
                }
            }
            plugin.getDatabaseManager().setHidePlayersEnabled(player.getUniqueId(), false);
            plugin.getMessageService().send(player, "messages.players-shown");
        });

        register("[SOUND]", (player, arg) -> {
            String[] parts = arg.split(";");
            if (parts.length >= 3) {
                try {
                    String soundName = parts[0].trim();
                    Sound sound = soundCache.computeIfAbsent(soundName, name ->
                            RegistryUtil.find(RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT), name)
                    );
                    if (sound != null) {
                        float volume = Float.parseFloat(parts[1].trim());
                        float pitch = Float.parseFloat(parts[2].trim());
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    } else {
                        plugin.getLogger().warning("Неизвестный звук: " + soundName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка воспроизведения звука: " + arg);
                }
            }
        });
    }

    public void execute(Player player, List<String> actionStrings) {
        if (actionStrings == null || actionStrings.isEmpty()) {
            return;
        }
        for (String actionStr : actionStrings) {
            execute(player, actionStr);
        }
    }

    public void execute(Player player, String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return;
        }

        String parsed = plugin.getPlaceholderUtil().apply(player, rawAction.trim());
        int closingBracket = parsed.indexOf(']');
        if (closingBracket == -1 || !parsed.startsWith("[")) {
            plugin.getLogger().warning("Некорректный синтаксис действия: " + rawAction);
            return;
        }

        String tag = parsed.substring(0, closingBracket + 1).toUpperCase();
        String argument = parsed.length() > closingBracket + 1
                ? parsed.substring(closingBracket + 1).trim()
                : "";

        Action action = actions.get(tag);
        if (action != null) {
            action.execute(player, argument);
        } else {
            plugin.getLogger().warning("Неизвестный тип действия: " + tag);
        }
    }
}
