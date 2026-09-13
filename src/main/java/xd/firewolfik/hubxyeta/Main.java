package xd.firewolfik.hubxyeta;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.hubxyeta.action.ActionService;
import xd.firewolfik.hubxyeta.commands.*;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.listeners.PlayerConnectionListener;
import xd.firewolfik.hubxyeta.listeners.PlayerInventoryListener;
import xd.firewolfik.hubxyeta.listeners.PlayerProtectionListener;
import xd.firewolfik.hubxyeta.managers.ActionBarManager;
import xd.firewolfik.hubxyeta.managers.BroadcastManager;
import xd.firewolfik.hubxyeta.managers.DatabaseManager;
import xd.firewolfik.hubxyeta.managers.ItemsManager;
import xd.firewolfik.hubxyeta.util.ActionExecutor;
import xd.firewolfik.hubxyeta.util.MessageService;
import xd.firewolfik.hubxyeta.util.PlaceholderUtil;
import xd.firewolfik.hubxyeta.util.UnloadUtil;

import java.util.List;

@Getter
public final class Main extends JavaPlugin {

    private MessageService messageService;
    private ActionService actionService;
    private ActionExecutor actionExecutor;
    private ActionBarManager actionBarManager;
    private ConfigManager configManager;
    private ItemsManager itemsManager;
    private BroadcastManager broadcastManager;
    private DatabaseManager databaseManager;
    private PlaceholderUtil placeholderUtil;
    private UnloadUtil unloadUtil;

    private PlayerConnectionListener connectionListener;
    private PlayerProtectionListener protectionListener;
    private PlayerInventoryListener inventoryListener;

    @Override
    public void onEnable() {
        saveDefaultResources();

        messageService = new MessageService(this);
        placeholderUtil = new PlaceholderUtil(this);
        actionBarManager = new ActionBarManager(this);
        actionService = new ActionService(this);
        actionExecutor = new ActionExecutor(this);

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

        connectionListener = new PlayerConnectionListener(this);
        protectionListener = new PlayerProtectionListener(this);
        inventoryListener = new PlayerInventoryListener(this);

        List.of(connectionListener, protectionListener, inventoryListener)
                .forEach(l -> getServer().getPluginManager().registerEvents(l, this));

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        registerCommands();
        logStartupBanner();

        Bukkit.getScheduler().runTask(this, this::restoreOnlinePlayers);
    }

    @Override
    public void onDisable() {
        getLogger().info("HubXYETA - выключен");
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");

        if (actionBarManager != null) {
            actionBarManager.stopAll();
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

    public void reloadPlugin() {
        if (actionBarManager != null) {
            actionBarManager.stopAll();
        }
        if (broadcastManager != null) {
            broadcastManager.stopBroadcasting();
        }

        messageService.reload();
        configManager.reloadConfigs();
        itemsManager.reloadItems();
        broadcastManager.reloadBroadcasts();
        restoreOnlinePlayers();
        getLogger().info("[Info] Плагин перезагружен");
    }

    private void restoreOnlinePlayers() {
        if (!isEnabled()) {
            return;
        }

        connectionListener.resetPlayerVisibility();
        for (Player player : Bukkit.getOnlinePlayers()) {
            configManager.setupPlayer(player);
            itemsManager.giveAllItems(player);
            player.getInventory().setHeldItemSlot(configManager.getSelectedSlot());
            connectionListener.restorePlayerVisibility(player);
        }
        actionBarManager.restartAll();
    }

    private void registerCommands() {
        registerCommand("hub", new HubCommand(this));
        registerCommand("spawn", new SpawnCommand(this));
        registerCommand("broadcast", new BroadcastCommand(this));
        registerCommand("players", new PlayersCommand(this));
        registerCommand("links", new LinksCommand(this));
    }

    private void registerCommand(String name, BaseCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().severe("Команда '" + name + "' не объявлена в plugin.yml");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private void saveDefaultResources() {
        saveDefaultConfig();
        saveResource("items.yml", false);
        saveResource("messages.yml", false);
        saveResource("broadcasts.yml", false);
    }

    private void logStartupBanner() {
        getLogger().info("#############################");
        getLogger().info("HubXYETA - включен");
        getLogger().info("Автор: firewolfik.lol");
        getLogger().info("Связь с разработчиком: t.me/firewolfik");
        getLogger().info("Версия плагина: " + getPluginMeta().getVersion());
        if (placeholderUtil.isPlaceholderApiEnabled()) {
            getLogger().info("PlaceholderAPI найден - сторонние плейсхолдеры включены");
        } else {
            getLogger().info("PlaceholderAPI не найден - доступны только встроенные плейсхолдеры");
        }
        getLogger().info("[Info] Загружено предметов: " + itemsManager.getAllItems().size());
        getLogger().info("#############################");
    }

    public FileConfiguration getMessagesConfig() {
        return messageService.getConfig();
    }
}
