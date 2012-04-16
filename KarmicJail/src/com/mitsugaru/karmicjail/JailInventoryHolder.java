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
	KarmicJail plugin;
	

	JailInventory inventory;

	public JailInventoryHolder(KarmicJail plugin, String target)
	{
		inventory = new JailInventory(plugin, target);
		//Import inventory from database
		final Map<Integer, ItemStack> targetInv = plugin.getDatabaseHandler().getPlayerItems(target);
		for(Map.Entry<Integer, ItemStack> entry : targetInv.entrySet())
		{
			inventory.setItem(entry.getKey().intValue(), entry.getValue());
		}
	}

	@Override
	public Inventory getInventory()
	{
		// TODO Auto-generated method stub
		return inventory;
	}
	
	

}