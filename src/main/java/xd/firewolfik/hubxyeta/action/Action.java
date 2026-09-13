package xd.firewolfik.hubxyeta.action;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface Action {
    void execute(Player player, String argument);
}
