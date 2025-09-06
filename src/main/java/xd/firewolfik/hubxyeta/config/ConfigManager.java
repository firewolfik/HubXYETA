package xd.firewolfik.hubxyeta.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xd.firewolfik.hubxyeta.Main;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ConfigManager {

    private final Main plugin;

    // General
    private Location lobbyLocation;

    // Settings
    private boolean hidePlayer;
    private boolean actionBarEnabled;
    private boolean disableChat;
    private boolean disableHunger;
    private boolean disableDamage;

    // Hiders
    private boolean hideStream;

    // Items
    private boolean disableMove;
    private boolean disableDrop;
    private boolean disablePickup;

    // World
    private int worldTime;
    private boolean doDaylightCycle;
    private boolean doWeatherCycle;
    private boolean doMobSpawning;

    // Player
    private GameMode playerGameMode;
    private double playerHealth;
    private List<PotionEffect> playerEffects;

    // Join
    private List<String> joinMessages;
    private String joinTitle;
    private String joinSubtitle;
    private boolean clearChat;

    // Leave
    private boolean clearItems;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.playerEffects = new ArrayList<>();
        this.joinMessages = new ArrayList<>();

        loadAll();
    }

    public void loadAll() {
        loadMainConfig();
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        loadAll();
        plugin.getLogger().info("Конфигурации перезагружены!");
    }

    private void loadMainConfig() {
        FileConfiguration config = plugin.getConfig();

        // Локация лобби
        loadLobbyLocation(config);

        // Settings
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            hidePlayer = settings.getBoolean("hide-player", false);
            actionBarEnabled = settings.getBoolean("action-bar-settings", false);
            disableChat = settings.getBoolean("disable-chat", true);
            disableHunger = settings.getBoolean("disable-hunger", true);
            disableDamage = settings.getBoolean("disable-damage", true);
        }

        // Hiders
        ConfigurationSection hiders = config.getConfigurationSection("hiders");
        if (hiders != null) {
            hideStream = hiders.getBoolean("hide-stream", true);
        }

        // Items
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            disableMove = items.getBoolean("disable_move", true);
            disableDrop = items.getBoolean("disable_drop", true);
            disablePickup = items.getBoolean("disable_pickup", true);
        }

        // World
        ConfigurationSection world = config.getConfigurationSection("world");
        if (world != null) {
            worldTime = world.getInt("time", 6000);
            ConfigurationSection rules = world.getConfigurationSection("rules");
            if (rules != null) {
                doDaylightCycle = rules.getBoolean("doDaylightCycle", false);
                doWeatherCycle = rules.getBoolean("doWeatherCycle", false);
                doMobSpawning = rules.getBoolean("doMobSpawning", false);
            }
        }

        // Player
        loadPlayerConfig(config);

        // Join
        ConfigurationSection join = config.getConfigurationSection("join");
        if (join != null) {
            joinMessages = join.getStringList("join-message");
            joinTitle = join.getString("join-title", "");
            joinSubtitle = join.getString("join-subtitle", "");
            clearChat = join.getBoolean("clear-chat", true);
        }

        // Leave
        ConfigurationSection leave = config.getConfigurationSection("leave");
        if (leave != null) {
            clearItems = leave.getBoolean("clear-items", true);
        }
    }

    private void loadLobbyLocation(FileConfiguration config) {
        ConfigurationSection locationHub = config.getConfigurationSection("general.locationHub");
        if (locationHub != null) {
            String worldName = locationHub.getString("world", "world");
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                lobbyLocation = new Location(
                        world,
                        locationHub.getDouble("x", 0),
                        locationHub.getDouble("y", 100),
                        locationHub.getDouble("z", 0),
                        (float) locationHub.getDouble("yaw", 0),
                        (float) locationHub.getDouble("pitch", 0)
                );
            } else {
                plugin.getLogger().warning("Мир '" + worldName + "' не найден!");
                lobbyLocation = null;
            }
        }
    }

    private void loadPlayerConfig(FileConfiguration config) {
        ConfigurationSection player = config.getConfigurationSection("player");
        if (player != null) {
            // Игровой режим
            try {
                playerGameMode = GameMode.valueOf(player.getString("gamemode", "ADVENTURE"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный игровой режим, используется ADVENTURE");
                playerGameMode = GameMode.ADVENTURE;
            }

            // Здоровье
            playerHealth = Math.max(1.0, Math.min(20.0, player.getDouble("heath", 20.0)));

            // Эффекты
            playerEffects.clear();
            List<String> effects = player.getStringList("effects");
            for (String effectStr : effects) {
                PotionEffect effect = parseEffect(effectStr);
                if (effect != null) {
                    playerEffects.add(effect);
                }
            }
        }
    }

    private PotionEffect parseEffect(String effectStr) {
        try {
            String[] parts = effectStr.split(":");
            if (parts.length < 3) {
                plugin.getLogger().warning("Неверный формат эффекта: " + effectStr);
                return null;
            }

            PotionEffectType type = PotionEffectType.getByName(parts[0]);
            if (type == null) {
                plugin.getLogger().warning("Неизвестный эффект: " + parts[0]);
                return null;
            }

            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2]) == 0 ? Integer.MAX_VALUE : Integer.parseInt(parts[2]) * 20;
            boolean ambient = parts.length > 3 && Boolean.parseBoolean(parts[3]);

            return new PotionEffect(type, duration, amplifier, ambient, true);

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка парсинга эффекта: " + effectStr);
            return null;
        }
    }

    public void saveLobbyLocation(Location location) {
        plugin.getConfig().set("general.locationHub.world", location.getWorld().getName());
        plugin.getConfig().set("general.locationHub.x", location.getX());
        plugin.getConfig().set("general.locationHub.y", location.getY());
        plugin.getConfig().set("general.locationHub.z", location.getZ());
        plugin.getConfig().set("general.locationHub.yaw", location.getYaw());
        plugin.getConfig().set("general.locationHub.pitch", location.getPitch());
        plugin.saveConfig();
        loadLobbyLocation(plugin.getConfig());
    }

    public boolean isLobbyLocationSet() {
        return lobbyLocation != null && lobbyLocation.getWorld() != null;
    }

    public Location getSafeLobbyLocation() {
        return lobbyLocation != null ? lobbyLocation.clone() : null;
    }

    public void setupWorld(World world) {
        if (world == null) return;

        world.setTime(worldTime);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, doDaylightCycle);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, doWeatherCycle);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, doMobSpawning);
    }

    public void setupPlayer(Player player) {
        player.setGameMode(playerGameMode);
        player.setHealth(playerHealth);

        // Убираем все эффекты
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        // Добавляем новые эффекты
        playerEffects.forEach(player::addPotionEffect);
    }
}