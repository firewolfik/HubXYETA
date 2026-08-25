package xd.firewolfik.hubxyeta.managers;

import lombok.Getter;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;
import xd.firewolfik.hubxyeta.util.RegistryUtil;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class BroadcastManager {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?<![\\w@.])(?:https?://\\S+|(?:[a-z0-9-]+\\.)+(?:ru|su|com|net|org|io|gg|me|xyz|shop|store|online|site|top|fun|pro|club|vip|info|biz|cc|tv|co|dev|app|link|live)(?:/\\S*)?)");

    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile(
            "(?i)&#[0-9a-f]{6}|&[0-9a-fk-or]");

    private final Main plugin;
    private FileConfiguration broadcastsConfig;
    private List<Broadcast> broadcasts;
    private List<Broadcast> remainingBroadcasts;
    private BukkitTask broadcastTask;

    private boolean enabled;
    private int interval;

    public BroadcastManager(Main plugin) {
        this.plugin = plugin;
        this.broadcasts = new ArrayList<>();
        this.remainingBroadcasts = new ArrayList<>();
        loadBroadcastsConfig();
    }

    public void loadBroadcastsConfig() {
        saveResourceIfNotExists("broadcasts.yml");
        File broadcastsFile = new File(plugin.getDataFolder(), "broadcasts.yml");
        broadcastsConfig = YamlConfiguration.loadConfiguration(broadcastsFile);
        loadBroadcasts();
        startBroadcasting();
    }

    public void reloadBroadcasts() {
        stopBroadcasting();
        broadcasts.clear();
        remainingBroadcasts.clear();
        loadBroadcastsConfig();
        plugin.getLogger().info("Объявления перезагружены! Загружено: " + broadcasts.size());
    }

    private void loadBroadcasts() {
        broadcasts.clear();

        enabled = broadcastsConfig.getBoolean("settings.enabled", true);
        interval = broadcastsConfig.getInt("settings.interval", 60);
        if (interval <= 0) {
            plugin.getLogger().warning("Интервал объявлений должен быть больше 0, используется 60 секунд");
            interval = 60;
        }

        ConfigurationSection broadcastsSection = broadcastsConfig.getConfigurationSection("broadcasts");
        if (broadcastsSection == null) {
            plugin.getLogger().warning("Секция 'broadcasts' не найдена в broadcasts.yml!");
            return;
        }

        for (String broadcastId : broadcastsSection.getKeys(false)) {
            ConfigurationSection section = broadcastsSection.getConfigurationSection(broadcastId);
            if (section != null) {
                try {
                    Broadcast broadcast = createBroadcast(broadcastId, section);
                    if (broadcast != null) {
                        broadcasts.add(broadcast);
                        plugin.getLogger().info("Загружено объявление: " + broadcastId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Ошибка загрузки объявления " + broadcastId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        remainingBroadcasts.addAll(broadcasts);
        plugin.getLogger().info("Загружено объявлений: " + broadcasts.size());
    }

    private Broadcast createBroadcast(String id, ConfigurationSection section) {
        List<String> messages = section.getStringList("messages");
        if (messages.isEmpty()) {
            plugin.getLogger().warning("Объявление " + id + " не содержит сообщений!");
            return null;
        }

        BroadcastHover broadcastHover = null;
        if (section.contains("hover")) {
            ConfigurationSection hoverSection = section.getConfigurationSection("hover");
            if (hoverSection != null) {
                broadcastHover = new BroadcastHover();
                String mode = hoverSection.getString("mode", "all");
                List<String> hoverLines = hoverSection.getStringList("text");

                broadcastHover.setMode(mode);
                broadcastHover.setText(hoverLines);

                if (mode.equals("line")) {
                    List<Integer> lineNumbers = hoverSection.getIntegerList("lines");
                    broadcastHover.setLines(lineNumbers);
                }
            }
        }

        BroadcastSound sound = null;
        if (section.contains("sound")) {
            ConfigurationSection soundSection = section.getConfigurationSection("sound");
            if (soundSection != null) {
                sound = new BroadcastSound();
                sound.setSoundName(soundSection.getString("name", "ENTITY_EXPERIENCE_ORB_PICKUP"));
                sound.setVolume((float) soundSection.getDouble("volume", 1.0));
                sound.setPitch((float) soundSection.getDouble("pitch", 1.0));
            }
        }

        BroadcastButton button = null;
        if (section.contains("button")) {
            ConfigurationSection buttonSection = section.getConfigurationSection("button");
            if (buttonSection != null) {
                button = new BroadcastButton();
                button.setText(buttonSection.getString("text", "&e[Нажми]"));
                button.setCommand(buttonSection.getString("command", ""));

                if (buttonSection.contains("hover")) {
                    button.setHoverText(buttonSection.getStringList("hover"));
                }
            }
        }

        return new Broadcast(id, messages, broadcastHover, button, sound);
    }

    private void startBroadcasting() {
        if (!enabled || broadcasts.isEmpty()) {
            plugin.getLogger().info("Автоматические объявления отключены или список пуст");
            return;
        }

        broadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }

            Broadcast broadcast = getNextBroadcast();
            if (broadcast != null) {
                sendBroadcast(broadcast);
            }
        }, interval * 20L, interval * 20L);

        plugin.getLogger().info("Автоматические объявления запущены с интервалом " + interval + " секунд");
    }

    private Broadcast getNextBroadcast() {
        if (broadcasts.isEmpty()) {
            return null;
        }

        if (remainingBroadcasts.isEmpty()) {
            remainingBroadcasts.addAll(broadcasts);
        }

        Random random = new Random();
        int index = random.nextInt(remainingBroadcasts.size());
        Broadcast broadcast = remainingBroadcasts.remove(index);

        return broadcast;
    }

    private void sendBroadcast(Broadcast broadcast) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDatabaseManager().isBroadcastsEnabled(player.getUniqueId())) {
                sendBroadcastToPlayer(player, broadcast);
            }
        }
    }

    private void sendBroadcastToPlayer(Player player, Broadcast broadcast) {
        for (int i = 0; i < broadcast.getMessages().size(); i++) {
            String message = broadcast.getMessages().get(i)
                    .replace("%player%", player.getName())
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%NL%", "\n");

            Component messageComponent = linkifyUrls(message);
            if (messageComponent == null) {
                messageComponent = ColorUtil.getInstance().component(message);
            }

            if (broadcast.getHover() != null && broadcast.getHover().getText() != null &&
                    !broadcast.getHover().getText().isEmpty()) {

                boolean applyHover = false;

                if (broadcast.getHover().getMode().equals("all")) {
                    applyHover = true;
                } else if (broadcast.getHover().getMode().equals("line") &&
                        broadcast.getHover().getLines() != null &&
                        broadcast.getHover().getLines().contains(i)) {
                    applyHover = true;
                }

                if (applyHover) {
                    List<String> hoverLines = new ArrayList<>();
                    for (String hoverLine : broadcast.getHover().getText()) {
                        hoverLines.add(hoverLine.replace("%player%", player.getName())
                                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));
                    }

                    String hoverText = String.join("\n", hoverLines);
                    HoverEvent<Component> hoverEvent = HoverEvent.showText(ColorUtil.getInstance().component(hoverText));
                    messageComponent = messageComponent.hoverEvent(hoverEvent);
                }
            }

            player.sendMessage(messageComponent);
        }

        if (broadcast.getButton() != null) {
            sendButton(player, broadcast.getButton());
        }

        if (broadcast.getSound() != null) {
            playSound(player, broadcast.getSound());
        }
    }

    private Component linkifyUrls(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String masked = FORMAT_CODE_PATTERN.matcher(raw)
                .replaceAll(m -> " ".repeat(m.group().length()));

        Matcher matcher = URL_PATTERN.matcher(masked);
        if (!matcher.find()) {
            return null;
        }
        matcher.reset();

        TextComponent.Builder builder = Component.text();
        int last = 0;

        while (matcher.find()) {
            String before = raw.substring(last, matcher.start());
            if (!before.isEmpty()) {
                builder.append(ColorUtil.getInstance().component(before));
            }

            String url = raw.substring(matcher.start(), matcher.end());
            String prefix = activeFormat(raw.substring(0, matcher.start()));
            Component linkComponent = ColorUtil.getInstance().component(prefix + url)
                    .clickEvent(ClickEvent.openUrl(toClickableUrl(url)));
            builder.append(linkComponent);

            last = matcher.end();
        }

        String tail = raw.substring(last);
        if (!tail.isEmpty()) {
            builder.append(ColorUtil.getInstance().component(tail));
        }

        return builder.build();
    }

    private String activeFormat(String text) {
        Matcher matcher = FORMAT_CODE_PATTERN.matcher(text);
        String color = "";
        StringBuilder formats = new StringBuilder();

        while (matcher.find()) {
            String code = matcher.group();
            char c = Character.toLowerCase(code.charAt(1));
            if (c == '#' || c == 'r' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                color = code;
                formats.setLength(0);
            } else {
                formats.append(code);
            }
        }

        return color + formats;
    }

    private String toClickableUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }

    private void sendButton(Player player, BroadcastButton button) {
        String buttonText = button.getText()
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        Component buttonComponent = ColorUtil.getInstance().component(buttonText);

        String command = button.getCommand()
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        ClickEvent clickEvent;
        if (command.startsWith("http://") || command.startsWith("https://")) {
            clickEvent = ClickEvent.openUrl(command);
        } else {
            clickEvent = ClickEvent.runCommand("/" + command);
        }
        buttonComponent = buttonComponent.clickEvent(clickEvent);

        if (button.getHoverText() != null && !button.getHoverText().isEmpty()) {
            List<String> hoverLines = new ArrayList<>();
            for (String hoverLine : button.getHoverText()) {
                hoverLines.add(hoverLine.replace("%player%", player.getName())
                        .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())));
            }

            String hoverText = String.join("\n", hoverLines);
            buttonComponent = buttonComponent.hoverEvent(HoverEvent.showText(
                    ColorUtil.getInstance().component(hoverText)
            ));
        }

        player.sendMessage(buttonComponent);
    }

    private void playSound(Player player, BroadcastSound broadcastSound) {
        try {
            Sound sound = RegistryUtil.find(
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT),
                    broadcastSound.getSoundName());
            if (sound == null) {
                throw new IllegalArgumentException("Unknown sound");
            }
            player.playSound(player.getLocation(), sound, broadcastSound.getVolume(), broadcastSound.getPitch());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверное название звука: " + broadcastSound.getSoundName());
        }
    }

    public void stopBroadcasting() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
            plugin.getLogger().info("Автоматические объявления остановлены");
        }
    }

    private void saveResourceIfNotExists(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    @Getter
    public static class Broadcast {
        private final String id;
        private final List<String> messages;
        private final BroadcastHover hover;
        private final BroadcastButton button;
        private final BroadcastSound sound;

        public Broadcast(String id, List<String> messages, BroadcastHover hover,
                         BroadcastButton button, BroadcastSound sound) {
            this.id = id;
            this.messages = messages;
            this.hover = hover;
            this.button = button;
            this.sound = sound;
        }
    }

    @Getter
    public static class BroadcastHover {
        private String mode = "all";
        private List<String> text;
        private List<Integer> lines;

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setText(List<String> text) {
            this.text = text;
        }

        public void setLines(List<Integer> lines) {
            this.lines = lines != null ? lines : new ArrayList<>();
        }
    }

    @Getter
    public static class BroadcastButton {
        private String text;
        private String command;
        private List<String> hoverText;

        public void setText(String text) {
            this.text = text;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public void setHoverText(List<String> hoverText) {
            this.hoverText = hoverText;
        }
    }

    @Getter
    public static class BroadcastSound {
        private String soundName;
        private float volume;
        private float pitch;

        public void setSoundName(String soundName) {
            this.soundName = soundName;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }
    }
}
