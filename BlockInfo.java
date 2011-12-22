package net.virtuallyabstract.minecraft;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.material.*;
import java.util.*;

public class BlockInfo implements Comparable<BlockInfo>
{
	private Block b;
	private Material m;
	private Byte data;
	private String metaStr = null;

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

		if(isRedstoneTorch() && !bi2.isRedstoneTorch())
			return -1;
		if(!isRedstoneTorch() && bi2.isRedstoneTorch())
			return 1;

		if(isLiquid && !isLiquid2)
			return 1;
		if(!isLiquid && isLiquid2)
			return -1;
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
