/**
 * Player listener. Built upon the SimpleJail project, created by imjake9.
 * https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */
package com.mitsugaru.karmicjail.events;

import com.mitsugaru.karmicjail.Commander;
import com.mitsugaru.karmicjail.Config;
import com.mitsugaru.karmicjail.JailLogic;
import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import com.mitsugaru.karmicjail.permissions.PermissionNode;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class KarmicJailListener implements Listener
{
	// Class variables
	private final KarmicJail plugin;
	private final Config config;
	private static final long minutesToTicks = 1200;

	public KarmicJailListener(KarmicJail plugin)
	{
		this.plugin = plugin;
		this.config = plugin.getPluginConfig();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(final PlayerChatEvent event)
	{
		final String name = event.getPlayer().getName();
		if (JailLogic.playerCache.contains(name))
		{
			if (JailLogic.playerIsMuted(name))
			{
				if (config.debugLog && config.debugEvents)
				{
					plugin.getLogger().info(
							"Muted '" + name + "' with message: "
									+ event.getMessage());
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(final PlayerRespawnEvent event)
	{
		// Grab name
		final String name = event.getPlayer().getName();
		// Check if they're in the cache
		if (config.jailTeleportRespawn && JailLogic.playerCache.contains(name))
		{
			event.setRespawnLocation(JailLogic.getJailLocation());
			if (config.debugLog && config.debugEvents)
			{
				plugin.getLogger().info("Respawned '" + name + "' to jail.");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(final PlayerJoinEvent event)
	{

		// Attempt to add player to database
		JailLogic.addPlayerToDatabase(event.getPlayer().getName());

		final Player player = event.getPlayer();
		final JailStatus status = JailLogic.getPlayerStatus(player.getName());
		// Check status
		switch (status)
		{
			case PENDINGJAIL:
			{
				JailLogic.setPlayerStatus(JailStatus.JAILED, player.getName());
				int id = plugin
						.getServer()
						.getScheduler()
						.scheduleSyncDelayedTask(plugin,
								new LoginJailTask(player), 30);
				if (id == -1)
				{
					plugin.getLogger().severe(
							"Could not jail player '" + player.getName()
									+ "' on login!");
				}
				break;
			}
			case JAILED:
			{
				int id = plugin
						.getServer()
						.getScheduler()
						.scheduleSyncDelayedTask(plugin,
								new LoginJailTask(player), 30);
				if (id == -1)
				{
					plugin.getLogger().severe(
							"Could not jail player '" + player.getName()
									+ "' on login!");
				}
				break;
			}
			case PENDINGFREE:
			{
				JailLogic.freePlayer(plugin.console, player.getName());
				// Warp them to unjail location
				int id = plugin
						.getServer()
						.getScheduler()
						.scheduleSyncDelayedTask(
								plugin,
								new LoginWarpTask(player, JailLogic
										.getUnjailLocation()), 30);
				if (id == -1)
				{
					plugin.getLogger().severe(
							"Could not warp player '" + player.getName()
									+ "' out jail on login!");
				}
				else
				{
					player.sendMessage(ChatColor.GREEN + KarmicJail.TAG
							+ ChatColor.AQUA
							+ "You have been removed from jail.");
				}
				if (config.debugLog && config.debugEvents)
				{
					plugin.getLogger().info(
							"Unjailed '" + player.getName() + "' on login.");
				}
			}
			default:
			{
				if (config.warpAllOnJoin)
				{
					if (!plugin.getPermissions().has(player,
							PermissionNode.WARP_JOINIGNORE))
					{
						// Warp them to jail location
						int id = plugin
								.getServer()
								.getScheduler()
								.scheduleSyncDelayedTask(
										plugin,
										new LoginWarpTask(player, JailLogic
												.getJailLocation()), 30);
						if (id == -1)
						{
							plugin.getLogger().severe(
									"Could not warp player '"
											+ player.getName()
											+ "' to jail on login!");
						}
					}
				}
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event)
	{
		if (config.debugLog && config.debugEvents)
		{
			plugin.getLogger().info(
					"Quit Event for: " + event.getPlayer().getName());
		}
		// Record location
		if (event.getPlayer() != null)
		{
			final String name = event.getPlayer().getName();
			if (name != null && event.getPlayer().getLocation() != null)
			{
				if (!JailLogic.playerIsJailed(name))
				{
					JailLogic.setPlayerLastLocation(name, event.getPlayer()
							.getLocation());
				}
			}
			if (event.getPlayer().getInventory() != null)
			{
				JailLogic.setPlayerInventory(name, event.getPlayer()
						.getInventory(), false);
			}
			// Remove from cache
			try
			{
				JailLogic.playerCache.remove(name);
			}
			catch (Exception e)
			{
				// IGNORE
			}
		}
		// Remove viewer
		Commander.inv.remove(event.getPlayer().getName());
		// Remove history viewer
		Commander.historyCache.remove(event.getPlayer().getName());
		plugin.stopTask(event.getPlayer().getName());
	}

	private class LoginWarpTask implements Runnable
	{
		private Player player;
		private Location location;

		public LoginWarpTask(Player player, Location location)
		{
			this.player = player;
			this.location = location;
		}

		@Override
		public void run()
		{
			player.teleport(location);
		}
	}

	private class LoginJailTask implements Runnable
	{
		private Player player;

		public LoginJailTask(Player player)
		{
			this.player = player;
		}

		@Override
		public void run()
		{
			// Get name
			final String playerName = player.getName();
			// Add to cache
			JailLogic.playerCache.add(playerName);
			if (JailLogic.playerIsTempJailed(playerName))
			{
				final long time = JailLogic.getPlayerTime(playerName);
				if (time > 0)
				{
					final int minutes = (int) ((time / minutesToTicks));
					player.sendMessage(ChatColor.RED + KarmicJail.TAG
							+ ChatColor.AQUA + " You are jailed for "
							+ plugin.prettifyMinutes(minutes) + ".");
					plugin.addThread(playerName, time);
					if (config.debugLog && config.debugEvents)
					{
						plugin.getLogger().info(
								"Jailed '" + player.getName()
										+ "' on login with time: "
										+ plugin.prettifyMinutes(minutes));
					}
				}
			}
			else
			{
				player.sendMessage(ChatColor.RED + KarmicJail.TAG
						+ ChatColor.AQUA + " You are jailed.");
				if (config.debugLog && config.debugEvents)
				{
					plugin.getLogger().info(
							"Jailed '" + playerName + "' on login.");
				}
			}
			if (config.clearInventory)
			{
				player.getInventory().clear();
			}
			if (config.jailTeleport)
			{
				player.teleport(JailLogic.getJailLocation());
			}
			if (config.broadcastJoin)
			{
				final StringBuilder sb = new StringBuilder();
				final String reason = JailLogic.getJailReason(player.getName());
				sb.append(ChatColor.RED + KarmicJail.TAG + ChatColor.AQUA
						+ " " + playerName + ChatColor.RED + " jailed");
				if (!reason.equals(""))
				{
					sb.append(" for " + ChatColor.GRAY
							+ plugin.colorizeText(reason));
				}
				if (config.broadcastPerms)
				{
					plugin.getServer().broadcast(sb.toString(),
							PermissionNode.BROADCAST.getNode());
				}
				else
				{
					plugin.getServer().broadcastMessage(sb.toString());
				}
			}
		}

	}

}
