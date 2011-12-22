package net.virtuallyabstract.minecraft;

import org.bukkit.Location;
import java.util.*;

public class LocationWrapper implements Comparable<LocationWrapper>
{
	private ArrayList<Location> targetLocations = new ArrayList<Location>();
	private LocationType type;
	private Dungeon d;
	private String metadata = "", alias;
	private boolean active = true;
	private int blockMax = 1;

	public enum LocationType
	{
		DUNGEON_START, DUNGEON_EXIT, SCRIPT_TRIGGER, MONSTER_TRIGGER, SAVE_POINT;
	}

	private LocationWrapper()
	{
	}

	public LocationWrapper(Dungeon d, String line)
		throws Exception
	{
		this(d, line, false);
	}

	public LocationWrapper(Dungeon d, String line, boolean relative)
		throws Exception
	{
		if(line.startsWith("MT:"))
			this.type = LocationType.MONSTER_TRIGGER;
		else if(line.startsWith("ST:"))
			this.type = LocationType.SCRIPT_TRIGGER;
		else if(line.startsWith("SP:"))
			this.type = LocationType.SAVE_POINT;
		else
			throw new Exception("Invalid line");

		line = line.substring(3);
		String [] comps = line.split(",");
		if(comps.length < 4)
			throw new Exception("Invalid line - Missing components");

		this.d = d;
		this.alias = comps[0];
		int i;
		for(i = 1; i < comps.length - 1; i+=3)
		{
			targetLocations.add(d.createLocation(comps[i], comps[i+1], comps[i+2], relative));
		}
		if(i < comps.length)
			this.metadata = comps[comps.length-1];
		else
			this.metadata = "";
	}

	public LocationWrapper(Location l, LocationType type, Dungeon d)
	{
		switch(type)
		{
			case DUNGEON_START:
				alias = d.getName() + "_DUNGEON_START";
				targetLocations.add(l);
				this.type = type;
				this.d = d;
				break;
			case DUNGEON_EXIT:
				alias = d.getName() + "_DUNGEON_EXIT";
				targetLocations.add(l);
				this.type = type;
				this.d = d;
				break;
			default:
				alias = d.getName();
				targetLocations.add(l);
				this.type = type;
				this.d = d;
				break;
		}
	}

	public LocationWrapper(String alias, Location l, LocationType type, Dungeon d)
	{
		this.type = type;
		targetLocations.add(l);
		this.d = d;
		this.alias = alias;
	}

	public void setBlockCount(int count)
	{
		blockMax = count;
	}

	public int remainingLocations()
	{
		return blockMax - targetLocations.size();
	}

	public boolean addLocation(Location l)
	{
		String key = WorldUtils.createLocationKey(l);

		for(Location l2 : targetLocations)
		{
			String key2 = WorldUtils.createLocationKey(l2);
			if(key.equals(key2))
				return false;
		}

		targetLocations.add(l);

		return true;
	}

	public Location getTargetLocation()
	{
		return targetLocations.get(0);
	}

	public ArrayList<Location> getTargetLocations()
	{
		return new ArrayList<Location>(targetLocations);
	}

	public LocationType getType()
	{
		return type;
	}

	public Dungeon getDungeon()
	{
		return d;
	}

	public String getMetaData()
	{
		return metadata;
	}

	public void setMetaData(String data)
	{
		this.metadata = data;
	}

