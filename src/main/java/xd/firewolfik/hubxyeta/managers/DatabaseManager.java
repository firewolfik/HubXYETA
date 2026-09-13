package xd.firewolfik.hubxyeta.managers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import xd.firewolfik.hubxyeta.Main;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final Main plugin;
    private Connection connection;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    @Getter
    @Setter
    public static class PlayerProfile {
        private final UUID uuid;
        private String name;
        private final int playerNumber;
        private boolean broadcastsEnabled;
        private boolean hidePlayers;
        private final boolean isNew;

        public PlayerProfile(UUID uuid, String name, int playerNumber, boolean broadcastsEnabled, boolean hidePlayers, boolean isNew) {
            this.uuid = uuid;
            this.name = name;
            this.playerNumber = playerNumber;
            this.broadcastsEnabled = broadcastsEnabled;
            this.hidePlayers = hidePlayers;
            this.isNew = isNew;
        }
    }

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Не удалось создать папку данных плагина");
                return false;
            }

            File dbFile = new File(dataFolder, "data.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS players (" +
                                "uuid TEXT PRIMARY KEY," +
                                "name TEXT NOT NULL," +
                                "player_number INTEGER NOT NULL," +
                                "broadcasts_enabled INTEGER DEFAULT 1," +
                                "hide_players INTEGER DEFAULT 0," +
                                "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")"
                );
            }

            migrateDatabase();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при инициализации базы данных: " + e.getMessage());
            return false;
        }
    }

    private void migrateDatabase() throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet columns = metaData.getColumns(null, null, "players", "hide_players")) {
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE players ADD COLUMN hide_players INTEGER DEFAULT 0");
                }
            }
        }
    }

    public synchronized PlayerProfile loadOrRegisterPlayer(UUID uuid, String name) {
        PlayerProfile cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_number, broadcasts_enabled, hide_players FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int number = rs.getInt("player_number");
                    boolean broadcasts = rs.getInt("broadcasts_enabled") == 1;
                    boolean hidePlayers = rs.getInt("hide_players") == 1;
                    PlayerProfile profile = new PlayerProfile(uuid, name, number, broadcasts, hidePlayers, false);
                    cache.put(uuid, profile);
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при загрузке игрока " + name + ": " + e.getMessage());
        }

        int newNumber = getNextPlayerNumber();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players (uuid, name, player_number, broadcasts_enabled, hide_players) VALUES (?, ?, ?, 1, 0)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setInt(3, newNumber);
            statement.executeUpdate();

            PlayerProfile profile = new PlayerProfile(uuid, name, newNumber, true, false, true);
            cache.put(uuid, profile);
            return profile;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при добавлении игрока " + name + ": " + e.getMessage());
            PlayerProfile fallback = new PlayerProfile(uuid, name, newNumber, true, false, false);
            cache.put(uuid, fallback);
            return fallback;
        }
    }

    public PlayerProfile getCachedProfile(UUID uuid) {
        return cache.get(uuid);
    }

    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public synchronized boolean isPlayerExists(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return true;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uuid FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке игрока: " + e.getMessage());
            return false;
        }
    }

    public synchronized int addPlayer(UUID uuid, String name) {
        PlayerProfile profile = loadOrRegisterPlayer(uuid, name);
        return profile != null ? profile.getPlayerNumber() : -1;
    }

    public void ensurePlayerExists(UUID uuid, String name) {
        loadOrRegisterPlayer(uuid, name);
    }

    private synchronized int getNextPlayerNumber() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(player_number) as max_number FROM players");
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("max_number") + 1;
            }
            return 1;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении номера игрока: " + e.getMessage());
            return 1;
        }
    }

    public int getPlayerNumber(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            return profile.getPlayerNumber();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_number FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_number");
                }
            }
            return -1;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении номера игрока: " + e.getMessage());
            return -1;
        }
    }

    public synchronized int getTotalPlayers() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) as total FROM players");
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении количества игроков: " + e.getMessage());
            return 0;
        }
    }

    public boolean isBroadcastsEnabled(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            return profile.isBroadcastsEnabled();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT broadcasts_enabled FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("broadcasts_enabled") == 1;
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке статуса рассылки: " + e.getMessage());
            return true;
        }
    }

    public void setBroadcastsEnabled(UUID uuid, boolean enabled) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setBroadcastsEnabled(enabled);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (DatabaseManager.this) {
                if (connection == null) return;
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE players SET broadcasts_enabled = ? WHERE uuid = ?")) {
                    statement.setInt(1, enabled ? 1 : 0);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка при изменении статуса рассылки: " + e.getMessage());
                }
            }
        });
    }

    public boolean isHidePlayersEnabled(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            return profile.isHidePlayers();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT hide_players FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("hide_players") == 1;
                }
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке статуса скрытия игроков: " + e.getMessage());
            return false;
        }
    }

    public void setHidePlayersEnabled(UUID uuid, boolean enabled) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setHidePlayers(enabled);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (DatabaseManager.this) {
                if (connection == null) return;
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE players SET hide_players = ? WHERE uuid = ?")) {
                    statement.setInt(1, enabled ? 1 : 0);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Ошибка при изменении статуса скрытия игроков: " + e.getMessage());
                }
            }
        });
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Соединение с базой данных закрыто.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при закрытии базы данных: " + e.getMessage());
        }
    }
}
