/**
 * Class to represent items that are in the pool Mostly used to help
 * differentiate between items that use damage values
 */
package com.mitsugaru.karmicjail.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public class Item extends MaterialData
{
	// Class variables
	private short durability;
	private String enchantments = "";

	public Item(ItemStack i)
	{
		super(i.getTypeId(), i.getData().getData());
		init(i);
	}

	/**
	 * Constructor
	 * 
	 * @param int of item id
	 * @param byte of data value
	 */
	public Item(int i, byte d, short dur)
	{
		super(i, d);
		durability = dur;
	}

	private void init(ItemStack item)
	{
		durability = item.getDurability();
		enchantments = getItemEnchantments(item);
	}

	@Override
	public boolean equals(Object obj)
	{
		return super.equals(obj);
	}

	/**
	 * Custom hashcode method to provide proper Item class equals check.
	 * Especially useful for potions, since data values are the same, but
	 * durability is not.
	 * 
	 * @return Object's hashcode
	 */
	@Override
	public int hashCode()
	{
		int hash = 0;
		hash += this.getItemTypeId();
		hash += this.getData();
		hash += this.getDurability();
		if (!enchantments.equals(""))
		{
			hash += enchantments.hashCode();
		}
		return hash;
	}

	/**
	 * Variant of equals(Object obj)
	 * 
	 * @param obj
	 * @return true if they are the same item
	 */
	public boolean areSame(Object obj)
	{
		// Both blocks
		try
		{
			if (this.getItemType().isBlock()
					&& ((Item) obj).getItemType().isBlock())
			{
				// Check both id and data values
				if (this.getItemTypeId() == ((Item) obj).getItemTypeId())
				{
					if (this.getId() == 9)
					{
						// Ignore data for leaves
						return true;
					}
					if (this.getData() == ((Item) obj).getData())
					{
						return true;
					}
				}
			}
			else if (!this.getItemType().isBlock()
					&& !((Item) obj).getItemType().isBlock())
			{
				// Both non-block, only check item id
				if (this.getItemTypeId() == ((Item) obj).getItemTypeId())
				{
					// handle if dye or potion
					if (this.getId() == 351)
					{
						if (this.getData() == ((Item) obj).getData())
						{
							return true;
						}
					}
					else if (this.getId() == 373)
					{
						if (durability == ((Item) obj).getDurability())
						{
							return true;
						}
					}
					else
						return true;
				}
			}
		}
		catch (ClassCastException e)
		{
			// Cast failed, so, they're not the same object
			return false;
		}
		return false;
	}

	public ItemStack toItemStack()
	{
		ItemStack item = null;
		if (isPotion())
		{
			item = new ItemStack(super.getItemTypeId(), 1, durability);
		}
		else if (isTool())
		{
			item = new ItemStack(super.getItemType(), 1, super.getData());
			item = addEnchantments(enchantments, item);
		}
		else
		{
			item = new ItemStack(super.getItemType(), 1, super.getData());
		}
		return item;
	}

	/**
	 * Method to check if the item is a potion/glass bottle
	 * 
	 * @return true if potion, else false;
	 */
	public boolean isPotion()
	{
		return Item.isPotion(this.getId());
	}

	public static boolean isPotion(int id)
	{
		if (id == 373 || id == 374)
		{
			return true;
		}
		return false;
	}

	/**
	 * Method to check if this item is a tool
	 * 
	 * @return true if its is a tool item
	 */
	public boolean isTool()
	{
		return Item.isTool(this.getItemTypeId());
	}

	/**
	 * Method that checks a given id to see if its a tool
	 * 
	 * @param int of item id
	 * @return true if it matches a known tool, else false
	 */
	public static boolean isTool(int id)
	{
		final int[] tool = { 256, 257, 258, 259, 261, 267, 268, 269, 270, 271,
				272, 273, 274, 275, 276, 277, 278, 279, 283, 284, 285, 286,
				290, 291, 292, 293, 294, 298, 299, 300, 301, 302, 303, 304,
				305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317 };
		if (id >= 256 && id <= 317)
		{
			// within range of "tool" ids
			for (int i = 0; i < tool.length; i++)
			{
				// iterate through array to see if we get a match
				if (id == tool[i])
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method will convert an item's enchantments into a sorted string.
	 * 
	 * @param item
	 *            - ItemStack to read enchantments from
	 * @return String representing the sorted enchantments
	 */
	public static String getItemEnchantments(ItemStack item)
	{
		if(item.getEnchantments().isEmpty())
		{
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		final Map<ComparableEnchantment, Integer> map = new HashMap<ComparableEnchantment, Integer>();
		for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments()
				.entrySet())
		{
			map.put(new ComparableEnchantment(entry.getKey()), entry.getValue());
		}
		final TreeSet<ComparableEnchantment> keys = new TreeSet<ComparableEnchantment>(
				map.keySet());
		for (ComparableEnchantment key : keys)
		{
			sb.append(key.getId()
					+ "v"
					+ item.getEnchantments().get(key.getEnchantment())
							.intValue() + "i");
		}
		// Remove trailing comma
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Given a proper enchantment string, it will parse the string and add the
	 * enchantments to the given item.
	 * 
	 * @param enchant
	 *            - Enchantment string
	 * @param item
	 *            - ItemStack to add enchantments to
	 * @return The ItemStack given, with the enchantments attacked
	 */
	public static ItemStack addEnchantments(String enchant, ItemStack item)
	{
		if (item == null)
		{
			return null;
		}
		else if (enchant == null || enchant.equals(""))
		{
			return item;
		}
		final String[] cut = enchant.split("i");
		for (int s = 0; s < cut.length; s++)
		{
			try
			{
				final String[] cutter = cut[s].split("v");
				final EnchantmentWrapper e = new EnchantmentWrapper(
						Integer.parseInt(cutter[0]));
				item.addUnsafeEnchantment(e.getEnchantment(),
						Integer.parseInt(cutter[1]));
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				// Ignore, poorly formated enchantment string
			}

		}
		return item;
	}

	/**
	 * Grabs the item id of this Item object
	 * 
	 * @return item id
	 */
	public int getId()
	{
		return this.getItemType().getId();
	}

	/**
	 * Grabs the durability value of this Item object
	 */
	public short getDurability()
	{
		return this.durability;
	}
}
