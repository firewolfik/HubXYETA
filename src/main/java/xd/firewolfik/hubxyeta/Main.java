package xd.firewolfik.hubxyeta;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xd.firewolfik.hubxyeta.commands.HubCommand;
import xd.firewolfik.hubxyeta.commands.SpawnCommand;
import xd.firewolfik.hubxyeta.config.ConfigManager;
import xd.firewolfik.hubxyeta.listeners.PlayerListener;
import xd.firewolfik.hubxyeta.managers.ItemsManager;

import java.io.File;

@Getter
public final class Main extends JavaPlugin {

    private ConfigManager configManager;
    private ItemsManager itemsManager;
    private FileConfiguration messagesConfig;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {

        // Сохраняем дефолтные конфиги
        saveDefaultConfig();
        saveResource("items.yml", false);
        saveResource("messages.yml", false);

        // Загружаем конфиг сообщений
        loadMessagesConfig();

        // Инициализируем менеджеры
        configManager = new ConfigManager(this);
        itemsManager = new ItemsManager(this);

        getLogger().info("Загрузка HubXyEta...");

        // Регистрируем слушатели
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Регистрируем команды
        registerCommands();

        getLogger().info("HubXyEta успешно загружен!");
        getLogger().info("Загружено предметов: " + itemsManager.getAllItems().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("HubXyEta выгружен!");
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
        reloadConfig();
        loadMessagesConfig();
        configManager.reloadConfigs();
        itemsManager.reloadItems();
        playerListener.restartAllActionBars();
        getLogger().info("Плагин перезагружен!");
    }
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}