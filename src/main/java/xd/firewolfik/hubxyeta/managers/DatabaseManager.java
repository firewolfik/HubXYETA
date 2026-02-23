package xd.firewolfik.hubxyeta.managers;

import xd.firewolfik.hubxyeta.Main;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final Main plugin;
    private Connection connection;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
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

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateDatabase() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            ResultSet columns = metaData.getColumns(null, null, "players", "hide_players");
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE players ADD COLUMN hide_players INTEGER DEFAULT 0");
                }
            }
            columns.close();

        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при миграции базы данных: " + e.getMessage());
        }
    }

    public boolean isPlayerExists(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT uuid FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке игрока: " + e.getMessage());
            return false;
        }
    }

    public int addPlayer(UUID uuid, String name) {
        try {
            int playerNumber = getNextPlayerNumber();

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO players (uuid, name, player_number, broadcasts_enabled, hide_players) VALUES (?, ?, ?, 1, 0)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setInt(3, playerNumber);
                statement.executeUpdate();
            }

            return playerNumber;

        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при добавлении игрока: " + e.getMessage());
            return -1;
        }
    }

    private int getNextPlayerNumber() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(player_number) as max_number FROM players")) {
            ResultSet rs = statement.executeQuery();
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
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_number FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("player_number");
            }
            return -1;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при получении номера игрока: " + e.getMessage());
            return -1;
        }
    }

    public int getTotalPlayers() {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) as total FROM players")) {
            ResultSet rs = statement.executeQuery();
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
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT broadcasts_enabled FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("broadcasts_enabled") == 1;
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке статуса рассылки: " + e.getMessage());
            return true;
        }
    }

    public void setBroadcastsEnabled(UUID uuid, boolean enabled) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE players SET broadcasts_enabled = ? WHERE uuid = ?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при изменении статуса рассылки: " + e.getMessage());
        }
    }

    public boolean isHidePlayersEnabled(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT hide_players FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt("hide_players") == 1;
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при проверке статуса скрытия игроков: " + e.getMessage());
            return false;
        }
    }

    public void setHidePlayersEnabled(UUID uuid, boolean enabled) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE players SET hide_players = ? WHERE uuid = ?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка при изменении статуса скрытия игроков: " + e.getMessage());
        }
    }

    public void close() {
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