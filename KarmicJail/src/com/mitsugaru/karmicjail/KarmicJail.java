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

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mitsugaru.karmicjail.config.RootConfig;
import com.mitsugaru.karmicjail.database.DBHandler;
import com.mitsugaru.karmicjail.events.KJInventoryListener;
import com.mitsugaru.karmicjail.events.KJPlayerListener;
import com.mitsugaru.karmicjail.events.KarmicJailListener;
import com.mitsugaru.karmicjail.permissions.PermCheck;
import com.mitsugaru.karmicjail.update.Update;

public class KarmicJail extends JavaPlugin
{
	// Class Variables
	public static final String TAG = "[KarmicJail]";
	public static final long minutesToTicks = 1200;
	private RootConfig config;
	public ConsoleCommandSender console;
	private PermCheck perm;
	private DBHandler database;
	private Commander commander;
	private static final Map<String, JailTask> threads = new HashMap<String, JailTask>();

	@Override
	public void onDisable()
	{
		// Stop all running threads
		getLogger().info("Stopping all jail threads...");
		for (JailTask task : threads.values())
		{
			task.stop();
		}
		// Disconnect from sql database
		if (database.checkConnection())
		{
			// Close connection
			database.close();
			getLogger().info("Disconnected from database.");
		}
	}

	@Override
	public void onEnable()
	{
		// Get console:
		console = this.getServer().getConsoleSender();

		// Grab config
		config = new RootConfig(this);
		// Grab database
		database = new DBHandler(this, config);

		// Check if any updates are necessary
		Update.init(this);
		Update.checkUpdate();

		// Get permissions plugin:
		perm = new PermCheck(this);

		// Get commander
		commander = new Commander(this);

		// Initialize logic
		JailLogic.init(this);

		// Setup listeners
		final PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new KarmicJailListener(this), this);
		pm.registerEvents(new KJInventoryListener(this), this);
		pm.registerEvents(new KJPlayerListener(this), this);
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

	public RootConfig getPluginConfig()
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
