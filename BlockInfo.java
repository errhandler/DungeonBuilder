package net.virtuallyabstract.minecraft;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.material.*;
import org.bukkit.entity.*;
import java.util.*;

public class BlockInfo implements Comparable<BlockInfo>
{
	private Block b;
	private Material m;
	private Byte data;
	private String metaStr = null;
	private Entity entity = null;

	public BlockInfo(Block b)
	{
		this.b = b;
		this.m = b.getType();
		this.data = b.getData();
	}

	public BlockInfo(Block b, Material m, Byte data)
	{
		this.b = b;
		this.m = m;
		this.data = data;
	}

	public void setBlock()
	{
		if(metaStr != null && metaStr.startsWith("ENTITY"))
		{
			setEntity();
			return;
		}

		if(m == null)
			return;

		b.setType(m);
			
		if(data != null)
			b.setData(data);
		
		if(metaStr != null)
		{
			BlockState bs = b.getState();
			if(bs instanceof ContainerBlock)
			{
				Dungeon.parseInventoryString((ContainerBlock)bs, metaStr);	
			}

			if(bs instanceof org.bukkit.block.Sign)
			{
				Dungeon.parseSignString((org.bukkit.block.Sign)bs, metaStr);
			}
		}
	}

	public Entity getEntity()
	{
		return entity;
	}
	
	public void setEntity(Entity e)
	{
		this.entity = e;
	}

	private void setEntity()
	{
		if(b == null)
			return;

		World w = b.getWorld();
		if(w == null)
			return;

		String clazz = metaStr.replace("ENTITY(", "");
		clazz = clazz.replace(")", "");

		if(clazz.contains("org.bukkit.craftbukkit.entity.CraftMinecart"))
			clazz = "org.bukkit.entity.Minecart";
		else if(clazz.contains("org.bukkit.craftbukkit.entity.CraftPoweredMinecart"))
			clazz = "org.bukkit.entity.PoweredMinecart";
		else if(clazz.contains("org.bukkit.craftbukkit.entity.CraftStorageMinecart"))
			clazz = "org.bukkit.entity.StorageMinecart";
		else
			return;

		try
		{
			if(entity != null)
			{
				entity.remove();
				entity = null;
			}

			@SuppressWarnings("unchecked")
			Class<? extends org.bukkit.entity.Entity> entityClass = (Class<? extends org.bukkit.entity.Entity>)Class.forName(clazz);
			entity = w.spawn(b.getLocation(), entityClass);
		}
		catch(Exception e)
		{
		}
	}

	public Block getBlock()
	{
		return b;
	}

	public Material getType()
	{
		return m;
	}

	public void setMetaString(String metaStr)
	{
		this.metaStr = metaStr;
	}

	public String getMetaString()
	{
		return metaStr;
	}

	public boolean isAttachable()
	{
		switch(m)
		{
			case TORCH:
			case LADDER:
			case IRON_DOOR_BLOCK:
			case LEVER:
			case RAILS:
			case POWERED_RAIL:
			case REDSTONE:
			case REDSTONE_TORCH_ON:
			case REDSTONE_TORCH_OFF:
			case REDSTONE_WIRE:
			case STONE_BUTTON:
			case TRAP_DOOR:
			case WALL_SIGN:
			case WOOD_DOOR:
			case WOODEN_DOOR:
			case SAPLING:
			case DETECTOR_RAIL:
			case WEB:
			case DEAD_BUSH:
			case LONG_GRASS:
			case YELLOW_FLOWER:
			case RED_ROSE:
			case BROWN_MUSHROOM:
			case RED_MUSHROOM:
			case CROPS:
			case STONE_PLATE:
			case WOOD_PLATE:
			case SUGAR_CANE_BLOCK:
			case DIODE_BLOCK_OFF:
			case DIODE_BLOCK_ON:
			case VINE:
			case STORAGE_MINECART:
			case POWERED_MINECART:
				return true;
			default:
				return false;
		}
	}

	public boolean isRedstoneTorch()
	{
		switch(m)
		{
			case REDSTONE_TORCH_ON:
			case REDSTONE_TORCH_OFF:
				return true;
			default:
				return false;
		}
	}

	public boolean isLiquid()
	{
		switch(m)
		{
			case LAVA:
			case STATIONARY_LAVA:
			case WATER:
			case STATIONARY_WATER:
				return true;
			default:
				return false;
		}
	}
	
	@Override public int compareTo(BlockInfo bi2)
	{
		boolean isLiquid = false, isLiquid2 = false;
		isLiquid = isLiquid();
		isLiquid2 = bi2.isLiquid();

		//Sugar cane needs to be loaded last since water needs to be present first
		if(m == Material.SUGAR_CANE_BLOCK && bi2.getType() != Material.SUGAR_CANE_BLOCK)
			return 1;
		if(m != Material.SUGAR_CANE_BLOCK && bi2.getType() == Material.SUGAR_CANE_BLOCK)
			return -1;

		//Load liquids last (but before sugar canes)
		if(isLiquid && !isLiquid2)
			return 1;
		if(!isLiquid && isLiquid2)
			return -1;

		//Load redstone torches first (before wires and other redstone related objects)
		if(isRedstoneTorch() && !bi2.isRedstoneTorch())
			return -1;
		if(!isRedstoneTorch() && bi2.isRedstoneTorch())
			return 1;

		//Everything else gets loaded based on if it is an attachable object or not.
		if(isLiquid == isLiquid2)
		{
			if(isAttachable() && bi2.isAttachable())
				return 0;
			if(isAttachable() && !bi2.isAttachable())
				return 1;
			if(!isAttachable() && bi2.isAttachable())
				return -1;
		}

		return 0;
	}
}