	public String getAlias()
	{
		return alias;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public boolean isActive()
	{
		return active;
	}

	@Override public boolean equals(Object obj)
	{
		if(!(obj instanceof LocationWrapper))
			return false;

		LocationWrapper lw2 = (LocationWrapper)obj;

		return alias.equals(lw2.getAlias());
	}

	@Override public String toString()
	{
		return toString(false);
	}

	public String toString(boolean relative)
	{
		StringBuffer retVal = new StringBuffer();
		switch(type)
		{
			case MONSTER_TRIGGER:
				retVal.append("MT:");
				break;
			case SCRIPT_TRIGGER:
				retVal.append("ST:");
				break;
			case SAVE_POINT:
				retVal.append("SP:");
				break;
			default:
				return "";
		}

		retVal.append(alias + ",");
		for(Location targetLocation : targetLocations)
		{
			retVal.append(d.createLocationString(targetLocation, relative) + ",");
		}
		retVal.append(metadata);

		return retVal.toString();
	}

	public int compareToReverse(LocationWrapper l2)
	{
		LocationType t1 = getType();
		LocationType t2 = l2.getType();

		if(t1 != LocationType.DUNGEON_START && t2 == LocationType.DUNGEON_START)
			return 1;
		if(t2 != LocationType.DUNGEON_START && t1 == LocationType.DUNGEON_START)
			return -1;
		if(t1 != LocationType.DUNGEON_START && t2 != LocationType.DUNGEON_START)
			return getAlias().compareTo(l2.getAlias());

		Dungeon d1 = getDungeon();
		Dungeon d2 = l2.getDungeon();
		Dungeon.PartyStatus status1 = d1.getStatus();
		Dungeon.PartyStatus status2 = d2.getStatus();

		System.out.println(status1.toString());
		System.out.println(status2.toString());

		if(status1 == Dungeon.PartyStatus.READY && status2 != Dungeon.PartyStatus.READY)
			return 1;
		if(status2 == Dungeon.PartyStatus.READY && status1 != Dungeon.PartyStatus.READY)
			return -1;
		if(status1 == Dungeon.PartyStatus.READY && status2 == Dungeon.PartyStatus.READY)
			return getAlias().compareTo(l2.getAlias());

		if(status1 == Dungeon.PartyStatus.MISSING_PLAYERS && status2 != Dungeon.PartyStatus.MISSING_PLAYERS)
			return 1;
		if(status2 == Dungeon.PartyStatus.MISSING_PLAYERS && status1 != Dungeon.PartyStatus.MISSING_PLAYERS)
			return -1;
		if(status1 == Dungeon.PartyStatus.MISSING_PLAYERS && status2 == Dungeon.PartyStatus.MISSING_PLAYERS)
			return getAlias().compareTo(l2.getAlias());

		if(status1 == Dungeon.PartyStatus.EMPTY && status2 != Dungeon.PartyStatus.EMPTY)
			return 1;
		if(status2 == Dungeon.PartyStatus.EMPTY && status1 != Dungeon.PartyStatus.EMPTY)
			return -1;
		if(status1 == Dungeon.PartyStatus.EMPTY && status2 == Dungeon.PartyStatus.EMPTY)
			return getAlias().compareTo(l2.getAlias());

		return getAlias().compareTo(l2.getAlias());
	}

	public int compareTo(LocationWrapper l2)
	{
		LocationType t1 = getType();
		LocationType t2 = l2.getType();

		if(t1 != LocationType.DUNGEON_START && t2 == LocationType.DUNGEON_START)
			return -1;
		if(t2 != LocationType.DUNGEON_START && t1 == LocationType.DUNGEON_START)
			return 1;
		if(t1 != LocationType.DUNGEON_START && t2 != LocationType.DUNGEON_START)
			return getAlias().compareTo(l2.getAlias());

		Dungeon d1 = getDungeon();
		Dungeon d2 = l2.getDungeon();
		Dungeon.PartyStatus status1 = d1.getStatus();
		Dungeon.PartyStatus status2 = d2.getStatus();

		if(status1 == Dungeon.PartyStatus.READY && status2 != Dungeon.PartyStatus.READY)
			return -1;
		if(status2 == Dungeon.PartyStatus.READY && status1 != Dungeon.PartyStatus.READY)
			return 1;
		if(status1 == Dungeon.PartyStatus.READY && status2 == Dungeon.PartyStatus.READY)
			return getAlias().compareTo(l2.getAlias());

		if(status1 == Dungeon.PartyStatus.MISSING_PLAYERS && status2 != Dungeon.PartyStatus.MISSING_PLAYERS)
			return -1;
		if(status2 == Dungeon.PartyStatus.MISSING_PLAYERS && status1 != Dungeon.PartyStatus.MISSING_PLAYERS)
			return 1;
		if(status1 == Dungeon.PartyStatus.MISSING_PLAYERS && status2 == Dungeon.PartyStatus.MISSING_PLAYERS)
			return getAlias().compareTo(l2.getAlias());

		if(status1 == Dungeon.PartyStatus.EMPTY && status2 != Dungeon.PartyStatus.EMPTY)
			return -1;
		if(status2 == Dungeon.PartyStatus.EMPTY && status1 != Dungeon.PartyStatus.EMPTY)
			return 1;
		if(status1 == Dungeon.PartyStatus.EMPTY && status2 == Dungeon.PartyStatus.EMPTY)
			return getAlias().compareTo(l2.getAlias());

		return getAlias().compareTo(l2.getAlias());
	}
}
