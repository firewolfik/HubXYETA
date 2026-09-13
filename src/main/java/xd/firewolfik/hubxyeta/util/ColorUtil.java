package xd.firewolfik.hubxyeta.util;

import net.kyori.adventure.text.Component;

public final class ColorUtil {

    private static ColorUtil instance;

    private ColorUtil() {
    }

    public static ColorUtil getInstance() {
        if (instance == null) {
            instance = new ColorUtil();
        }
        return instance;
    }

    public String translateColor(String message) {
        return ComponentFormatter.toLegacy(message);
    }

    public Component component(String message) {
        return ComponentFormatter.format(message);
    }
}
