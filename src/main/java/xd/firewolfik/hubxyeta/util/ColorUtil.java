package xd.firewolfik.hubxyeta.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtil {
    private static ColorUtil instance;
    private static final LegacyComponentSerializer INPUT_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();
    private static final LegacyComponentSerializer OUTPUT_SERIALIZER = LegacyComponentSerializer.legacySection();

    private ColorUtil() {
    }

    public static ColorUtil getInstance() {
        if (instance == null) {
            instance = new ColorUtil();
        }
        return instance;
    }

    public String translateColor(String message) {
        return OUTPUT_SERIALIZER.serialize(component(message));
    }

    public Component component(String message) {
        return INPUT_SERIALIZER.deserialize(message == null ? "" : message);
    }
}
