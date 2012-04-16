/**
 * Player listener. Built upon the SimpleJail project, created by imjake9.
 * https://github.com/imjake9/SimpleJail
 * 
 * @author imjake9
 * @author Mitsugaru
 */
package com.mitsugaru.karmicjail.events;

import com.mitsugaru.karmicjail.JailInventory;
import com.mitsugaru.karmicjail.JailInventoryHolder;
import com.mitsugaru.karmicjail.JailLogic;
import com.mitsugaru.karmicjail.KarmicJail;
import com.mitsugaru.karmicjail.KarmicJail.JailStatus;
import com.mitsugaru.utils.Config;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
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
	
	public void inventoryManipulation(final InventoryClickEvent event)
	{
		//Check if they're in our viewer list
	}
	
	//Sadly unused
	/*public void inventoryOpen(final InventoryOpenEvent event)
	{
		plugin.getLogger().info("inv open");
		if(event.getInventory().getHolder() instanceof JailInventoryHolder)
		{
			plugin.getLogger().info("our holder");
		}
	}
	
	public void inventoryClose(final InventoryCloseEvent event)
	{
		plugin.getLogger().info("inv close");
		if(event.getInventory().getHolder() instanceof JailInventoryHolder)
		{
			plugin.getLogger().info("our holder");
		}
	}*/

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(final PlayerChatEvent event)
	{
		final Player player = event.getPlayer();
		if (JailLogic.playerIsJailed(player.getName()))
		{
			if (JailLogic.playerIsMuted(player.getName()))
			{
				if (config.debugLog && config.debugEvents)
				{
					plugin.getLogger().info(
							"Muted '" + player.getName() + "' with message: "
									+ event.getMessage());
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(final PlayerRespawnEvent event)
	{

		final Player player = event.getPlayer();

		if (!JailLogic.playerIsJailed(player.getName()))
			return;
		if (config.debugLog && config.debugEvents)
		{
			plugin.getLogger().info(
					"Respawned '" + player.getName() + "' to jail.");
		}
		event.setRespawnLocation(JailLogic.getJailLocation());
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
				jailPlayer(player);
				// TODO throw inventory into database
				break;
			}
			case JAILED:
			{
				jailPlayer(player);
				break;
			}
			case PENDINGFREE:
			{
				JailLogic.freePlayer(plugin.console, player.getName());
				JailLogic.teleportOut(player.getName());
				player.sendMessage(ChatColor.GREEN + KarmicJail.prefix
						+ ChatColor.AQUA + "You have been removed from jail.");
				if (config.debugLog && config.debugEvents)
				{
					plugin.getLogger().info(
							"Unjailed '" + player.getName() + "' on login.");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(final PlayerQuitEvent event)
	{
		// Record location
		if (event.getPlayer() != null)
		{
			if (event.getPlayer().getName() != null
					&& event.getPlayer().getLocation() != null)
			{
				if(!JailLogic.playerIsJailed(event.getPlayer().getName()))
				{
				JailLogic.setPlayerLastLocation(event.getPlayer().getName(),
						event.getPlayer().getLocation());
				}
			}
		}
		//TODO record inventory
		if (config.debugLog && config.debugEvents)
		{
			plugin.getLogger().info(
					"Quit Event for: " + event.getPlayer().getName());
		}
		plugin.stopTask(event.getPlayer().getName());
	}

	private void jailPlayer(Player player)
	{
		if (JailLogic.playerIsTempJailed(player.getName()))
		{
			final long time = JailLogic.getPlayerTime(player.getName());
			if (time > 0)
			{
				final int minutes = (int) ((time / minutesToTicks));
				player.sendMessage(ChatColor.RED + KarmicJail.prefix
						+ ChatColor.AQUA + " You are jailed for "
						+ plugin.prettifyMinutes(minutes) + ".");
				plugin.addThread(player.getName(), time);
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
			player.sendMessage(ChatColor.RED + KarmicJail.prefix
					+ ChatColor.AQUA + " You are jailed.");
			if (config.debugLog && config.debugEvents)
			{
				plugin.getLogger().info(
						"Jailed '" + player.getName() + "' on login.");
			}
		}
		player.teleport(JailLogic.getJailLocation());
		if (config.broadcastJoin)
		{
			final StringBuilder sb = new StringBuilder();
			final String reason = JailLogic.getJailReason(player.getName());
			sb.append(ChatColor.RED + KarmicJail.prefix + ChatColor.AQUA + " "
					+ player.getName() + ChatColor.RED + " jailed");
			if (!reason.equals(""))
			{
				sb.append(" for " + ChatColor.GRAY
						+ plugin.colorizeText(reason));
			}
			if (config.broadcastPerms)
			{
				plugin.getServer().broadcast(sb.toString(),
						"KarmicJail.broadcast");
			}
			else
			{
				plugin.getServer().broadcastMessage(sb.toString());
			}
		}
	}

}
