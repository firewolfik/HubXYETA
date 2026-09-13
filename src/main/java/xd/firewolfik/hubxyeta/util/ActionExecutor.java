package xd.firewolfik.hubxyeta.util;

import org.bukkit.entity.Player;
import xd.firewolfik.hubxyeta.Main;

import java.util.List;

public class ActionExecutor {

    private final Main plugin;

    public ActionExecutor(Main plugin) {
        this.plugin = plugin;
    }

    public void executeActions(Player player, List<String> actions) {
        plugin.getActionService().execute(player, actions);
    }

    public void executeAction(Player player, String action) {
        plugin.getActionService().execute(player, action);
    }
}
