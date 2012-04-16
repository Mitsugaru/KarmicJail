package com.mitsugaru.karmicjail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

@Deprecated
public class JailInventory implements Inventory
{
	KarmicJail plugin;
	private String target;
	private int maxstack = 64;
	private ItemStack[] storage = new ItemStack[44];
	private List<HumanEntity> viewers = new ArrayList<HumanEntity>();
	
	public JailInventory(KarmicJail plugin, String target)
	{
		this.plugin = plugin;
		this.target = target;
	}
	
	public void onOpen(HumanEntity player)
	{
		viewers.add(player);
	}
	
	public void onClose(HumanEntity player)
	{
		viewers.remove(player);
	}
	
	@Override
	public void setMaxStackSize(int size)
	{
		maxstack = size;
		plugin.getLogger().info("getMaxStackSize()");
	}

	@Override
	public void setItem(int index, ItemStack item)
	{
		plugin.getLogger().info("setItem(" + index +", " + item.toString() + ")");
		if (index <= 0 && index >= 44)
		{
			// What happens to old item?
			storage[index] = item;
		}
	}

	@Override
	public void setContents(ItemStack[] items)
	{
		for (int i = 0; i < storage.length; i++)
		{
			storage[i] = items[i];
		}
		plugin.getLogger().info("setContents()");
	}

	@Override
	public HashMap<Integer, ItemStack> removeItem(ItemStack... items)
	{
		HashMap<Integer, ItemStack> notRemoved = new HashMap<Integer, ItemStack>();
		int count = 0;
		for (ItemStack item : items)
		{
			boolean removed = false;
			for (int i = 0; i < storage.length; i++)
			{
				try
				{
					if (storage[i].equals(item))
					{
						removed = true;
						storage[i] = null;
						break;
					}
				}
				catch (NullPointerException n)
				{
					// Ignore
				}
			}
			if (!removed)
			{
				notRemoved.put(new Integer(count), item);
				count++;
			}
		}
		plugin.getLogger().info("removeItem()");
		return notRemoved;
	}

