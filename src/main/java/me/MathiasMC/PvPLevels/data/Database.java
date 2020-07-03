package me.MathiasMC.PvPLevels.data;

import me.MathiasMC.PvPLevels.PvPLevels;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class Database {

    private final PvPLevels plugin;
    private final boolean debug_database;

    private dev.magicmq.rappu.Database database;

    public Database(final PvPLevels plugin) {
        this.plugin = plugin;
        debug_database = plugin.config.get.getBoolean("debug.database");
        database = dev.magicmq.rappu.Database.newDatabase()
                .withPluginUsing(PvPLevels.call)
                .withConnectionInfo(plugin.config.get.getString("mysql.host"), plugin.config.get.getInt("mysql.port"), plugin.config.get.getString("mysql.database"), false)
                .withUsername(plugin.config.get.getString("mysql.username"))
                .withPassword(plugin.config.get.getString("mysql.password"))
                .withDefaultProperties()
                .open();
        createTable();
    }

    private void createTable() {
        try {
            database.createTableFromFile("table.sql", PvPLevels.class);
            alterTable();
            if (debug_database) { plugin.textUtils.debug("[Database] Creating table if not exists"); }
        } catch (IOException | SQLException e) {
            plugin.textUtils.warning("[Database] Error when creating the MySQL table:");
            e.printStackTrace();
        }
    }

    private void alterTable() {
        if (plugin.config.get.getBoolean("mysql.alter")) {
            String sql = "ALTER TABLE `players` ADD COLUMN `lastseen` DATETIME AFTER `level`";
            database.updateAsync(sql, new Object[]{}, integer -> {});
            plugin.config.get.set("mysql.alter", false);
            plugin.config.save();
            plugin.textUtils.warning("[Database] Alter table players");
        }
    }

    public void close() {
        database.close();
        if (debug_database) { plugin.textUtils.debug("[Database] Closing connection"); }
    }

    public void insert(final String uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?;";
        database.queryAsync(sql, new Object[]{uuid}, resultSet -> {
            if (!resultSet.next()) {
                String insertSQL = "INSERT INTO PLAYERS ";
                insertSQL += "(uuid, kills, deaths, xp, level, lastseen) ";
                insertSQL += "VALUES(?, ?, ?, ?, ?, ?);";
                Object[] toSet = new Object[]{uuid, 0L, 0L, 0L, 0L, new Timestamp(new Date().getTime())};
                database.updateAsync(insertSQL, toSet, integer -> {});
                if (debug_database) { plugin.textUtils.debug("[Database] Inserting default values for UUID: " + uuid); }
            }
        });
    }

    public void setValues(final String uuid, final Long kills, final Long deaths, final Long xp, final Long level, Timestamp timestamp) {
        String sql = "SELECT * FROM players WHERE uuid = ?;";
        database.queryAsync(sql, new Object[]{uuid}, resultSet -> {
            if (resultSet.next()) {
                String updateSQL = "UPDATE players SET ";
                updateSQL += "kills = ?, deaths = ?, xp = ?, level = ?, lastseen = ? ";
                updateSQL +="WHERE uuid = ?;";
                Object[] toSet = new Object[]{kills, deaths, xp, level, timestamp, uuid};
                database.updateAsync(updateSQL, toSet, integer -> {});
                if (debug_database) { plugin.textUtils.debug("[Database] Updating values for UUID: " + uuid); }
            }
        });
    }

    public void getValues(String uuid) {
        String sql = "SELECT * FROM players ";
        sql += "WHERE uuid = ?;";
        database.queryAsync(sql, new Object[]{uuid}, resultSet -> {
            if (resultSet.next()) {
                if (debug_database) { plugin.textUtils.debug("[Database] Getting values for UUID: " + uuid); }
                PvPLevels.call.loadCallback(uuid, new String[]{String.valueOf(resultSet.getLong("kills")),
                                String.valueOf(resultSet.getLong("deaths")),
                                String.valueOf(resultSet.getLong("xp")),
                                String.valueOf(resultSet.getLong("level")),
                                String.valueOf(resultSet.getTime("lastseen"))});
            } else {
                PvPLevels.call.loadCallback(uuid, new String[] { String.valueOf(0L), String.valueOf(0L), String.valueOf(0L), String.valueOf(0L) });
            }
        });
    }

    private ArrayList<String> getUUIDList() {
        if (debug_database) { plugin.textUtils.debug("[Database] Getting list of UUID in the table"); }
        ArrayList<String> toReturn = new ArrayList<>();
        String sql = "SELECT uuid FROM players;";
        try (ResultSet result = database.query(sql, new Object[]{})) {
            while (result.next()) {
                toReturn.add(result.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.textUtils.exception(e.getStackTrace(), e.getMessage());
        }
        return toReturn;
    }

    public void loadOnline() {
        if (plugin.getServer().getOnlinePlayers().size() > 0) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!plugin.list().contains(player.getUniqueId().toString()))
                    plugin.load(player.getUniqueId().toString());
            }
        }
        if (debug_database) { plugin.textUtils.debug("[Database] Loading all online players into cache"); }
    }

    public void loadALL() {
        for (String list : getUUIDList()) {
            if (!plugin.list().contains(list))
                plugin.load(list);
        }
        if (debug_database) { plugin.textUtils.debug("[Database] Loading all players into cache"); }
    }
}