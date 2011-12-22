package net.virtuallyabstract.minecraft;

import org.bukkit.entity.*;
import org.bukkit.*;
import java.util.*;

public class DungeonParty
{
	private String leader;
	private Server server;
	private Dungeon activeDungeon;

	private HashSet<String> members;

	private DungeonParty()
	{
	}

	public DungeonParty(String leader, Server server)
	{
		this.leader = leader;
		this.server = server;
		members = new HashSet<String>();
		members.add(leader);
	}

	public void addMember(String member)
	{
		members.add(member);
	}

	public void removeMember(String member)
	{
		members.remove(member);
	}

	public boolean containsMember(Player member)
	{
		return containsMember(member.getName());
	}

	public boolean containsMember(String member)
	{
		if(members.contains(member))
			return true;
		else
			return false;
	}

	public Set<String> listMembers()
	{
		//HashSet<Player> retVal = new HashSet<Player>();
		//for(String playername : members)
		//{
		//	if(server.getPlayer(playername) != null)
		//		retVal.add(server.getPlayer(playername));
		//}

		//return retVal;
		return new HashSet<String>(members);
	}

	public int getSize()
	{
		return members.size();
	}

	public String getLeader()
	{
		return leader;
	}

	public void setDungeon(Dungeon d)
	{
		activeDungeon = d;
	}

	public Dungeon getDungeon()
	{
		return activeDungeon;
	}

	@Override public boolean equals(Object obj2)
	{
		if(!(obj2 instanceof DungeonParty))
			return false;

		DungeonParty party2 = (DungeonParty)obj2;

		if(getSize() != party2.getSize())
			return false;

		for(String playername : members)
		{
			if(!party2.containsMember(playername))
				return false;	
		}

		return true;
	}
}
