package xd.firewolfik.hubxyeta.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ComponentFormatter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarManager {

    private final Main plugin;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pausedStates = new ConcurrentHashMap<>();

    public ActionBarManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        UUID uuid = player.getUniqueId();
        stop(uuid);

        if (!plugin.getConfigManager().isActionBarEnabled()) {
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    activeTasks.remove(uuid);
                    pausedStates.remove(uuid);
                    return;
                }

                if (plugin.getConfigManager().isPixelBattleWorld(p.getWorld())) {
                    return;
                }

                if (pausedStates.getOrDefault(uuid, false)) {
                    return;
                }

                String raw = plugin.getMessageService().getRaw("messages.action-bar");
                if (raw == null || raw.isEmpty()) {
                    return;
                }

                String formatted = plugin.getPlaceholderUtil().apply(p, raw);
                p.sendActionBar(ComponentFormatter.format(formatted));
            }
        }.runTaskTimer(plugin, 0L, 20L);

        activeTasks.put(uuid, task);
        pausedStates.put(uuid, false);
    }

    public void stop(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        pausedStates.remove(uuid);
    }

    public boolean isRunning(UUID uuid) {
        BukkitTask task = activeTasks.get(uuid);
        return task != null && !task.isCancelled();
    }

    public void pause(UUID uuid) {
        pausedStates.put(uuid, true);
    }

    public void resume(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && isRunning(uuid)) {
            pausedStates.put(uuid, false);
        }
    }

    public void stopAll() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        pausedStates.clear();
    }

    public void restartAll() {
        if (plugin.getConfigManager().isActionBarEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isRunning(player.getUniqueId())) {
                    start(player);
                }
            }
        }
    }
}
