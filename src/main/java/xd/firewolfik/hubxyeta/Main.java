package xd.firewolfik.hubxyeta;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.hubxyeta.commands.*;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.listeners.PlayerListener;
import xd.firewolfik.hubxyeta.managers.BroadcastManager;
import xd.firewolfik.hubxyeta.managers.ItemsManager;
import xd.firewolfik.hubxyeta.managers.DatabaseManager;
import xd.firewolfik.hubxyeta.util.UnloadUtil;

import java.io.File;

@Getter
public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private ItemsManager itemsManager;
    private BroadcastManager broadcastManager;
    private DatabaseManager databaseManager;
    private FileConfiguration messagesConfig;
    private PlayerListener playerListener;
    private UnloadUtil unloadUtil;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        saveResource("messages.yml", false);
        saveResource("broadcasts.yml", false);

        loadMessagesConfig();

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Плагин отключен: база данных недоступна");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);
        itemsManager = new ItemsManager(this);
        broadcastManager = new BroadcastManager(this);
        unloadUtil = new UnloadUtil(this);
        playerListener = new PlayerListener(this);

        getServer().getPluginManager().registerEvents(playerListener, this);

        getLogger().info("#############################");
        getLogger().info("HubXYETA - включен");
        getLogger().info("Автор: firewolfik.lol");
        getLogger().info("Связь с разработчиком: t.me/firewolfik");
        getLogger().info("Версия плагина: " + getPluginMeta().getVersion());
        getLogger().info("#############################");

        registerCommands();

        getLogger().info("[Info] Загружено предметов: " + itemsManager.getAllItems().size());
        Bukkit.getScheduler().runTask(this, this::restoreOnlinePlayers);
    }

    @Override
    public void onDisable() {
        getLogger().info("HubXYETA - выключен");
        if (playerListener != null) {
            playerListener.stopAllActionBars();
        }
        if (broadcastManager != null) {
            broadcastManager.stopBroadcasting();
        }
        if (unloadUtil != null) {
            unloadUtil.unloadPlayerConfig();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void restoreOnlinePlayers() {
        if (!isEnabled()) {
            return;
        }

        playerListener.resetPlayerVisibility();
        for (Player player : Bukkit.getOnlinePlayers()) {
            configManager.setupPlayer(player);
            itemsManager.giveAllItems(player);
            playerListener.restorePlayerVisibility(player);
        }
        playerListener.restartAllActionBars();
    }

    private void registerCommands() {
        HubCommand hubCommand = new HubCommand(this);
        registerCommand("hub", hubCommand, hubCommand);

        SpawnCommand spawnCommand = new SpawnCommand(this);
        registerCommand("spawn", spawnCommand, spawnCommand);

        BroadcastCommand broadcastCommand = new BroadcastCommand(this);
        registerCommand("broadcast", broadcastCommand, broadcastCommand);

        PlayersCommand playersCommand = new PlayersCommand(this);
        registerCommand("players", playersCommand, playersCommand);

        LinksCommand linksCommand = new LinksCommand(this);
        registerCommand("links", linksCommand, null);
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Команда '" + name + "' не объявлена в plugin.yml");
            return;
        }

        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadPlugin() {
        playerListener.stopAllActionBars();
        broadcastManager.stopBroadcasting();
        loadMessagesConfig();
        configManager.reloadConfigs();
        itemsManager.reloadItems();
        broadcastManager.reloadBroadcasts();
        restoreOnlinePlayers();
        getLogger().info("[Info] Плагин перезагружен");
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
