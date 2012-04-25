package com.mitsugaru.karmicjail.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.mitsugaru.karmicjail.Commander;
import com.mitsugaru.karmicjail.JailInventoryHolder;
import com.mitsugaru.karmicjail.KarmicJail;

public class InventoryListener implements Listener
{
	private static KarmicJail plugin;

	public InventoryListener(KarmicJail plugin)
	{
		InventoryListener.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCloseJailInventory(final InventoryCloseEvent event)
	{
		if(event.getInventory().getHolder() instanceof JailInventoryHolder)
		{
			Commander.inv.remove(event.getPlayer().getName());
			if (plugin.getPluginConfig().debugLog && plugin.getPluginConfig().debugEvents)
			{
				plugin.getLogger().info(
						"'" + event.getPlayer().getName() + "' closed JailInventory view");
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void handleInventory(final InventoryClickEvent event)
	{
		// Check if its our holder
		if (!event.isCancelled()
				&& event.getInventory().getHolder() instanceof JailInventoryHolder)
		{
			final Player player = (Player) event.getWhoClicked();
			if(!plugin.getPluginConfig().modifyInventory || !plugin.getPermissions().has(player, "KarmicJail.inventory.modify"))
			{
				event.setCancelled(true);
				return;
			}
			JailInventoryHolder holder = (JailInventoryHolder) event
					.getInventory().getHolder();
			final String target = holder.getTarget();
			//plugin.getLogger().info("raw slot: " + event.getRawSlot());
			// Determine if they clicked inside, and which half
			final int rawSlot = event.getRawSlot();
			boolean inside = false;
			boolean fromInventory = false;
			//boolean armor = false;
			boolean invalid = false;
			// TODO differentiate between regular inventory section and armor
			// section 36 -> 39
			// and ignore the other sections
			if(rawSlot < 0)
			{
				//Ignore
			}
			else if (rawSlot >= 0 && rawSlot <= 35)
			{
				inside = true;
				fromInventory = true;
			}
			else if (rawSlot >= 45 && rawSlot <= 80)
			{
				inside = true;
			}
			else
			{
				invalid = true;
			}
			// They clicked inside, handle changes
			if (inside)
			{
				try
				{
					if (fromInventory)
					{
						// Handle left clicks
						if (event.isLeftClick())
						{
							// Handle shift clicks
							if (event.isShiftClick())
							{
								/*
								 * Shift Left click We don't care about the
								 * cursor as it doesn't get changed on a shift
								 * click
								 */
								// Handle shift left click from inventory
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									plugin.getDatabaseHandler().removeItem(
											target, rawSlot);
								}
							}
							else
							{
								// Handle regular left click from inventory
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR)
										&& !event.getCursor().getType()
												.equals(Material.AIR))
								{
									if (event
											.getCurrentItem()
											.getType()
											.equals(event.getCursor().getType()))
									{
										/*
										 * Of the same time, so add to current
										 * stack
										 */
										final int cursorAmount = event
												.getCursor().getAmount();
										final int itemAmount = event
												.getCurrentItem().getAmount();
										int newAmount = itemAmount
												+ cursorAmount;
										if (newAmount > event.getCurrentItem()
												.getMaxStackSize())
										{
											newAmount = event.getCurrentItem()
													.getMaxStackSize();
										}
										plugin.getDatabaseHandler().setItem(
												target, rawSlot,
												event.getCurrentItem(),
												newAmount);
									}
									else
									{
										/*
										 * Switching items from chest to cursor
										 * When switching, put item first, then
										 * attempt to take item
										 */
										plugin.getDatabaseHandler().removeItem(
												target, rawSlot);
										plugin.getDatabaseHandler().setItem(
												target, rawSlot,
												event.getCursor());
									}
								}
								else if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									// Attempting to take item
									plugin.getDatabaseHandler().removeItem(
											target, rawSlot);
								}
								else if (!event.getCursor().getType()
										.equals(Material.AIR))
								{
									// putting item into empty slot in chest
									plugin.getDatabaseHandler().setItem(target,
											rawSlot, event.getCursor());
								}
							}
						}
						else if (event.isRightClick())
						{
							// Handle right clicks
							if (event.isShiftClick())
							{
								// Handle right shift click from inventory
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									plugin.getDatabaseHandler().removeItem(
											target, rawSlot);
								}
							}
							else
							{
								// handle regular right click from inventory
								if (!event.getCurrentItem().getType()
										.equals(Material.AIR)
										&& !event.getCursor().getType()
												.equals(Material.AIR))
								{
									if (event
											.getCurrentItem()
											.getType()
											.equals(event.getCursor().getType()))
									{
										/*
										 * Same item, so give only one from
										 * cursor to item
										 */
										final int itemAmount = event
												.getCurrentItem().getAmount();
										int newAmount = itemAmount + 1;
										if (newAmount > event.getCurrentItem()
												.getMaxStackSize())
										{
											newAmount = event.getCurrentItem()
													.getMaxStackSize();
										}
										plugin.getDatabaseHandler().setItem(
												target, rawSlot,
												event.getCurrentItem(),
												newAmount);
									}
									else
									{
										/*
										 * Switching Put item first, then
										 * attempt to take item
										 */
										plugin.getDatabaseHandler().removeItem(
												target, rawSlot);
										plugin.getDatabaseHandler().setItem(
												target, rawSlot,
												event.getCursor());
									}
								}
								else if (!event.getCurrentItem().getType()
										.equals(Material.AIR))
								{
									/*
									 * If cursor is air and item is not air
									 * they are taking half of the stack,
									 * with the larger half given to cursor
									 */
									// Calculate "half"
									int half = event.getCurrentItem()
											.getAmount() / 2;
									final double rem = (double) event
											.getCurrentItem().getAmount() % 2.0;
									if (rem != 0)
									{
										half++;
									}
									plugin.getDatabaseHandler().setItem(
											target, rawSlot,
											event.getCurrentItem(),
											half);
								}
								else if (!event.getCursor().getType()
										.equals(Material.AIR))
								{
									//Only give one
									final int itemAmount = event
											.getCurrentItem().getAmount();
									int newAmount = itemAmount - 1;
									if (newAmount > event.getCurrentItem()
											.getMaxStackSize())
									{
										newAmount = event.getCurrentItem()
												.getMaxStackSize();
									}
									plugin.getDatabaseHandler().setItem(
											target, rawSlot,
											event.getCurrentItem(),
											newAmount);
								}
							}
						}
					}
					else
					{
						//TODO not sure about the logic here... how would I know what slot it would go to?
						// Clicked in own inventory, only need to bother with
						// shift click
						if (event.isShiftClick())
						{
							event.setCancelled(true);
							/*if (event.isLeftClick())
							{
								// Handle shift left click to inventory
							}
							else if (event.isRightClick())
							{
								// Handle right left click to inventory
							}*/
						}
					}
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
				}
			}
			else if(invalid)
			{
				event.setCancelled(true);
			}
		}
	}
}
