package net.virtuallyabstract.minecraft;

import org.bukkit.plugin.java.*;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.material.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.util.*;

public class WorldUtils
{
	public static void createRoomNoWalls(Location loc, int width, int depth, int height, int floorType)
	{
		Location newLoc = loc.clone();

		int halfdepth = depth / 2;
		int halfwidth = width / 2;

		double xbase = loc.getX();
		double zbase = loc.getZ();
		double ybase = loc.getY() - 1;
		//Construct the walls and interior
		for(double y = 0.0; y <= height; y += 1.0)
		{
			newLoc.setY(ybase + y);
			for(double x = -halfdepth; x <= halfdepth; x += 1.0)
			{
				newLoc.setX(xbase + x);
				for(double z = -halfwidth; z <= halfwidth; z += 1.0)
				{
					newLoc.setZ(zbase + z);	
					if(y == 0.0)
					{
						if(x >= -halfdepth && x <= halfdepth)
						{
							if(z >= -halfwidth && z <= halfwidth)
							{
								newLoc.getBlock().setTypeId(floorType);

								continue;
							}
						}
					}

					newLoc.getBlock().setType(Material.AIR);
				}
			}
		}
	}

	public static ArrayList<BlockInfo> createRoom(Location loc, int width, int depth, int height, int type, boolean hollow)
	{
		ArrayList<BlockInfo> retVal = new ArrayList<BlockInfo>();

		Location newLoc = loc.clone();

		int halfdepth = depth / 2;
		int halfwidth = width / 2;

		double xbase = loc.getX();
		double zbase = loc.getZ();
		double ybase = loc.getY() - 1;
		//Construct the walls and interior
		for(double y = 0.0; y <= height; y += 1.0)
		{
			newLoc.setY(ybase + y);
			for(double x = -halfdepth; x <= halfdepth; x += 1.0)
			{
				newLoc.setX(xbase + x);
				for(double z = -halfwidth; z <= halfwidth; z += 1.0)
				{
					newLoc.setZ(zbase + z);	
					if(y > 0.0 && y < height)
					{
						if(x > -halfdepth && x < halfdepth)
						{
							if(z > -halfwidth && z < halfwidth)
							{
								if(hollow)
								{
									BlockInfo bi = new BlockInfo(newLoc.getBlock());
									retVal.add(bi);
									newLoc.getBlock().setType(Material.AIR);
								}

								continue;
							}
						}
					}

					BlockInfo bi = new BlockInfo(newLoc.getBlock());
					retVal.add(bi);
					if(type != -1)
						newLoc.getBlock().setTypeId(type);
				}
			}
		}

		if(!hollow || type == 20)
			return retVal;

		//Lets add some torches
		for(double y = 1.0; y <= height-1; y += 1.0)
		{
			newLoc.setY(ybase + y);
			for(double x = -halfdepth+1; x <= halfdepth-1; x += 1.0)
			{
				newLoc.setX(xbase + x);
				for(double z = -halfwidth+1; z <= halfwidth-1; z += 1.0)
				{
					newLoc.setZ(zbase + z);	
					if(y > 0.0 && y < height && y % 3 == 0)
					{
						if((x == -halfdepth+1 || x == halfdepth-1) && z % 3 == 0)
						{
							newLoc.getBlock().setType(Material.TORCH);

							continue;
						}

						if((z == -halfwidth+1 || z == halfwidth-1) && x % 3 == 0)
						{
							newLoc.getBlock().setType(Material.TORCH);

							continue;
						}
					}
					if(y == 1.0 && z % 3 == 0 && x % 3 == 0)
					{
						newLoc.getBlock().setType(Material.TORCH);

						continue;
					}
				}
			}
		}

		return retVal;
	}

	public static String createLocationKey(Location loc)
	{
		String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockZ() + ":" + loc.getBlockY();

		return key;
	}
}