	@Override
	public void remove(ItemStack item)
	{
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].equals(item))
				{
					storage[i] = null;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		plugin.getLogger().info("remove(item)");
	}

	@Override
	public void remove(Material material)
	{
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getType().equals(material))
				{
					storage[i] = null;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		plugin.getLogger().info("remove(material)");
	}

	@Override
	public void remove(int materialId)
	{
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getTypeId() == materialId)
				{
					storage[i] = null;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		plugin.getLogger().info("remove(materialId)");
	}

	@Override
	public ListIterator<ItemStack> iterator(final int index)
	{
		return new ListIterator<ItemStack>() {
			private int count = index;

			@Override
			public void add(ItemStack e)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean hasNext()
			{
				if (count < storage.length)
				{
					return true;
				}
				return false;
			}

			@Override
			public boolean hasPrevious()
			{
				if (count > 0)
				{
					return true;
				}
				return false;
			}

			@Override
			public ItemStack next()
			{
				if (count >= storage.length)
				{
					throw new NoSuchElementException();
				}
				return storage[count++];
			}

			@Override
			public int nextIndex()
			{
				return (count + 1);
			}

			@Override
			public ItemStack previous()
			{
				if (count <= 0)
				{
					throw new NoSuchElementException();
				}
				return storage[count--];
			}

			@Override
			public int previousIndex()
			{
				return (count - 1);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(ItemStack e)
			{
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public ListIterator<ItemStack> iterator()
	{
		return iterator(0);
	}

	@Override
	public List<HumanEntity> getViewers()
	{
		plugin.getLogger().info("getViewers()");
		return viewers;
	}

	@Override
	public InventoryType getType()
	{
		plugin.getLogger().info("getType()");
		return InventoryType.CHEST;
	}

	@Override
	public String getTitle()
	{
		plugin.getLogger().info("getTitle()");
		return target;
	}

	@Override
	public int getSize()
	{
		plugin.getLogger().info("getSize()");
		return storage.length;
	}

	@Override
	public String getName()
	{
		plugin.getLogger().info("getName()");
		return target;
	}

	@Override
	public int getMaxStackSize()
	{
		plugin.getLogger().info("getMaxStackSize()");
		return maxstack;
	}

	@Override
	public ItemStack getItem(int index)
	{
		plugin.getLogger().info("getItem()");
		try
		{
			return storage[index];
		}
		catch (ArrayIndexOutOfBoundsException a)
		{
			return null;
		}
	}

	@Override
	public InventoryHolder getHolder()
	{
		plugin.getLogger().info("getHolder()");
		return null;
	}

	@Override
	public ItemStack[] getContents()
	{
		plugin.getLogger().info("getContents()");
		return storage;
	}

	@Override
	public int firstEmpty()
	{
		plugin.getLogger().info("firstEmpty");
		int empty = -999;
		for (int i = 0; i < storage.length; i++)
		{
			if (storage[i] == null)
			{
				empty = i;
				break;
			}
		}
		return empty;
	}

	@Override
	public int first(ItemStack item)
	{
		plugin.getLogger().info("first(item)");
		int first = -999;
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].equals(item))
				{
					first = i;
					break;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return first;
	}

	@Override
	public int first(Material material)
	{
		plugin.getLogger().info("first(material)");
		int first = -999;
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getType().equals(material))
				{
					first = i;
					break;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return first;
	}

	@Override
	public int first(int materialId)
	{
		plugin.getLogger().info("first(id)");
		int first = -999;
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getTypeId() == materialId)
				{
					first = i;
					break;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return first;
	}

	@Override
	public boolean contains(ItemStack item, int amount)
	{
		plugin.getLogger().info("contains(item, int)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].equals(item))
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public boolean contains(Material material, int amount)
	{
		plugin.getLogger().info("contains(material, int)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getType().equals(material)
						&& storage[i].getAmount() >= amount)
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public boolean contains(int materialId, int amount)
	{
		plugin.getLogger().info("contains(id, amount)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getTypeId() == materialId
						&& storage[i].getAmount() >= amount)
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public boolean contains(ItemStack item)
	{
		plugin.getLogger().info("contains(item)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].equals(item))
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public boolean contains(Material material)
	{
		plugin.getLogger().info("contains(material)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getType().equals(material))
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public boolean contains(int materialId)
	{
		plugin.getLogger().info("contains(id)");
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getTypeId() == materialId)
				{
					return true;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return false;
	}

	@Override
	public void clear(int index)
	{
		plugin.getLogger().info("clear(int)");
		try
		{
			storage[index] = null;
			//TODO clear from database
		}
		catch (ArrayIndexOutOfBoundsException a)
		{
			// Ignore
		}
	}

	@Override
	public void clear()
	{
		plugin.getLogger().info("clear");
		for (int i = 0; i < storage.length; i++)
		{
			storage[i] = null;
			//TODO clear form database
		}
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(ItemStack item)
	{
		plugin.getLogger().info("all(item)");
		HashMap<Integer, ItemStack> found = new HashMap<Integer, ItemStack>();
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].equals(item))
				{
					found.put(new Integer(i), storage[i]);
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return found;
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(Material material)
	{
		plugin.getLogger().info("all(material)");
		HashMap<Integer, ItemStack> found = new HashMap<Integer, ItemStack>();
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getType().equals(material))
				{
					found.put(new Integer(i), storage[i]);
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return found;
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(int materialId)
	{
		plugin.getLogger().info("all(id)");
		HashMap<Integer, ItemStack> found = new HashMap<Integer, ItemStack>();
		for (int i = 0; i < storage.length; i++)
		{
			try
			{
				if (storage[i].getTypeId() == materialId)
				{
					found.put(new Integer(i), storage[i]);
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return found;
	}

	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack... items)
	{
		plugin.getLogger().info("addItem(items)");
		HashMap<Integer, ItemStack> notAdded = new HashMap<Integer, ItemStack>();
		int count = 0;
		int first = firstEmpty();
		for (ItemStack item : items)
		{
			try
			{
				if (contains(item))
				{
					int index = first(item);
					if ((storage[index].getAmount() + item.getAmount()) > maxstack)
					{
						item.setAmount(maxstack - storage[index].getAmount());
						storage[index].setAmount(maxstack);
						notAdded.put(new Integer(count), item);
					}
					else
					{
						storage[index].setAmount(storage[index].getAmount()
								+ item.getAmount());
					}
				}
				else if (first >= 0)
				{
					storage[first] = item;
					if (item.getAmount() > maxstack)
					{
						storage[first].setAmount(maxstack);
						item.setAmount(item.getAmount() - maxstack);
						notAdded.put(new Integer(count), item);
						count++;
					}
					first = firstEmpty();
				}
				else
				{
					notAdded.put(new Integer(count), item);
					count++;
				}
			}
			catch (NullPointerException n)
			{
				// Ignore
			}
		}
		return notAdded;
	}

}
