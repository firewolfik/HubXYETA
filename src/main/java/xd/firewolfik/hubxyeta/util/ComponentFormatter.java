package xd.firewolfik.hubxyeta.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ComponentFormatter {

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ComponentFormatter() {
    }

    public static Component format(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return AMPERSAND_SERIALIZER.deserialize(text);
    }

    public static Component formatMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(text);
    }

    public static String toLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(format(text));
    }
}
