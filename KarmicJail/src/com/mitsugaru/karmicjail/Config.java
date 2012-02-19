package com.mitsugaru.karmicjail;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class Config {

	private KarmicJail plugin;
	public String host, port, database, user, password, tablePrefix;
	public boolean useMySQL, debugTime, importSQL, unjailTeleport;
	public Location jailLoc, unjailLoc;
	public String jailGroup;
	public int limit;

	/**
	 * Loads config from yaml file
	 */
	public Config(KarmicJail karmicJail) {
		plugin = karmicJail;
		// Init config files:
		ConfigurationSection config = plugin.getConfig();
		final Map<String, Object> defaults = new HashMap<String, Object>();
		defaults.put("jailgroup", "Jailed");
		defaults.put("jail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("jail.x", 0);
		defaults.put("jail.y", 0);
		defaults.put("jail.z", 0);
		defaults.put("unjail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("unjail.x", 0);
		defaults.put("unjail.y", 0);
		defaults.put("unjail.z", 0);
		defaults.put("unjail.teleport", true);
		defaults.put("entrylimit", 10);
		defaults.put("version", plugin.getDescription().getVersion());

		// Insert defaults into config file if they're not present
		for (final Entry<String, Object> e : defaults.entrySet()) {
			if (!config.contains(e.getKey())) {
				config.set(e.getKey(), e.getValue());
			}
		}
		// Save config
		plugin.saveConfig();

		// Load variables from config
		jailLoc = new Location(plugin.getServer().getWorld(
				config.getString("jail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("jail.x", 0),
				config.getInt("jail.y", 0), config.getInt("jail.z", 0));
		unjailLoc = new Location(plugin.getServer().getWorld(
				config.getString("unjail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("unjail.x", 0),
				config.getInt("unjail.y", 0), config.getInt("unjail.z", 0));
		jailGroup = config.getString("jailgroup", "Jailed");
		debugTime = config.getBoolean("debugTime", false);
		limit = config.getInt("entrylimit", 10);
		unjailTeleport = config.getBoolean("unjail.teleport", true);
		// Bounds check on the limit
		if (limit <= 0 || limit > 16) {
			plugin.log.warning(KarmicJail.prefix
					+ " Entry limit is <= 0 || > 16. Reverting to default: 10");
			limit = 10;
			config.set("entrylimit", 10);
		}
	}

	public void reload() {
		// Reload
		plugin.reloadConfig();
		// Grab config
		ConfigurationSection config = plugin.getConfig();
		// Load variables from config
		jailLoc = new Location(plugin.getServer().getWorld(
				config.getString("jail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("jail.x", 0),
				config.getInt("jail.y", 0), config.getInt("jail.z", 0));
		unjailLoc = new Location(plugin.getServer().getWorld(
				config.getString("unjail.world", plugin.getServer().getWorlds()
						.get(0).getName())), config.getInt("unjail.x", 0),
				config.getInt("unjail.y", 0), config.getInt("unjail.z", 0));
		jailGroup = config.getString("jailgroup", "Jailed");
		debugTime = config.getBoolean("debugTime", false);
		limit = config.getInt("entrylimit", 10);
		unjailTeleport = config.getBoolean("unjail.teleport", true);
		// Bounds check on the limit
		if (limit <= 0 || limit > 16) {
			plugin.log.warning(KarmicJail.prefix
					+ " Entry limit is <= 0 || > 16. Reverting to default: 10");
			limit = 10;
			config.set("entrylimit", 10);
		}
	}
	
	/**
	 * Check if updates are necessary
	 */
	public void checkUpdate() {
		// Check if need to update
		ConfigurationSection config = plugin.getConfig();
		if (Double.parseDouble(plugin.getDescription().getVersion()) > Double
				.parseDouble(config.getString("version")))
		{
			// Update to latest version
			plugin.log.info(KarmicJail.prefix + " Updating to v"
					+ plugin.getDescription().getVersion());
			update();
		}
	}
	
	/**
	 * This method is called to make the appropriate changes, most likely only
	 * necessary for database schema modification, for a proper update.
	 */
	private void update() {
		// Grab current version
		double ver = Double.parseDouble(plugin.getConfig().getString("version"));
		String query = "";
		// Updates to alpha 0.08
		if (ver < 0.2)
		{
			// Add enchantments column
			query = "ALTER TABLE jailed ADD muted INTEGER;";
			plugin.getLiteDB().standardQuery(query);
		}

		// Update version number in config.yml
		plugin.getConfig().set("version", plugin.getDescription().getVersion());
		plugin.saveConfig();
	}
	
	public void set(String path, Object o) {
		final ConfigurationSection config = plugin.getConfig();
		config.set(path, o);
		plugin.saveConfig();
	}
}
