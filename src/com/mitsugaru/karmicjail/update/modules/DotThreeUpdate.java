package com.mitsugaru.karmicjail.update.modules;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.database.Table;
import com.mitsugaru.karmicjail.services.Version;
import com.mitsugaru.karmicjail.update.UpdateModule;

/**
 * Handles the 0.3 update.
 * 
 * @author Mitsugaru
 */
public class DotThreeUpdate extends UpdateModule {

    /**
     * Constructor.
     * 
     * @param plugin
     *            - Plugin instance.
     */
    public DotThreeUpdate(KarmicJail plugin) {
        super(plugin);
        targetVersion = new Version("0.3");
        targetVersion.setIgnorePatch(true);
    }

    @Override
    public void update() {
        DBHandler database = plugin.getModuleForClass(DBHandler.class);
        // Drop newly created tables
        plugin.getLogger().info(KarmicJail.TAG + " Dropping empty tables.");
        ResultSet rs = null;
        try {
            rs = database.query("DROP TABLE " + Table.JAILED.getName() + ";");
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to drop jailed table.", e);
        } finally {
            database.cleanup(rs, null);
        }
        // Update tables to have prefix
        plugin.getLogger().info(
                KarmicJail.TAG + " Renaming jailed table to '"
                        + Table.JAILED.getName() + "'.");
        String query = "ALTER TABLE jailed RENAME TO " + Table.JAILED.getName()
                + ";";
        ResultSet second = null;
        try {
            second = database.query(query);
        } catch(SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to alter jailed table with prefix.", e);
        } finally {
            database.cleanup(second, null);
        }
    }

}
