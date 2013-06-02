package com.mitsugaru.karmicjail.update.modules;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.services.Version;
import com.mitsugaru.karmicjail.update.UpdateModule;
import com.mitsugaru.karmicjail.update.holders.PointFourFourObject;

/**
 * Handles the 0.44 update.
 * 
 * @author Mitsugaru
 */
public class DotFourFourUpdate extends UpdateModule {

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public DotFourFourUpdate(KarmicJail plugin) {
        super(plugin);
        targetVersion = new Version("0.44");
        targetVersion.setIgnorePatch(true);
    }

    @Override
    public void update() {
        DBHandler database = plugin.getModuleForClass(DBHandler.class);
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        String query = "";
        plugin.getLogger().info("Update item database");
        final Set<PointFourFourObject> entries = new HashSet<PointFourFourObject>();
        ResultSet rs = null;
        try {
            rs = database.query("SELECT * FROM " + Table.INVENTORY.getName()
                    + ";");
            if(rs.next()) {
                do {
                    PointFourFourObject tempInv = new PointFourFourObject();
                    tempInv.row = rs.getInt("row");
                    tempInv.id = rs.getInt("id");
                    tempInv.slot = rs.getInt("slot");
                    tempInv.itemId = rs.getInt("itemId");
                    tempInv.amount = rs.getInt("amount");
                    tempInv.durability = Short.valueOf(rs
                            .getString("durability"));
                    tempInv.enchantments = rs.getString("enchantments");
                    entries.add(tempInv);
                } while(rs.next());
            }
            database.cleanup(rs, null);
            // Drop old table
            ResultSet second = database.query("DROP TABLE "
                    + Table.INVENTORY.getName() + ";");
            database.cleanup(second, null);
            // Create new table
            if(config.useMySQL) {
                query = "CREATE TABLE "
                        + Table.INVENTORY.getName()
                        + " (row INT UNSIGNED NOT NULL AUTO_INCREMENT, id INT UNSIGNED NOT NULL, slot INT NOT NULL, itemid SMALLINT UNSIGNED NOT NULL, amount INT NOT NULL, durability TINYTEXT NOT NULL, enchantments TEXT, PRIMARY KEY(row));";
            } else {
                query = "CREATE TABLE "
                        + Table.INVENTORY.getName()
                        + " (row INTEGER PRIMARY KEY, id INTEGER NOT NULL, slot INTEGER NOT NULL, itemid INTEGER NOT NULL, amount INTEGER NOT NULL, durability TEXT NOT NULL, enchantments TEXT);";
            }
            ResultSet third = database.query(query);
            database.cleanup(third, null);
            // Add back entries
            try {
                if(!entries.isEmpty()) {
                    for(PointFourFourObject entry : entries) {
                        PreparedStatement statement = database
                                .prepare("INSERT INTO "
                                        + Table.INVENTORY.getName()
                                        + " (row,id,slot,itemid,amount,durability,enchantments) VALUES(?,?,?,?,?,?,?)");
                        statement.setInt(1, entry.row);
                        statement.setInt(2, entry.id);
                        statement.setInt(3, entry.slot);
                        statement.setInt(4, entry.itemId);
                        statement.setInt(5, entry.amount);
                        statement.setString(6, entry.durability + "");
                        statement.setString(7, entry.enchantments);
                        ResultSet fourth = null;
                        try {
                            fourth = database.query(statement);
                        } catch(SQLException e) {
                            plugin.getLogger().log(Level.SEVERE,
                                    KarmicJail.TAG + " SQLException", e);
                        } finally {
                            database.cleanup(fourth, statement);
                        }
                    }
                }
            } catch(SQLException e) {
                plugin.getLogger().warning(
                        KarmicJail.TAG + " SQL Exception on 0.44 update");
            }
        } catch(SQLException e) {
            plugin.getLogger().warning(
                    KarmicJail.TAG + " SQL Exception on 0.44 update");
        }
    }

}
