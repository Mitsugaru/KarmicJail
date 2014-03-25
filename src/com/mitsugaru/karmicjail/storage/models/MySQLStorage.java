package com.mitsugaru.karmicjail.storage.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;

import lib.PatPeter.SQLibrary.MySQL;

import org.bukkit.Location;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.interfaces.IRootConfig;
import com.mitsugaru.karmicjail.jail.Jail;
import com.mitsugaru.karmicjail.jail.JailStatus;

public class MySQLStorage extends DatabaseStorage {

    private final MySQL mysql;

    public MySQLStorage(KarmicJail plugin) {
        super(plugin);

        IRootConfig config = plugin.getModuleForClass(IRootConfig.class);
        // Connect to mysql database
        mysql = new MySQL(plugin.getLogger(), "KJ", config.getHostName(),
                config.getPort(), config.getDatabase(), config.getUsername(),
                config.getPassword());
    }

    @Override
    public String getDate(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setDate(String playerName, String date) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getJailer(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setJailer(String playerName, String jailer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public double getJailTime(String playerName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean setJailTime(String playerName, double time) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JailStatus getStatus(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setStatus(String playerName, JailStatus status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Location getLastLocation(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setLastLocation(String playerName, Location location) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getReason(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setReason(String playerName, String reason) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMuted(String playerName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setMuted(String playerName, boolean mute) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Jail getJail(String jail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addJail(String jail, Location location) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeJail(String jail) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Jail getJailForPlayer(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getGroups(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setGroups(String playerName, Collection<String> groups) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() {
        mysql.close();
    }

    @Override
    public boolean checkConnection() {
        return mysql.isOpen();
    }

    @Override
    boolean checkTables() {
        mysql.open();
        boolean valid = true;
        // Check if jailed table exists
        if(!mysql.isTable(Table.JAILED.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created jailed table");
            // Jail table
            try {
                mysql.query("CREATE TABLE "
                        + Table.JAILED.getName()
                        + " (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(32) NOT NULL, status TEXT NOT NULL, time REAL NOT NULL, groups TEXT, jailer varchar(32), date TEXT, reason TEXT, muted INT, lastpos TEXT, UNIQUE (playername), PRIMARY KEY(id));");
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating jailed table.", e);
                valid = false;
            }
        }
        // Check for history table
        if(!mysql.isTable(Table.HISTORY.getName())) {
            plugin.getLogger().info(KarmicJail.TAG + " Created history table");
            // history table
            try {
                mysql.query("CREATE TABLE "
                        + Table.HISTORY.getName()
                        + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, history TEXT NOT NULL, PRIMARY KEY(row));");
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating history table.", e);
                valid = false;
            }
        }
        // Check inventory table
        if(!mysql.isTable(Table.INVENTORY.getName())) {
            plugin.getLogger()
                    .info(KarmicJail.TAG + " Created inventory table");
            // inventory table
            try {
                mysql.query("CREATE TABLE "
                        + Table.INVENTORY.getName()
                        + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, slot INT NOT NULL, itemid SMALLINT UNSIGNED NOT NULL, amount INT NOT NULL, durability TINYTEXT NOT NULL, enchantments TEXT, PRIMARY KEY(row));");
            } catch(SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "SQLException on creating inventory table.", e);
                valid = false;
            }
        }
        return valid;
    }

    public ResultSet query(String query) throws SQLException {
        try {
            return mysql.query(query);
        } catch(SQLException e) {
            // Try and reopen
            mysql.open();
            return mysql.query(query);
        }
    }

    public ResultSet query(PreparedStatement statement) throws SQLException {
        try {
            return mysql.query(statement);
        } catch(SQLException e) {
            mysql.open();
            return mysql.query(statement);
        }
    }

    public PreparedStatement prepare(String statement) throws SQLException {
        try {
            return mysql.prepare(statement);
        } catch(SQLException e) {
            mysql.open();
            return mysql.prepare(statement);
        }
    }

}
