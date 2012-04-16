package com.mitsugaru.karmicjail;

import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Displays a jailed user's inventory.
 * 
 * @author Tokume
 * 
 */
public class JailInventoryHolder implements InventoryHolder
{
	private KarmicJail plugin;
	private Inventory inventory;
	private String target;

	public JailInventoryHolder(KarmicJail plugin, String target)
	{
		this.plugin = plugin;
		this.target = target;
	}

	public void setInventory(Inventory inventory)
	{
		this.inventory = inventory;
		// Import inventory from database
		final Map<Integer, ItemStack> targetInv = plugin.getDatabaseHandler()
				.getPlayerItems(target);
		for (Map.Entry<Integer, ItemStack> entry : targetInv.entrySet())
		{
			inventory.setItem(entry.getKey().intValue(), entry.getValue());
		}
	}

	@Override
	public Inventory getInventory()
	{
		return inventory;
	}
	
	public String getTarget()
	{
		return target;
	}
}