package xd.firewolfik.hubxyeta.managers;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import xd.firewolfik.hubxyeta.Main;
import xd.firewolfik.hubxyeta.util.ActionExecutor;
import xd.firewolfik.hubxyeta.util.ColorUtil;

import java.io.File;
import java.util.*;

@Getter
public class ItemsManager {

    private final Main plugin;
    private final ActionExecutor actionExecutor;
    private FileConfiguration itemsConfig;
    private Map<String, LobbyItem> lobbyItems;
    private Map<UUID, Map<String, Long>> cooldowns;

    public ItemsManager(Main plugin) {
        this.plugin = plugin;
        this.actionExecutor = new ActionExecutor(plugin);
        this.lobbyItems = new HashMap<>();
        this.cooldowns = new HashMap<>();
        loadItemsConfig();
    }

    public void loadItemsConfig() {
        saveResourceIfNotExists("items.yml");
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        loadItems();
    }

    public void reloadItems() {
        lobbyItems.clear();
        loadItemsConfig();
        plugin.getLogger().info("Предметы перезагружены! Загружено: " + lobbyItems.size());
    }

    private void loadItems() {
        lobbyItems.clear();

        ConfigurationSection items = itemsConfig.getConfigurationSection("Items");
        if (items == null) {
            plugin.getLogger().warning("Секция 'Items' не найдена в items.yml!");
            return;
        }

        for (String itemId : items.getKeys(false)) {
            ConfigurationSection itemSection = items.getConfigurationSection(itemId);
            if (itemSection != null) {
                try {
                    LobbyItem lobbyItem = createLobbyItem(itemId, itemSection);
                    if (lobbyItem != null) {
                        lobbyItems.put(itemId, lobbyItem);
                        plugin.getLogger().info("Загружен предмет: " + itemId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Ошибка загрузки предмета " + itemId + ": " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("Загружено предметов: " + lobbyItems.size());
    }

    private LobbyItem createLobbyItem(String id, ConfigurationSection section) {
        try {
            String name = section.getString("name");
            List<String> lore = section.getStringList("lore");
            String materialName = section.getString("material").toUpperCase();
            int slot = section.getInt("slot", 0);
            int amount = Math.max(1, section.getInt("amount", 1));
            List<String> actions = section.getStringList("actions");
            int cooldown = section.getInt("cooldown", 0);
            String cooldownMessage = section.getString("cooldown-message");

            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный материал для предмета " + id + ": " + materialName);
                material = Material.PAPER;
            }

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                plugin.getLogger().warning("Не удалось получить ItemMeta для предмета " + id);
                return null;
            }

            meta.setDisplayName(ColorUtil.getInstance().translateColor(name));

            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.getInstance().translateColor(line));
                }
                meta.setLore(coloredLore);
            }

            if (section.contains("enchantments")) {
                List<String> enchantments = section.getStringList("enchantments");
                for (String enchantStr : enchantments) {
                    applyEnchantment(meta, enchantStr, id);
                }
            }

            if (section.contains("flags")) {
                List<String> flags = section.getStringList("flags");
                for (String flagStr : flags) {
                    applyItemFlag(meta, flagStr, id);
                }
            }

            NamespacedKey key = new NamespacedKey(plugin, "lobby_item_" + id);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);

            return new LobbyItem(id, item, slot, actions, cooldown, cooldownMessage);

        } catch (Exception e) {
            plugin.getLogger().severe("Критическая ошибка создания предмета " + id + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void applyEnchantment(ItemMeta meta, String enchantStr, String itemId) {
        try {
            String[] parts = enchantStr.split(":");
            if (parts.length < 2) {
                plugin.getLogger().warning("Неверный формат зачарования для " + itemId + ": " + enchantStr);
                return;
            }

            String enchantName = parts[0].toUpperCase();
            int level = Integer.parseInt(parts[1]);

            Enchantment enchantment = getEnchantmentByName(enchantName);

            if (enchantment != null) {
                meta.addEnchant(enchantment, level, true);
            } else {
                plugin.getLogger().warning("Неизвестное зачарование для " + itemId + ": " + enchantName);
            }

        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Неверный уровень зачарования для " + itemId + ": " + enchantStr);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка применения зачарования для " + itemId + ": " + e.getMessage());
        }
    }

    private Enchantment getEnchantmentByName(String name) {
        Enchantment enchantment = Enchantment.getByName(name);
        if (enchantment != null) {
            return enchantment;
        }

        Map<String, String> legacyNames = Map.of(
                "DIG_SPEED", "EFFICIENCY",
                "DAMAGE_ALL", "SHARPNESS",
                "ARROW_DAMAGE", "POWER",
                "ARROW_KNOCKBACK", "PUNCH",
                "ARROW_FIRE", "FLAME",
                "ARROW_INFINITE", "INFINITY",
                "LUCK", "LUCK_OF_THE_SEA",
                "LURE", "LURE"
        );

        String modernName = legacyNames.get(name);
        if (modernName != null) {
            return Enchantment.getByName(modernName);
        }

        return null;
    }

    private void applyItemFlag(ItemMeta meta, String flagStr, String itemId) {
        try {
            ItemFlag flag = ItemFlag.valueOf(flagStr.toUpperCase());
            meta.addItemFlags(flag);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный флаг для предмета " + itemId + ": " + flagStr);
        }
    }

    public void giveAllItems(Player player) {
        player.getInventory().clear();

        for (LobbyItem lobbyItem : lobbyItems.values()) {
            giveItem(player, lobbyItem);
        }

        player.updateInventory();
    }

    public void giveItem(Player player, String itemId) {
        LobbyItem item = lobbyItems.get(itemId);
        if (item != null) {
            giveItem(player, item);
            player.updateInventory();
        } else {
            plugin.getLogger().warning("Попытка выдать несуществующий предмет: " + itemId);
        }
    }

    public void giveItemToSlot(Player player, String itemId, int slot) {
        LobbyItem item = lobbyItems.get(itemId);
        if (item != null) {
            if (slot < 0 || slot > 35) {
                plugin.getLogger().warning("Неверный слот " + slot + " для предмета " + itemId);
                slot = 0;
            }

            player.getInventory().setItem(slot, item.getItem().clone());
            player.updateInventory();
        } else {
            plugin.getLogger().warning("Попытка выдать несуществующий предмет: " + itemId);
        }
    }

    private void giveItem(Player player, LobbyItem lobbyItem) {
        int slot = lobbyItem.getSlot();

        if (slot < 0 || slot > 35) {
            plugin.getLogger().warning("Неверный слот " + slot + " для предмета " + lobbyItem.getId());
            slot = 0;
        }

        player.getInventory().setItem(slot, lobbyItem.getItem().clone());
    }

    public void executeItemActions(Player player, String itemId) {
        LobbyItem item = lobbyItems.get(itemId);
        if (item == null) {
            return;
        }

        if (item.getCooldown() > 0) {
            if (isOnCooldown(player, itemId)) {
                long remaining = getRemainingCooldown(player, itemId);
                String message = item.getCooldownMessage()
                        .replace("%time%", String.valueOf(remaining));
                player.sendMessage(ColorUtil.getInstance().translateColor(message));
                return;
            }
            setCooldown(player, itemId);
        }

        if (item.getActions() != null && !item.getActions().isEmpty()) {
            actionExecutor.executeActions(player, item.getActions());
        }
    }

    private boolean isOnCooldown(Player player, String itemId) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }

        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(itemId)) {
            return false;
        }

        long cooldownEnd = playerCooldowns.get(itemId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    private long getRemainingCooldown(Player player, String itemId) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }

        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(itemId)) {
            return 0;
        }

        long cooldownEnd = playerCooldowns.get(itemId);
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    private void setCooldown(Player player, String itemId) {
        LobbyItem item = lobbyItems.get(itemId);
        if (item == null || item.getCooldown() <= 0) {
            return;
        }

        cooldowns.putIfAbsent(player.getUniqueId(), new HashMap<>());
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());

        long cooldownEnd = System.currentTimeMillis() + (item.getCooldown() * 1000L);
        playerCooldowns.put(itemId, cooldownEnd);
    }

    public String getLobbyItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        for (String itemId : lobbyItems.keySet()) {
            NamespacedKey key = new NamespacedKey(plugin, "lobby_item_" + itemId);
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                return itemId;
            }
        }

        return null;
    }

    public boolean isLobbyItem(ItemStack item) {
        return getLobbyItemId(item) != null;
    }

    public LobbyItem getLobbyItem(String itemId) {
        return lobbyItems.get(itemId);
    }

    public Map<String, LobbyItem> getAllItems() {
        return new HashMap<>(lobbyItems);
    }

    private void saveResourceIfNotExists(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    @Getter
    public static class LobbyItem {
        private final String id;
        private final ItemStack item;
        private final int slot;
        private final List<String> actions;
        private final int cooldown;
        private final String cooldownMessage;

        public LobbyItem(String id, ItemStack item, int slot, List<String> actions, int cooldown, String cooldownMessage) {
            this.id = id;
            this.item = item.clone();
            this.slot = slot;
            this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
            this.cooldown = cooldown;
            this.cooldownMessage = cooldownMessage;
        }

        public ItemStack getItemClone() {
            return item.clone();
        }

        public boolean hasActions() {
            return actions != null && !actions.isEmpty();
        }

        public int getActionsCount() {
            return actions != null ? actions.size() : 0;
        }
    }
}