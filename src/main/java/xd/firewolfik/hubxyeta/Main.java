package xd.firewolfik.hubxyeta;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.hubxyeta.commands.HubCommand;
import xd.firewolfik.hubxyeta.commands.SpawnCommand;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.listeners.PlayerListener;
import xd.firewolfik.hubxyeta.managers.BroadcastManager;
import xd.firewolfik.hubxyeta.managers.ItemsManager;
import xd.firewolfik.hubxyeta.util.UnloadUtil;

import java.io.File;

@Getter
public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private ItemsManager itemsManager;
    private BroadcastManager broadcastManager;
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

        configManager = new ConfigManager(this);
        itemsManager = new ItemsManager(this);
        broadcastManager = new BroadcastManager(this);
        unloadUtil = new UnloadUtil(this);
        playerListener = new PlayerListener(this);

        getServer().getPluginManager().registerEvents(playerListener, this);

        getLogger().info(ChatColor.YELLOW + "#############################");
        getLogger().info(ChatColor.WHITE + "HubXYETA" + ChatColor.GRAY + " - " + ChatColor.GREEN + "включен");
        getLogger().info(ChatColor.WHITE + "Автор:" + ChatColor.GOLD + " firewolfik");
        getLogger().info(ChatColor.WHITE + "Связь с разработчиком:" + ChatColor.YELLOW + " t.me/oooSwagParty");
        getLogger().info(ChatColor.WHITE + "Версия плагина:" + ChatColor.GOLD + " 2.1" + ChatColor.GRAY + " (Fix errors 14.09.25)");
        getLogger().info(ChatColor.YELLOW + "#############################");

        registerCommands();

        getLogger().info(ChatColor.GRAY +"[Info]" + ChatColor.WHITE + " Загружено предметов: " + ChatColor.YELLOW + itemsManager.getAllItems().size());
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.YELLOW + "#############################");
        getLogger().info(ChatColor.WHITE + "HubXYETA" + ChatColor.GRAY + " - " + ChatColor.RED + "выключен");
        getLogger().info(ChatColor.WHITE + "Автор:" + ChatColor.GOLD + " firewolfik");
        getLogger().info(ChatColor.WHITE + "Связь с разработчиком:" + ChatColor.YELLOW + " t.me/oooSwagParty");
        getLogger().info(ChatColor.WHITE + "Версия плагина:" + ChatColor.GOLD + " 2.1" + ChatColor.GRAY + " (Fix errors 14.09.25)");
        getLogger().info(ChatColor.YELLOW + "#############################");
        broadcastManager.stopBroadcasting();
        unloadUtil.unloadPlayerConfig();
    }

    private void registerCommands() {
        HubCommand hubCommand = new HubCommand(this);
        getCommand("hub").setExecutor(hubCommand);
        getCommand("hub").setTabCompleter(hubCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(this));
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
        reloadConfig();
        loadMessagesConfig();
        configManager.reloadConfigs();
        itemsManager.reloadItems();
        broadcastManager.reloadBroadcasts();
        playerListener.restartAllActionBars();
        getLogger().info(ChatColor.WHITE +"[" + ChatColor.YELLOW + "Info" + ChatColor.WHITE + "]" + ChatColor.WHITE + " Плагин " + ChatColor.GREEN + "перезагружен");
    }
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}