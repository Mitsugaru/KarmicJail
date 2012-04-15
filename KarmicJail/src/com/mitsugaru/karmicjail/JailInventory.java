package com.mitsugaru.karmicjail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mitsugaru.utils.Item;

public class JailInventory implements DoubleChestInventory
{
	private KarmicJail plugin;
	private String owner;
	public List<HumanEntity> viewers = new ArrayList<HumanEntity>();
	public HashMap<Integer, Item> items = new HashMap<Integer, Item>();
	
	
	public JailInventory(KarmicJail plugin, String jailedPlayer, Map<Integer, ItemStack> items)
	{
		this.plugin = plugin;
		this.owner = jailedPlayer;
		for(Map.Entry<Integer, ItemStack> entry : items.entrySet())
		{
			this.items.put(entry.getKey(), new Item(entry.getValue()));
		}
	}

	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack... items)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("addItem(ItemStack... items) was called");
		return new HashMap<Integer, ItemStack>();
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(int materialId)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("all(int materialId was called");
		return new HashMap<Integer, ItemStack>();
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(Material material)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("all(Material material) was called");
		return new HashMap<Integer, ItemStack>();
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(ItemStack item)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("all(ItemStack item) was called");
		return new HashMap<Integer, ItemStack>();
	}

	@Override
	public void clear()
	{
		// TODO remove items from database and viewer?
		plugin.getLogger().info("clear() was called");
	}

	@Override
	public void clear(int index)
	{
		// TODO remove item of slot from database and viewer
		plugin.getLogger().info("clear(int index) was called");
	}

	@Override
	public boolean contains(int materialId)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(int materialId) was called");
		return false;
	}

	@Override
	public boolean contains(Material material)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(Material material) was called");
		return false;
	}

	@Override
	public boolean contains(ItemStack item)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(ItemStack item) was called");
		return false;
	}

	@Override
	public boolean contains(int materialId, int amount)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(int materialId, int amount) was called");
		return false;
	}

	@Override
	public boolean contains(Material material, int amount)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(Material material, int amount) was called");
		return false;
	}

	@Override
	public boolean contains(ItemStack item, int amount)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("contains(ItemStack item, int amount) was called");
		return false;
	}

	@Override
	public int first(int materialId)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("first(int materialId) was called");
		return -999;
	}

	@Override
	public int first(Material material)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("first(Material material) was called");
		return -999;
	}

	@Override
	public int first(ItemStack item)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("first(ItemStack item) was called");
		return -999;
	}

	@Override
	public int firstEmpty()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("firstEmpty() was called");
		return 0;
	}

	@Override
	public ItemStack[] getContents()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getContents() was called");
		return null;
	}

	@Override
	public ItemStack getItem(int index)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getItem(int index) was called");
		return null;
	}

	@Override
	public int getMaxStackSize()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getMaxStackSize() was called");
		return 0;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getName() was called");
		return null;
	}

	@Override
	public int getSize()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getSize() was called");
		return 0;
	}

	@Override
	public String getTitle()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getTitle() was called");
		return null;
	}

	@Override
	public InventoryType getType()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getType() was called");
		return null;
	}

	@Override
	public List<HumanEntity> getViewers()
	{
		plugin.getLogger().info("getViewers was called");
		return viewers;
	}

	@Override
	public ListIterator<ItemStack> iterator()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("iterator() was called");
		return null;
	}

	@Override
	public ListIterator<ItemStack> iterator(int index)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("iterator(int index) was called");
		return null;
	}

	@Override
	public void remove(int materialId)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("remove(int materialId) was called");
	}

	@Override
	public void remove(Material material)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("remove(Material material) was called");
	}

	@Override
	public void remove(ItemStack item)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("remove(ItemStack item) was called");
	}

	@Override
	public HashMap<Integer, ItemStack> removeItem(ItemStack... items)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("removeItem(ItemStack... items) was called");
		return null;
	}

	@Override
	public void setContents(ItemStack[] items)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("setContents(ItemStack[] items) was called");
	}

	@Override
	public void setItem(int index, ItemStack item)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("set(int index, ItemStack item) was called");
	}

	@Override
	public void setMaxStackSize(int size)
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("setMaxStackSize(int size) was called");
	}

	@Override
	public DoubleChest getHolder()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getHolder() was called");
		return null;
	}

	@Override
	public Inventory getLeftSide()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getLeftSide() was called");
		return null;
	}

	@Override
	public Inventory getRightSide()
	{
		// TODO Auto-generated method stub
		plugin.getLogger().info("getRightSide() was called");
		return null;
	}

}
