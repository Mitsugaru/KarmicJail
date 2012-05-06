/**
 * Jail plugin custom tailed for the needs of Mine-RP. Built upon the SimpleJail
 * project, created by imjake9. https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */

package com.mitsugaru.karmicjail;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.mitsugaru.karmicjail.events.InventoryListener;
import com.mitsugaru.karmicjail.events.KarmicJailListener;
import com.mitsugaru.karmicjail.permissions.PermCheck;

public class KarmicJail extends JavaPlugin
{
	// Class Variables
	public static final String prefix = "[KarmicJail]";
	public static final long minutesToTicks = 1200;
	private Config config;
	public ConsoleCommandSender console;
	private PermCheck perm;
	private DBHandler database;
	private Commander commander;
	private static final Map<String, JailTask> threads = new HashMap<String, JailTask>();

	@Override
	public void onDisable()
	{
		// Stop all running threads
		getLogger().info(prefix + " Stopping all jail threads...");
		for (JailTask task : threads.values())
		{
			task.stop();
		}
		// Disconnect from sql database
		if (database.checkConnection())
		{
			// Close connection
			database.close();
		}
		getLogger().info(
				prefix + " " + this.getDescription().getName() + " v"
						+ this.getDescription().getVersion() + " disabled.");
	}

	@Override
	public void onEnable()
	{
		// Get console:
		console = this.getServer().getConsoleSender();

		// Grab config
		config = new Config(this);
		// Grab database
		database = new DBHandler(this, config);

		// Check if any updates are necessary
		config.checkUpdate();

		// Get permissions plugin:
		perm = new PermCheck(this);

		// Get commander
		commander = new Commander(this);

		// Initialize logic
		JailLogic.init(this);

		// Setup listeners
		this.getServer().getPluginManager()
				.registerEvents(new KarmicJailListener(this), this);
		this.getServer().getPluginManager()
				.registerEvents(new InventoryListener(this), this);

		getLogger().info(
				prefix + " " + this.getDescription().getName() + " v"
						+ this.getDescription().getVersion() + " enabled.");
	}

	public PermCheck getPermissions()
	{
		return perm;
	}

	/**
	 * Makes the minutes readable
	 * 
	 * @param minutes
	 * @return String of readable minutes
	 */
	public String prettifyMinutes(int minutes)
	{
		if (minutes < 1)
		{
			return "about less than a minute";
		}
		if (minutes == 1)
			return "about one minute";
		if (minutes < 60)
			return "about " + minutes + " minutes";
		if (minutes % 60 == 0)
		{
			if (minutes / 60 == 1)
				return "about one hour";
			else
				return "about " + (minutes / 60) + " hours";
		}
		int m = minutes % 60;
		int h = (minutes - m) / 60;
		return "about " + h + "h" + m + "m";
	}

	public DBHandler getDatabaseHandler()
	{
		return database;
	}

	/**
	 * Attempts to look up full name based on who's on the server Given a
	 * partial name
	 * 
	 * @author Frigid, edited by Raphfrk and petteyg359
	 */
	public String expandName(String Name)
	{
		int m = 0;
		String Result = "";
		for (int n = 0; n < this.getServer().getOnlinePlayers().length; n++)
		{
			String str = this.getServer().getOnlinePlayers()[n].getName();
			if (str.matches("(?i).*" + Name + ".*"))
			{
				m++;
				Result = str;
				if (m == 2)
				{
					return null;
				}
			}
			if (str.equalsIgnoreCase(Name))
				return str;
		}
		if (m == 1)
			return Result;
		if (m > 1)
		{
			return null;
		}
		return Name;
	}

	/**
	 * Colorizes a given string to Bukkit standards
	 * 
	 * http://forums.bukkit.org/threads/methode-to-colorize.69543/#post-1063437
	 * 
	 * @param string
	 * @return String with appropriate Bukkit ChatColor in them
	 * @author AmberK
	 */
	public String colorizeText(String string)
	{
		/**
		 * Colors
		 */
		string = string.replaceAll("&0", "" + ChatColor.BLACK);
		string = string.replaceAll("&1", "" + ChatColor.DARK_BLUE);
		string = string.replaceAll("&2", "" + ChatColor.DARK_GREEN);
		string = string.replaceAll("&3", "" + ChatColor.DARK_AQUA);
		string = string.replaceAll("&4", "" + ChatColor.DARK_RED);
		string = string.replaceAll("&5", "" + ChatColor.DARK_PURPLE);
		string = string.replaceAll("&6", "" + ChatColor.GOLD);
		string = string.replaceAll("&7", "" + ChatColor.GRAY);
		string = string.replaceAll("&8", "" + ChatColor.DARK_GRAY);
		string = string.replaceAll("&9", "" + ChatColor.BLUE);
		string = string.replaceAll("&a", "" + ChatColor.GREEN);
		string = string.replaceAll("&b", "" + ChatColor.AQUA);
		string = string.replaceAll("&c", "" + ChatColor.RED);
		string = string.replaceAll("&d", "" + ChatColor.LIGHT_PURPLE);
		string = string.replaceAll("&e", "" + ChatColor.YELLOW);
		string = string.replaceAll("&f", "" + ChatColor.WHITE);
		/**
		 * Formatting
		 */
		string = string.replaceAll("&k", "" + ChatColor.MAGIC);
		string = string.replaceAll("&l", "" + ChatColor.BOLD);
		string = string.replaceAll("&m", "" + ChatColor.STRIKETHROUGH);
		string = string.replaceAll("&n", "" + ChatColor.UNDERLINE);
		string = string.replaceAll("&o", "" + ChatColor.ITALIC);
		string = string.replaceAll("&r", "" + ChatColor.RESET);
		return string;
	}

	public Config getPluginConfig()
	{
		return config;
	}

	public Commander getCommander()
	{
		return commander;
	}

	/**
	 * Creates a new thread for a player to get auto-released
	 * 
	 * @param name
	 *            of player
	 */
	public void addThread(String name, long time)
	{
		threads.put(name, new JailTask(this, name, time));
	}

	/**
	 * Removes a task
	 * 
	 * @param name
	 */
	public static void removeTask(String name)
	{
		threads.remove(name);
	}

	/**
	 * Stops a player's timed task
	 * 
	 * @param name
	 *            of player
	 * @return true if the player's task was stopped. If unsucessful or if
	 *         player did not have a timed task, then it returns false.
	 */
	public boolean stopTask(String name)
	{
		if (threads.containsKey(name))
		{
			if (config.debugLog && config.debugEvents)
			{
				this.getLogger().info("Thread found for: " + name);
			}
			final boolean stop = threads.get(name).stop();
			if (config.debugLog && config.debugEvents)
			{
				if (stop)
				{
					this.getLogger().info("Thread stopped for: " + name);
				}
				else
				{
					this.getLogger().warning("Thread NOT stopped for: " + name);
				}
			}
			return stop;
		}
		else
		{
			if (config.debugLog && config.debugEvents)
			{
				this.getLogger().warning("Thread NOT found for: " + name);
			}
		}
		return false;
	}

	public static Map<String, JailTask> getJailThreads()
	{
		return threads;
	}

	public enum JailStatus
	{
		JAILED, PENDINGJAIL, PENDINGFREE, FREED;
	}

	public static class PrisonerInfo
	{
		public String name, jailer, date, reason;
		public long time;
		public boolean mute;

		public PrisonerInfo(String n, String j, String d, String r, long t,
				boolean m)
		{
			name = n;
			jailer = j;
			date = d;
			reason = r;
			time = t;
			mute = m;
		}

		public void updateTime(long t)
		{
			time = t;
		}
	}

}
