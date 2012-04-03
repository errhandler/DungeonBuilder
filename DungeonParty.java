package net.virtuallyabstract.minecraft;

import com.herocraftonline.heroes.characters.*;
import com.herocraftonline.heroes.characters.party.*;
import org.bukkit.entity.*;
import org.bukkit.*;
import java.util.*;

public class DungeonParty
{
	private String leader;
	private Server server;
	private Dungeon activeDungeon;
	private HeroParty heroParty;

	private HashSet<String> members;

	private DungeonParty()
	{
	}

	public DungeonParty(HeroParty hparty)
	{
		heroParty = hparty;
	}

	public DungeonParty(String leader, Server server)
	{
		heroParty = null;
		this.leader = leader;
		this.server = server;
		members = new HashSet<String>();
		members.add(leader);
	}

	public void addMember(String member)
	{
		if(heroParty != null)
		{
			Player p = server.getPlayer(member);
			CharacterManager cm = new CharacterManager(DungeonBuilder.heroesPlugin);
			Hero h = cm.getHero(p);
			heroParty.addMember(h);

			return;
		}

		members.add(member);
	}

	public void removeMember(String member)
	{
		if(heroParty != null)
		{
			Player p = server.getPlayer(member);
			CharacterManager cm = new CharacterManager(DungeonBuilder.heroesPlugin);
			Hero h = cm.getHero(p);
			heroParty.removeMember(h);

			return;
		}

		members.remove(member);
	}

	public boolean containsMember(Player member)
	{
		if(heroParty != null)
			return heroParty.isPartyMember(member);

		return containsMember(member.getName());
	}

	public boolean containsMember(String member)
	{
		if(heroParty != null)
		{
			Player temp = server.getPlayer(member);
			return heroParty.isPartyMember(temp);
		}

		if(members.contains(member))
			return true;
		else
			return false;
	}

	public Set<String> listMembers()
	{
		HashSet<String> retVal = new HashSet<String>();
		if(heroParty != null)
		{
			for(Hero h : heroParty.getMembers())
				retVal.add(h.getPlayer().getName());
		}
		else
			retVal.addAll(members);

		return retVal;
	}

	public int getSize()
	{
		if(heroParty != null)
		{
			return heroParty.getMembers().size();
		}

		return members.size();
	}

	public String getLeader()
	{
		if(heroParty != null)
		{
			return heroParty.getLeader().getPlayer().getName();
		}

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

	public HeroParty getHeroParty()
	{
		return heroParty;
	}

	@Override public boolean equals(Object obj2)
	{
		if(!(obj2 instanceof DungeonParty))
			return false;

		DungeonParty party2 = (DungeonParty)obj2;

		if(heroParty != null && party2.getHeroParty() != null)
			return heroParty.equals(party2.getHeroParty());

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
