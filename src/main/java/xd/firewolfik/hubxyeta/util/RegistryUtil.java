package xd.firewolfik.hubxyeta.util;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Locale;

public final class RegistryUtil {

    private RegistryUtil() {
    }

    public static <T extends Keyed> T find(Registry<T> registry, String configuredName) {
        if (configuredName == null || configuredName.isBlank()) {
            return null;
        }

        String normalized = configuredName.toLowerCase(Locale.ROOT);
        NamespacedKey directKey = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        T directMatch = directKey == null ? null : registry.get(directKey);
        if (directMatch != null) {
            return directMatch;
        }

        String legacyName = normalized.replace(':', '_');
        return registry.stream()
                .filter(value -> registry.getKeyOrThrow(value).getKey().replace('.', '_').equals(legacyName))
                .findFirst()
                .orElse(null);
    }
}
