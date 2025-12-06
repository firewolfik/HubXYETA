package xd.firewolfik.hubxyeta.managers;

import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.io.File;
import java.util.*;

@Getter
public class BroadcastManager {

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

        // Загружаем hover и sound на уровне broadcast
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
            sendBroadcastToPlayer(player, broadcast);
        }
    }

    private void sendBroadcastToPlayer(Player player, Broadcast broadcast) {
        for (int i = 0; i < broadcast.getMessages().size(); i++) {
            String message = broadcast.getMessages().get(i)
                    .replace("%player%", player.getName())
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("%NL%", "\n");

            // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Сначала переводим цвет (включая Hex), потом создаем компоненты
            message = ColorUtil.getInstance().translateColor(message);
            BaseComponent[] messageComponents = TextComponent.fromLegacyText(message);

            // Применяем hover если он настроен
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
                        hoverLines.add(ColorUtil.getInstance().translateColor(
                                hoverLine.replace("%player%", player.getName())
                                        .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                        ));
                    }

                    String hoverText = String.join("\n", hoverLines);
                    BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents));

                    for (BaseComponent comp : messageComponents) {
                        comp.setHoverEvent(hoverEvent);
                    }
                }
            }

            player.spigot().sendMessage(messageComponents);
        }

        // Отправляем кнопку после всех сообщений
        if (broadcast.getButton() != null) {
            sendButton(player, broadcast.getButton());
        }

        // Воспроизводим звук
        if (broadcast.getSound() != null) {
            playSound(player, broadcast.getSound());
        }
    }

    private void sendButton(Player player, BroadcastButton button) {
        String buttonText = button.getText()
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        buttonText = ColorUtil.getInstance().translateColor(buttonText);
        BaseComponent[] buttonComponents = TextComponent.fromLegacyText(buttonText);

        String command = button.getCommand()
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command);

        if (button.getHoverText() != null && !button.getHoverText().isEmpty()) {
            List<String> hoverLines = new ArrayList<>();
            for (String hoverLine : button.getHoverText()) {
                hoverLines.add(ColorUtil.getInstance().translateColor(
                        hoverLine.replace("%player%", player.getName())
                                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                ));
            }

            String hoverText = String.join("\n", hoverLines);
            BaseComponent[] hoverComponents = TextComponent.fromLegacyText(hoverText);
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponents));

            for (BaseComponent comp : buttonComponents) {
                comp.setClickEvent(clickEvent);
                comp.setHoverEvent(hoverEvent);
            }
        } else {
            for (BaseComponent comp : buttonComponents) {
                comp.setClickEvent(clickEvent);
            }
        }

        player.spigot().sendMessage(buttonComponents);
    }

    private void playSound(Player player, BroadcastSound broadcastSound) {
        try {
            Sound sound = Sound.valueOf(broadcastSound.getSoundName().toUpperCase());
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