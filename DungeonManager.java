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
import java.util.*;
import java.util.concurrent.*;

public class DungeonManager
{
	private int width = 3, depth, height = 4;
	private ConcurrentHashMap<String, ArrayList<LocationWrapper>> locationMap;
	private ConcurrentHashMap<String, ArrayList<LocationWrapper>> triggerMap;
	private Server server;

	public DungeonManager(Server s, ConcurrentHashMap<String, ArrayList<Dungeon>> dungeonMap)
	{
		server = s;
		locationMap = new ConcurrentHashMap<String, ArrayList<LocationWrapper>>();
		triggerMap = new ConcurrentHashMap<String, ArrayList<LocationWrapper>>();

		for(String key : dungeonMap.keySet())
		{
			for(Dungeon d : dungeonMap.get(key))
			{
				addDungeon(d);
			}
		}
	}

	public void addDungeon(Dungeon d)
	{
		addDungeon(d, false);
	}

	public void addDungeon(Dungeon d, boolean setMarkers)
	{
		if(!d.isPublished())
			return;

		if(d.getTeleporterLocation() != null && d.getStartingLocation() != null)
		{
			String key = WorldUtils.createLocationKey(d.getTeleporterLocation());
			LocationWrapper lwStart = new LocationWrapper(d.getStartingLocation(), LocationWrapper.LocationType.DUNGEON_START, d);
			if(!locationMap.containsKey(key))
				locationMap.put(key, new ArrayList<LocationWrapper>());

			locationMap.get(key).add(lwStart);

			Collections.sort(locationMap.get(key));

			if(setMarkers)
			{
				Location loc = d.getTeleporterLocation();
				Block b = loc.getBlock();
				b.setType(Material.TORCH);
			}
		}

		if(d.getExitLocation() != null && d.getTeleporterLocation() != null)
		{
			String key = WorldUtils.createLocationKey(d.getExitLocation());
			Location l = d.getExitDestination();
			LocationWrapper lwExit = new LocationWrapper(l, LocationWrapper.LocationType.DUNGEON_EXIT, d);
			if(!locationMap.containsKey(key))
				locationMap.put(key, new ArrayList<LocationWrapper>());

			locationMap.get(key).add(lwExit);

			if(setMarkers)
			{
				Block b = l.getBlock();
				b.setType(Material.REDSTONE_TORCH_ON);
			}
		}

		for(LocationWrapper lw : d.listMonsterTriggers())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					triggerMap.put(key, new ArrayList<LocationWrapper>());

				triggerMap.get(key).add(lw);
			}
		}

		for(LocationWrapper lw : d.listSavePoints())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					triggerMap.put(key, new ArrayList<LocationWrapper>());

				triggerMap.get(key).add(lw);
			}
		}

		for(LocationWrapper lw : d.listScriptTriggers())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					triggerMap.put(key, new ArrayList<LocationWrapper>());

				triggerMap.get(key).add(lw);
			}
		}
	}

	public void removeDungeon(Dungeon d)
	{
		if(d.getTeleporterLocation() != null)
		{
			String key = WorldUtils.createLocationKey(d.getTeleporterLocation());
			ArrayList<LocationWrapper> toRemove = new ArrayList<LocationWrapper>();
			if(locationMap.containsKey(key))
			{
				for(LocationWrapper lw : locationMap.get(key))
				{
					if(lw.getDungeon().equals(d))
						toRemove.add(lw);
				}
			}

			for(LocationWrapper lw : toRemove)
				locationMap.get(key).remove(lw);

			Collections.sort(locationMap.get(key));
		}

		if(d.getExitLocation() != null)
		{
			String key = WorldUtils.createLocationKey(d.getExitLocation());

			ArrayList<LocationWrapper> toRemove = new ArrayList<LocationWrapper>();
			if(locationMap.containsKey(key))
			{
				for(LocationWrapper lw : locationMap.get(key))
				{
					if(lw.getDungeon().equals(d))
						toRemove.add(lw);
				}
			}

			for(LocationWrapper lw : toRemove)
				locationMap.get(key).remove(lw);
		}

		for(LocationWrapper lw : d.listMonsterTriggers())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					return;

				triggerMap.get(key).remove(lw);
			}
		}

		for(LocationWrapper lw : d.listSavePoints())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					return;

				triggerMap.get(key).remove(lw);
			}
		}

		for(LocationWrapper lw : d.listScriptTriggers())
		{
			for(Location loc : lw.getTargetLocations())
			{
				String key = WorldUtils.createLocationKey(loc);
				if(!triggerMap.containsKey(key))
					return;

				triggerMap.get(key).remove(lw);
			}
		}
	}

	//public Location getStartingLocation()
	//{
	//	return world.getSpawnLocation();
	//}


	public ArrayList<LocationWrapper> checkLocation(Location loc)
	{
		String key = WorldUtils.createLocationKey(loc);
		ArrayList<LocationWrapper> retVal = new ArrayList<LocationWrapper>();

		if(locationMap.containsKey(key))
		{
			Collections.sort(locationMap.get(key));
			retVal.addAll(locationMap.get(key));
		}

		if(triggerMap.containsKey(key))
			retVal.addAll(triggerMap.get(key));

		return retVal;
	}
}
