package com.mitsugaru.karmicjail.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mitsugaru.karmicjail.KarmicJail;

public class JailConfig
{
	private KarmicJail plugin;
	private File file;
	private YamlConfiguration config;
	private String jailName = "";
	
	public JailConfig(KarmicJail plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;
		try
		{
			this.config = YamlConfiguration.loadConfiguration(file);
			/*
			 * Thanks to brianegge for the regex
			 * http://stackoverflow.com/questions
			 * /924394/how-to-get-file-name-without-the-extension
			 */
			jailName = file.getName().replaceFirst("[.][^.]+$", "").toLowerCase();
			loadDefaults();
			loadSettings();
		}
		catch (IllegalArgumentException ia)
		{
			// TODO notify
			ia.printStackTrace();
		}
	}
	
	private void loadDefaults()
	{
		// Setup defaults
		final Map<String, Object> defaults = new LinkedHashMap<String, Object>();
		defaults.put("broadcast.jail", false);
		defaults.put("broadcast.unjail", false);
		defaults.put("broadcast.reasonChange", false);
		defaults.put("broadcast.onjoin", false);
		defaults.put("broadcast.ignorePermission", false);
		defaults.put("deny.block.break", true);
		defaults.put("deny.block.place", true);
		defaults.put("deny.chat", false);
		defaults.put("deny.commands", true);
		defaults.put("deny.interact", true);
		defaults.put("deny.inventory", false);
		defaults.put("deny.item.craft", true);
		defaults.put("deny.item.drop", true);
		defaults.put("deny.item.enchant", true);
		defaults.put("deny.item.pickup", true);
		defaults.put("deny.move", false);
		defaults.put("group.removeOnJail", true);
		defaults.put("group.returnOnUnjail", true);
		defaults.put("group.jail.group", "Jailed");
		defaults.put("group.jail.use", true);
		defaults.put("group.unjail.group", "Default");
		defaults.put("group.unjail.use", false);
		defaults.put("inventory.clearOnJail", true);
		defaults.put("inventory.returnOnUnjail", true);
		defaults.put("inventory.modify", true);
		defaults.put("jail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("jail.x", 0);
		defaults.put("jail.y", 0);
		defaults.put("jail.z", 0);
		defaults.put("jail.warpAllOnJoin", false);
		defaults.put("jail.teleport", true);
		defaults.put("jail.teleportRespawn", true);
		defaults.put("unjail.world", plugin.getServer().getWorlds().get(0)
				.getName());
		defaults.put("unjail.x", 0);
		defaults.put("unjail.y", 0);
		defaults.put("unjail.z", 0);
		defaults.put("unjail.teleport", true);
		// Add missing defaults
		for (final Map.Entry<String, Object> entry : defaults.entrySet())
		{
			if (!config.contains(entry.getKey()))
			{
				config.set(entry.getKey(), entry.getValue());
			}
		}
		save();
	}
	
	private void loadSettings()
	{
		
	}
	
	public String getJailName()
	{
		return jailName;
	}
	
	public void save()
	{
		try
		{
			config.save(file);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reload()
	{
		try
		{
			config.load(file);
		}
		catch (FileNotFoundException fnf)
		{
			fnf.printStackTrace();
		}
		catch (IOException io)
		{
			io.printStackTrace();
		}
		catch (InvalidConfigurationException ic)
		{
			ic.printStackTrace();
		}
		loadDefaults();
		loadSettings();
	}
}
