package xd.firewolfik.hubxyeta.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

final class PapiHook {

    private PapiHook() {
    }

    static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}