package net.virtuallyabstract.minecraft;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.*;
import java.util.concurrent.*;
import java.util.*;

public class DBPlayerListener extends PlayerListener
{
	private ConcurrentHashMap<String, Thread> runningThreads = new ConcurrentHashMap<String, Thread>();
	public ConcurrentHashMap<String, LocationWrapper> recordingLocations = new ConcurrentHashMap<String, LocationWrapper>();
	private HashMap<String, Long> teleportCooldowns = new HashMap<String, Long>();
	private DungeonBuilder plugin;

	private DBPlayerListener()
	{
	}

	/**
	 * Initialize the player listener for DungeonBuilder. 
	 * 
	 * This includes managing behavior for triggers and starting dungeons as well as the proximity check.
	 *
	 */
	public DBPlayerListener(DungeonBuilder plugin)
	{
		this.plugin = plugin;
	}

	@Override public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		Player p = event.getPlayer();

		if(plugin.respawnPriority == Event.Priority.Monitor)
		{
			plugin.removePlayerFromDungeon(p, false);
			return;
		}

		String playername = p.getName();
		if(plugin.inDungeons.containsKey(playername))
		{
			LocationWrapper lw = plugin.getSavePoint(playername);
			if(lw != null)
			{
				event.setRespawnLocation(lw.getTargetLocation());
				return;
			}
			else
			{
				Dungeon d = plugin.inDungeons.get(playername);
				event.setRespawnLocation(d.getStartingLocation());
			}
		}
	}

	@Override public void onPlayerQuit(PlayerQuitEvent event)
	{
		Player p = event.getPlayer();
		plugin.idleTimer.put(p.getName(), System.currentTimeMillis());
	}

	@Override public void onPlayerJoin(PlayerJoinEvent event)
	{
		final Player p = event.getPlayer();

		boolean idle = false;
		if(plugin.idleTimer.containsKey(p.getName()))
		{
			idle = plugin.isPlayerIdle(p.getName());

			plugin.idleTimer.remove(p.getName());
		}

		if(plugin.inDungeons.containsKey(p.getName()))
		{
			if(!plugin.isPlayerStillInDungeon(p))
				plugin.removePlayerFromDungeon(p, false);
			else if(idle)
				plugin.removePlayerFromDungeon(p, true);
		}
	}

	@Override public void onPlayerMove(final PlayerMoveEvent event)
	{
		final Player p = event.getPlayer();
		final String name = p.getName();

		if(recordingLocations.containsKey(name))
		{
			Location loc = p.getLocation();
			LocationWrapper lw = recordingLocations.get(name);
			if(lw.addLocation(loc))
			{
				p.sendMessage("Trigger location added");
			}

			if(lw.remainingLocations() == 0)
			{
				p.sendMessage("Finished adding trigger locations");
				p.sendMessage("Trigger Added");
				Dungeon d = lw.getDungeon();
				plugin.dungeonManager.removeDungeon(d);
				plugin.dungeonManager.addDungeon(d);
				recordingLocations.remove(name);
			}
		}

		boolean onCooldown = false;
		if(teleportCooldowns.containsKey(name))
		{
			Long currentTime = System.currentTimeMillis();
			Long targetTime = teleportCooldowns.get(name);
			if(currentTime >= targetTime)
				teleportCooldowns.remove(name);
			else
				onCooldown = true;
		}

		lwloop: for(final LocationWrapper lworig : plugin.dungeonManager.checkLocation(event.getTo()))
		{
			synchronized(lworig)
			{
				final Dungeon d = lworig.getDungeon();

				switch(lworig.getType())
				{
					case MONSTER_TRIGGER:
						if(lworig.isActive())
						{
							d.spawnMonster(lworig.getMetaData());
							lworig.setActive(false);
						}
						continue lwloop;
					case SAVE_POINT:
						if(lworig.isActive())
						{
							LocationWrapper savePoint = plugin.getSavePoint(name);
							if(savePoint != null)
							{
								if(savePoint.equals(lworig))
									continue lwloop;
							}

							if(plugin.setSavePoint(name, lworig))
								p.sendMessage("Save point activated");
						}
						continue lwloop;
					case SCRIPT_TRIGGER:
						if(lworig.isActive())
						{
							System.out.println("Running script - " + lworig.getMetaData());
							ScriptManager.runScript(d, plugin.server, p, plugin, lworig.getMetaData());	
							lworig.setActive(false);
						}
					default:
						break;
				}

				if(!onCooldown && !runningThreads.containsKey(d.getName()))
				{
					boolean startDungeon = false;
					if(lworig.getType() == LocationWrapper.LocationType.DUNGEON_START)
					{
						Dungeon.PartyStatus status = null;
						DungeonParty dp = null;
						if(plugin.inParty.containsKey(p.getName()))	
							dp = plugin.inParty.get(p.getName());
						else if(d.getPartySize() == 1)
						{
							dp = new DungeonParty(p.getName(), plugin.server);
							plugin.inParty.put(p.getName(), dp);
						}

						if(dp == null || dp.getSize() != d.getPartySize())
						{
							p.sendMessage("Unable to queue for dungeon.  The expected party size is: " + d.getPartySize());
							teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
							continue lwloop;
						}

						StringBuffer cooldownbuffer = new StringBuffer();
						for(String pname : dp.listMembers())
						{
							if(!d.isPlayerOnCooldown(pname))
								continue;

							Long timeleft = d.timeLeftOnCooldown(pname);
							if(timeleft >= 0)
							{
								timeleft = timeleft / 1000;
								cooldownbuffer.append(pname + " - " + timeleft + " seconds\n");
							}
							else
							{
								cooldownbuffer.append(pname + " - Never\n");
							}
						}

						if(cooldownbuffer.length() > 0)
						{
							p.sendMessage("Unable to queue for dungeon.  The following party members");
							p.sendMessage("still have active cooldowns:");
							for(String line : cooldownbuffer.toString().split("\n"))
								p.sendMessage(line);
							teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
							continue lwloop;
						}

						status = d.addParty(dp);

						switch(status)
						{
							case MISSING_PLAYERS:
								p.sendMessage("The dungeon will start when the rest of the party steps on the teleporter");
								p.sendMessage("To abort joining the dungeon type /leaveDungeon");

								StringBuffer partyList = new StringBuffer();
								for(String partyMember : dp.listMembers())
								{
									if(partyList.length() > 0)
										partyList.append(", ");
									partyList.append(partyMember);
								}
								p.sendMessage("Current party members: " + partyList.toString());

								teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
								break lwloop;
							case FULL:
								p.sendMessage("The dungeon party is already full");
								teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
								continue lwloop;
							case INQUEUE:
								p.sendMessage("You are currently in the queue for this dungeon, you will be notified when it becomes available");
								teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
								continue lwloop;
							case TOOLARGE:
								p.sendMessage("Your party size is too large for this dungeon.  Expected size: " + d.getPartySize());
								teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
								continue lwloop;
							case READY:
								startDungeon = true;
								break;
						}
						
						if(!startDungeon)
							return;
					}

					Thread t = new Thread() { @Override public void run() {
						try
						{
							DungeonParty partytemp = null;
							if(plugin.inParty.containsKey(p.getName()))
								partytemp = plugin.inParty.get(p.getName());

							if(partytemp == null)
								return;

							final DungeonParty party = partytemp;

							if(lworig.getType() == LocationWrapper.LocationType.DUNGEON_START)
							{

								for(int i = 5; i > 0; i--)
								{
									for(String dpname : party.listMembers())
									{
										Player dp = plugin.server.getPlayer(dpname);	
										if(dp != null)
											dp.sendMessage("Starting dungeon in " + i + "...");
									}

									Thread.sleep(1000);

									boolean found = false;
									for(LocationWrapper lwnew : plugin.dungeonManager.checkLocation(p.getLocation()))
									{
										if(lwnew != null && lwnew.equals(lworig))
											found = true;
									}

									if(!found)
									{
										if(party.getSize() == 1)
										{
											d.removeParty(party);
											plugin.inParty.remove(p.getName());
										}

										return;
									}
								}
							}

							plugin.scheduler.scheduleSyncDelayedTask(plugin, new Runnable() { @Override public void run() {
								Location loc = lworig.getTargetLocation();
								World world = loc.getWorld();
								world.loadChunk(loc.getBlockX(), loc.getBlockZ());
								event.setTo(loc);

								switch(lworig.getType())
								{
									case DUNGEON_START:
										plugin.startDungeon(d, party, loc);
										for(String dp : party.listMembers())
											teleportCooldowns.put(dp, System.currentTimeMillis() + 5000L);
										break;
									case DUNGEON_EXIT:
										plugin.rewardPlayer(p, d);
										d.rewardPlayerExp(p);
										plugin.removePlayerFromDungeon(p, true);	
										teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
										//if(d.getAutoload())
										//	d.loadDungeon();	
										//d.killMonsters();
										//DungeonParty dparty = d.getActiveParty();
										//if(dparty == null)
										//	break;
										//for(Player dp : dparty.listMembers())
										//{
										//	Player ptemp = server.getPlayer(dp.getName());

										//	rewardPlayer(dp, d);
										//	inDungeons.remove(dp.getName());
										//	clearSavePoint(dp.getName());
										//	ScriptManager.runDungeonExitScript(d, server, ptemp);

										//	ptemp.teleport(loc);
										//}
										//if(dparty.getSize() == 1)
										//	inParty.remove(p.getName());
										//d.removeParty(dparty);

										break;
								}
							}});
						}
						catch(Exception e)
						{
							e.printStackTrace();
							p.sendMessage("An error occurred trying to teleport player: " + e.getMessage());
							event.setCancelled(true);
						}
						finally
						{
							runningThreads.remove(d.getName());
						}
					}};
					teleportCooldowns.put(p.getName(), System.currentTimeMillis() + 5000L);
					runningThreads.put(d.getName(), t);
					t.start();

					return;
				}
			}	
			
		}

		if(plugin.proximityCheck == false)
			return;

		Dungeon inDungeon = null;
		if(plugin.inDungeons.containsKey(name))
			inDungeon = plugin.inDungeons.get(name);

		for(String key : plugin.dungeonMap.keySet())
		{
			if(key.equals(name))
				continue;

			for(Dungeon d : plugin.dungeonMap.get(key))
			{
				if(!d.getWorld().equals(p.getWorld()))
					continue;

				if(inDungeon != null && d.equals(inDungeon))
					continue;

				if(d.inDungeon(p.getLocation()))
				{
					Location loc1 = event.getFrom();
					Location loc2 = event.getTo();
					double xdiff = loc2.getX() - loc1.getX();
					double ydiff = loc2.getY() - loc1.getY();
					double zdiff = loc2.getZ() - loc1.getZ();

					Location newloc = loc2.clone();
					newloc.setX(newloc.getX() - xdiff*10.0);
					newloc.setY(newloc.getY() - ydiff*10.0);
					newloc.setZ(newloc.getZ() - zdiff*10.0);

					p.teleport(newloc);
					p.sendMessage("Off Limits");

					return;
				}
			}
		}

	}
}
