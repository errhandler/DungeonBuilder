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
import javax.script.*;
import java.util.*;
import java.io.*;

public class Dungeon implements Comparable<Dungeon>
{
	private World world;
	private DungeonBuilder plugin;
	private String owner, name;
	private Location start, center, exit, teleporter, exitdest, sphereCenter;
	private int width, depth, height;
	private ArrayList<BlockInfo> blocks, origBlocks = null;
	private ArrayList<Entity> liveMonsters;
	private ArrayList<String> defaultPermissions;
	private ArrayList<MonsterInfo> savedMonsters;
	private HashMap<String, LocationWrapper> monsterTriggers, scriptTriggers, savePoints;
	private volatile boolean published, autoload = true, allowSpawn = false;
	private double reward, sphereRadius;
	private int partySize = 1;
	private ArrayList<Player> currentPlayers = new ArrayList<Player>();
	private ArrayList<DungeonParty> partyList = new ArrayList<DungeonParty>();
	private volatile DungeonParty activeParty;
	private volatile PartyStatus currentStatus;

	public enum PartyStatus
	{
		MISSING_PLAYERS, FULL, READY, EMPTY, INQUEUE, TOOLARGE;
	}

	private void init()
	{
		blocks = new ArrayList<BlockInfo>();
		liveMonsters = new ArrayList<Entity>();
		savedMonsters = new ArrayList<MonsterInfo>();
		defaultPermissions = new ArrayList<String>();
		monsterTriggers = new HashMap<String, LocationWrapper>();
		scriptTriggers = new HashMap<String, LocationWrapper>();
		savePoints = new HashMap<String, LocationWrapper>();
		currentStatus = PartyStatus.EMPTY;
	}

	public Dungeon(String name, String owner, Location center, File dungeonFile, DungeonBuilder plugin)
		throws Exception
	{
		this.name = name;
		this.owner = owner;
		this.start = center.clone();
		this.exit = center.clone();
		this.center = center;
		this.published = false;
		this.world = center.getWorld();
		this.reward = 0.0;
		this.plugin = plugin;
		init();
		loadFromFile(dungeonFile, null, true);
		refreshExitBlock();
		calculateSphere();
	}

	public Dungeon(String name, String owner, Location center, int width, int depth, int height, DungeonBuilder plugin)
		throws Exception
	{
		this.owner = owner;
		this.start = center.clone();
		this.exit = center.clone();
		this.exit.setX(this.exit.getX()-1);
		this.center = center;
		this.width = width;
		this.depth = depth;
		this.height = height;
		this.name = name;
		this.published = false;
		this.world = center.getWorld();
		this.reward = 0.0;
		this.plugin = plugin;
		init();
		refreshExitBlock();
		calculateSphere();
		saveDungeon();
	}

	public Dungeon(String name, String owner, DungeonBuilder plugin)
		throws Exception
	{
		this.owner = owner;
		this.name = name;
		this.published = false;
		this.reward = 0.0;
		this.plugin = plugin;
		init();
		loadFromFile(plugin.server);
		calculateSphere();
	}

	public void setOriginalBlocks(ArrayList<BlockInfo> blocks)
	{
		origBlocks = blocks;	
	}

	public boolean canUndo()
	{
		return origBlocks != null;
	}

	public World getWorld()
	{
		return world;
	}

	public int getWidth()
	{
		return width;
	}

	public int getDepth()
	{
		return depth;
	}

	public int getHeight()
	{
		return height;
	}

	public boolean containsLocation(Location l)
	{
		return containsLocation(l, 0);
	}

	public boolean containsLocation(Location l, int buffer)
	{
		double x = l.getX();
		double y = l.getY();
		double z = l.getZ();

		double halfdepth = (depth / 2) + buffer;
		double minx = center.getX() - halfdepth;
		double maxx = center.getX() + halfdepth;

		double halfwidth = (width / 2) + buffer;
		double minz = center.getZ() - halfwidth;
		double maxz = center.getZ() + halfwidth;

		double miny = center.getY() - buffer - 1.0;
		double maxy = center.getY() + height - 1.0 + buffer;

		if(x >= minx && x <= maxx && z >= minz && z <= maxz && y >= miny && y <= maxy)
			return true;

		return false;
	}
	
	public Location setStartingLocation(Location l)
	{
		Location retVal = start;
		this.start = l;

		//try
		//{
		//	saveDungeon();
		//}
		//catch(Exception e)
		//{
		//	e.printStackTrace();
		//}

		return retVal;
	}

	public Location getStartingLocation()
	{
		return this.start;
	}

	public Location getExitLocation()
	{
		return this.exit;
	}
	
	public Location setExitLocation(Location l)
	{
		Location temp = exit.clone();
		temp.setY(temp.getY() - 1);
		Block b = temp.getBlock();
		Block b2 = b.getRelative(-1, 0, 0);
		b.setType(b2.getType());

		Location retVal = exit;
		exit = l;

		refreshExitBlock();

		//try
		//{
		//	saveDungeon();
		//}
		//catch(Exception e)
		//{
		//	e.printStackTrace();
		//}

		return retVal;
	}

	public void refreshExitBlock()
	{
		Location temp = exit.clone();
		temp.setY(temp.getY() - 1);
		Block b = temp.getBlock();
		b.setType(Material.DIAMOND_BLOCK);
	}

	public Location getCenterLocation()
	{
		return this.center;
	}

	public String getName()
	{
		return name;
	}

	public String getOwner()
	{
		return owner;
	}

	public void publish(Location teleport)
		throws Exception
	{
		this.published = true;

		teleporter = teleport;

		if(exitdest == null)
		{
			Location temp = teleporter.clone();
			temp.setX(temp.getX() - 2.0);
			exitdest = temp;
		}

		saveDungeon();
	}

	public void unpublish()
		throws Exception
	{
		this.published = false;
		//saveDungeon();
	}

	public boolean isPublished()
	{
		return published;
	}

	public Location getTeleporterLocation()
	{
		return teleporter;
	}

	public Location getExitDestination()
	{
		return exitdest;
	}

	public void setExitDestination(Location dest)
		throws Exception
	{
		exitdest = dest;
		//saveDungeon();
	}

	public void addDefaultPermission(String node)
	{
		defaultPermissions.add(node);
	}

	public void removeDefaultPermission(String node)
	{
		defaultPermissions.remove(node);
	}

	public void clearDefaultPermissions()
	{
		defaultPermissions.clear();
	}

	public ArrayList<String> listDefaultPermissions()
	{
		return new ArrayList<String>(defaultPermissions);
	}

	public boolean hasDefaultPermission(String node)
	{
		return defaultPermissions.contains(node);
	}

	public void saveDungeon()
		throws Exception
	{
		if(!DungeonBuilder.dontSaveBlocks)
		{
			blocks.clear();

			Location newLoc = center.clone();

			int halfdepth = depth / 2;
			int halfwidth = width / 2;

			double xbase = center.getX();
			double zbase = center.getZ();
			double ybase = center.getY() - 1;
			for(double y = 0.0; y <= height; y += 1.0)
			{
				newLoc.setY(ybase + y);
				for(double x = -halfdepth; x <= halfdepth; x += 1.0)
				{
					newLoc.setX(xbase + x);
					for(double z = -halfwidth; z <= halfwidth; z += 1.0)
					{
						newLoc.setZ(zbase + z);	
						blocks.add(new BlockInfo(newLoc.getBlock()));
					}
				}
			}

			for(Entity e : world.getEntities())
			{
				Location eloc = e.getLocation();
				if(!containsLocation(eloc))
					continue;

				Block eblock = eloc.getBlock();
				BlockInfo bi = new BlockInfo(eblock);
				bi.setMetaString("ENTITY("+ e.getClass().toString() + ")");
				bi.setEntity(e);
				blocks.add(bi);
			}

			Collections.sort(blocks);
		}

		saveToFile();

		origBlocks = null;
	}

	public boolean deleteFromServer(boolean clear)
	{
		if(clear)
		{
			Collections.reverse(blocks);
			for(BlockInfo bi : blocks)
			{
				Block b = bi.getBlock();
				b.setType(Material.AIR);
			}
		}

		File dungeonDir = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name);
		if(!dungeonDir.exists())
			return true;

		return dungeonDir.delete();
	}

	public void loadDungeon()
	{
		clearLiquids();

		for(BlockInfo bi : blocks)
		{
			bi.setBlock();
		}

		refreshExitBlock();
	}

	public void undoDungeon()
	{
		if(origBlocks == null)
			return;

		for(BlockInfo bi : origBlocks)
		{
			bi.setBlock();
		}

		origBlocks = null;
	}

	public void clearDungeon()
	{
		ArrayList<BlockInfo> temp = new ArrayList<BlockInfo>(blocks);
		Collections.reverse(temp);

		for(BlockInfo bi : temp)
		{
			if(bi.getType() != Material.BEDROCK)
			{
				Block b = bi.getBlock();
				BlockState bs = b.getState();
				if(bs instanceof ContainerBlock)
				{
					ContainerBlock cb = (ContainerBlock)bs;
					cb.getInventory().clear();
				}

				bi.getBlock().setType(Material.AIR);
			}
		}

		refreshExitBlock();
	}

	public void clearEntities()
	{
		for(BlockInfo bi : blocks)
		{
			if(bi.getEntity() == null)
				continue;

			bi.getEntity().remove();
		}
	}

	public void clearTorches()
	{
		Location loc = center.clone();

		int halfdepth = depth / 2;
		int halfwidth = width / 2;

		double xbase = center.getX();
		double zbase = center.getZ();
		double ybase = center.getY() - 1;
		for(double y = 1.0; y <= height-1; y += 1.0)
		{
			loc.setY(ybase + y);
			for(double x = -halfdepth+1; x <= halfdepth-1; x += 1.0)
			{
				loc.setX(xbase + x);
				for(double z = -halfwidth+1; z <= halfwidth-1; z += 1.0)
				{
					loc.setZ(zbase + z);	
					Block b = loc.getBlock();
					Material m = b.getType();
					if(m == Material.TORCH)
						b.setType(Material.AIR);
				}
			}
		}
	}

	public void clearLiquids()
	{
		Location loc = center.clone();

		int halfdepth = depth / 2;
		int halfwidth = width / 2;

		double xbase = center.getX();
		double zbase = center.getZ();
		double ybase = center.getY() - 1;
		for(double y = 1.0; y <= height-1; y += 1.0)
		{
			loc.setY(ybase + y);
			for(double x = -halfdepth+1; x <= halfdepth-1; x += 1.0)
			{
				loc.setX(xbase + x);
				for(double z = -halfwidth+1; z <= halfwidth-1; z += 1.0)
				{
					loc.setZ(zbase + z);	
					Block b = loc.getBlock();
					Material m = b.getType();
					if(m == Material.LAVA || m == Material.STATIONARY_LAVA || m == Material.WATER || m == Material.STATIONARY_WATER)
						b.setType(Material.AIR);
				}
			}
		}
	}

	public String getFileName()
	{
		return DungeonBuilder.dungeonRoot + "/" + owner + "/" + name;
	}

	public void saveToFile()
		throws Exception
	{
		File playerDir = new File(DungeonBuilder.dungeonRoot + "/" + owner);
		if(!playerDir.exists())
		{
			if(!playerDir.mkdirs())
				throw new Exception("Failed to make directory: " + playerDir.getPath());
		}

		File dungeonFile = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name);
		if(dungeonFile.exists())
		{
			File backup = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name + ".previous");
			if(backup.exists())
			{
				if(!backup.delete())
				{
					throw new Exception("Failed to delete previous backup file for dungeon");
				}
			}

			if(!dungeonFile.renameTo(backup))
				throw new Exception("Failed to backup existing dungeon file");
		}

		saveToFile(dungeonFile, false);
	}

	public synchronized void saveToFile(File dungeonFile, boolean relative)
		throws Exception
	{
		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter(dungeonFile));
			pw.print("World:" + world.getName() + "," + world.getEnvironment().getId() + "\n");
			pw.print("PartySize:" + partySize + "\n");
			pw.print("Reward:" + reward + "\n");
			pw.print("Autoload:" + autoload + "\n");
			if(published)
			{
				World tworld = teleporter.getWorld();
				pw.print("T:" + teleporter.getX() + "," + teleporter.getY() + "," + teleporter.getZ() + "," + tworld.getName() + "," + tworld.getEnvironment().getId() + "\n");

				World eworld = exitdest.getWorld();
				pw.print("E:" + exitdest.getX() + "," + exitdest.getY() + "," + exitdest.getZ() + "," + eworld.getName() + "," + eworld.getEnvironment().getId() + "\n");
			}
			else
			{
				pw.print("\n");
				pw.print("\n");
			}
			pw.print(center.getX() + "," + center.getY() + "," + center.getZ() + "," + width + "," + depth + "," + height + "\n");
			pw.print(createLocationString(start, relative) + "\n");
			pw.print(createLocationString(exit, relative) + "\n");

			if(!DungeonBuilder.dontSaveBlocks)
			{
				for(BlockInfo bi : blocks)
				{
					Block b = bi.getBlock();
					Material m = b.getType();
					Location loc = b.getLocation();

					String metaStr = null;
					BlockState bs = b.getState();
					if(bs instanceof ContainerBlock)
					{
						metaStr = createInventoryString((ContainerBlock)bs);
					}

					if(bs instanceof org.bukkit.block.Sign)
					{
						metaStr = createSignString((org.bukkit.block.Sign)bs);
					}

					//For saving entities
					if(metaStr == null && bi.getMetaString() != null)
						metaStr = bi.getMetaString();

					if(metaStr == null)
						pw.print(createLocationString(loc, relative) + "," + m.getId() + "," +  b.getData() + "\n");
					else
					{
						pw.print(createLocationString(loc, relative) + "," + m.getId() + "," +  b.getData() + "," + metaStr + "\n");
						bi.setMetaString(metaStr);
					}
				}
			}

			for(MonsterInfo monster : savedMonsters)
				pw.print(monster.getLine(relative) + "\n");

			for(LocationWrapper lw : monsterTriggers.values())
				pw.print(lw.toString(relative) + "\n");

			for(LocationWrapper lw : savePoints.values())
				pw.print(lw.toString(relative) + "\n");

			for(LocationWrapper lw : scriptTriggers.values())
				pw.print(lw.toString(relative) + "\n");
		}
		finally
		{
			if(pw != null)
				pw.close();
		}

		File permFile = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name + ".perms");

		pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter(permFile));
			for(String node : defaultPermissions)
				pw.print(node + "\n");
		}
		finally
		{
			if(pw != null)
				pw.close();
		}
	}

	public void loadFromFile(Server s)
		throws Exception
	{
		File dungeonFile = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name);
		if(!dungeonFile.exists())
			throw new Exception("Unable to locate dungeon file: " + dungeonFile.getPath());

		loadFromFile(dungeonFile, s, false);
	}

	public synchronized void loadFromFile(File dungeonFile, Server s, boolean relative)
		throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(dungeonFile));
		String line = br.readLine();
		if(line == null)
			throw new Exception("Dungeon file is empty or incorrectly formatted");

		if(line.startsWith("World:"))
		{
			String [] comps = line.substring(6).split(",");
			String wname = comps[0];
			Integer envid = Integer.parseInt(comps[1]);
			if(!relative && s != null)
			{
				WorldCreator wc = new WorldCreator(wname);
				wc.environment(World.Environment.getEnvironment(envid));
				world = s.createWorld(wc);
			}
			line = br.readLine();
		}
		else if(!relative)
			world = s.getWorld("world");

		if(line.startsWith("PartySize:"))
		{
			String temp = line.substring(10);
			try
			{
				this.partySize = Integer.parseInt(temp);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				this.partySize = 1;
			}

			line = br.readLine();
		}
		else
			partySize = 1;

		if(partySize == 1)
			currentStatus = PartyStatus.READY;

		if(line.startsWith("Reward:"))
		{
			String temp = line.substring(7);
			this.reward = Double.parseDouble(temp);

			line = br.readLine();
		}
		else
			reward = 0.0;

		if(line.startsWith("Autoload:"))
		{
			String temp = line.substring(9);
			this.autoload = Boolean.parseBoolean(temp);

			line = br.readLine();
		}

		if(line.startsWith("T:"))
		{
			String temp = line.substring(2);
			String [] comps = temp.split(",");
			if(comps.length < 3)
				throw new Exception("The dungeon file is missing parameters for the teleporter location");

			if(!relative)
			{
				if(comps.length == 3)
					teleporter = createLocation(world, comps[0], comps[1], comps[2]);
				else if(comps.length == 5)
					teleporter = createLocation(s, comps[0], comps[1], comps[2], comps[3], comps[4]);
				published = true;
			}

			line = br.readLine();
		}
		else if(line.trim().length() == 0)
			line = br.readLine();

		if(line.startsWith("E:"))
		{
			String temp = line.substring(2);
			String [] comps = temp.split(",");
			if(comps.length < 3)
				throw new Exception("The dungeon file is missing parameters for the exit destination location");
			if(!relative)
			{
				if(comps.length == 3)
					exitdest = createLocation(world, comps[0], comps[1], comps[2]);
				else if(comps.length == 5)
					exitdest = createLocation(s, comps[0], comps[1], comps[2], comps[3], comps[4]);
			}

			line = br.readLine();
		}
		else if(line.trim().length() == 0)
			line = br.readLine();

		if(exitdest == null && teleporter != null)
		{
			exitdest = teleporter.clone();
			exitdest.setX(exitdest.getX() - 2.0);
		}

		String [] dungeonParams = line.trim().split(",");
		if(dungeonParams.length < 6)
			throw new Exception("The following line in the dungeon file is missing parameters: " + line.trim());
		if(!relative)
			center = createLocation(world, dungeonParams[0], dungeonParams[1], dungeonParams[2]);
		width = Integer.parseInt(dungeonParams[3]);
		depth = Integer.parseInt(dungeonParams[4]);
		height = Integer.parseInt(dungeonParams[5]);
	
		line = br.readLine();
		if(line == null)
			throw new Exception("The dungeon file is missing the starting location for the dungeon");

		dungeonParams = line.trim().split(",");
		if(dungeonParams.length < 3)
			throw new Exception("The following line of the dungeon file is missing parameters: " + line.trim());
		start = createLocation(world, dungeonParams[0], dungeonParams[1], dungeonParams[2], relative);

		line = br.readLine();
		if(line == null)
			throw new Exception("The dungeon file is missing the exit location for the dungeon");

		dungeonParams = line.trim().split(",");
		if(dungeonParams.length < 3)
			throw new Exception("The following line of the dungeon file is missing parameters: " + line.trim());
		exit = createLocation(world, dungeonParams[0], dungeonParams[1], dungeonParams[2], relative);
		
		blocks.clear();

		if(relative)
			origBlocks = new ArrayList<BlockInfo>();

		ArrayList<BlockInfo> attachableBlocks = new ArrayList<BlockInfo>();
		while((line = br.readLine()) != null)
		{
			if(line.startsWith("M:"))
			{
				try
				{
					MonsterInfo mi = new MonsterInfo(line.trim(), relative);
					savedMonsters.add(mi);
					addMonsterTrigger(start, mi.getAlias(), mi.getAlias());
				}
				catch(Exception e)
				{
					System.out.println("Invalid line: " + line);
					System.out.println(e.getMessage());
				}

				continue;
			}

			if(line.startsWith("MT:"))
			{
				try
				{
					LocationWrapper lw = new LocationWrapper(this, line.trim(), relative);
					addMonsterTrigger(lw);
				}
				catch(Exception e)
				{
					System.out.println(e.getMessage() + ": " + line);
					e.printStackTrace();
				}

				continue;
			}

			if(line.startsWith("SP:"))
			{
				try
				{
					LocationWrapper lw = new LocationWrapper(this, line.trim(), relative);
					addSavePoint(lw);
				}
				catch(Exception e)
				{
					System.out.println(e.getMessage() + ": " + line);
					e.printStackTrace();
				}

				continue;
			}

			if(line.startsWith("ST:"))
			{
				try
				{
					LocationWrapper lw = new LocationWrapper(this, line.trim(), relative);
					addScriptTrigger(lw);
				}
				catch(Exception e)
				{
					System.out.println(e.getMessage() + ": " + line);
					e.printStackTrace();
				}

				continue;
			}

			String [] comps = line.trim().split(",");
			if(comps.length < 4)
				throw new Exception("The dungeon file is missing required fields");

			Location loc = createLocation(world, comps[0], comps[1], comps[2], relative);
			int type = Integer.parseInt(comps[3]);
			Block b = loc.getBlock();
			if(relative)
				origBlocks.add(new BlockInfo(b));
			Material m = Material.getMaterial(type);
			Byte data = null;
			if(comps.length == 5)
			{
				data = Byte.parseByte(comps[4]);
			}

			BlockInfo bi = new BlockInfo(b, m, data);
			if(comps.length == 6)
			{
				bi.setMetaString(comps[5]);	
			}

			blocks.add(bi);
		}

		Collections.sort(blocks);

		br.close();

		File permissionFile = new File(DungeonBuilder.dungeonRoot + "/" + owner + "/" + name + ".perms");
		if(permissionFile.exists())
		{
			defaultPermissions.clear();

			br = new BufferedReader(new FileReader(permissionFile));
			line = null;
			while((line = br.readLine()) != null)
			{
				defaultPermissions.add(line.trim());
			}
			br.close();
		}

		if(autoload || relative)
			loadDungeon();
	}

	@Override public int compareTo(Dungeon d)
	{
		return getName().compareTo(d.getName());
	}

	private Location createLocation(Server server, String x, String y, String z, String worldname, String worldid)
		throws Exception
	{
		Integer id = Integer.parseInt(worldid);
		WorldCreator wc = new WorldCreator(worldname);
		wc.environment(World.Environment.getEnvironment(id));
		World w = server.createWorld(wc);

		return createLocation(w, x, y, z);
	}

	public Location createLocation(String x, String y, String z)
		throws Exception
	{
		return createLocation(world, x, y, z);
	}

	public Location createLocation(String x, String y, String z, boolean relative)
		throws Exception
	{
		return createLocation(world, x, y, z, relative);
	}

	private Location createLocation(World w, String x, String y, String z)
		throws Exception
	{
		return createLocation(w, x, y, z, false);
	}

	private Location createLocation(World w, String x, String y, String z, boolean relative)
		throws Exception
	{
		double xd = Double.parseDouble(x);
		double yd = Double.parseDouble(y);
		double zd = Double.parseDouble(z);

		if(relative)
		{
			double cx = center.getX();
			double cy = center.getY();
			double cz = center.getZ();

			xd = cx + xd;
			yd = cy + yd;
			zd = cz + zd;
		}


		return new Location(w, xd, yd, zd);
	}

	public String createLocationString(Location loc, boolean relative)
	{
		StringBuffer retVal = new StringBuffer();
		double x = Math.floor(loc.getX());
		double y = Math.floor(loc.getY());
		double z = Math.floor(loc.getZ());

		if(relative)
		{
			x = x - Math.floor(center.getX());
			y = y - Math.floor(center.getY());
			z = z - Math.floor(center.getZ());
		}

		retVal.append(x);
		retVal.append("," + y);
		retVal.append("," + z);

		return retVal.toString();
	}

	private String createSignString(org.bukkit.block.Sign s)
	{
		StringBuffer retVal = new StringBuffer("text(");
		for(String line : s.getLines())
		{
			retVal.append(line + ";");
		}
		retVal.append(")");

		return retVal.toString();
	}

	public static void parseSignString(org.bukkit.block.Sign s, String metaStr)
	{
		metaStr = metaStr.substring(5);
		metaStr = metaStr.substring(0, metaStr.length() - 2);

		String [] lines = metaStr.split(";");
		for(int i = 0; i < lines.length; i++)
		{
			s.setLine(i, lines[i]);
		}
	}

	private String createInventoryString(ContainerBlock cb)
	{
		Inventory i = cb.getInventory();
		StringBuffer retVal = new StringBuffer();

		retVal.append("inv(");
		for(ItemStack is : i.getContents())
		{
			if(is == null)
				continue;

			String item = is.getTypeId() + ":" + is.getAmount() + ":" + is.getDurability();
			retVal.append(item + ";");
		}
		retVal.append(")");

		return retVal.toString();
	}

	public static void parseInventoryString(ContainerBlock cb, String invStr)
	{
		Inventory i = cb.getInventory();
		i.clear();

		invStr = invStr.substring(4);
		invStr = invStr.substring(0, invStr.length() - 1);

		String [] comps = invStr.split(";");
		for(String itemStr : comps)
		{
			String [] itemData = itemStr.split(":");
			if(itemData.length < 3)
				continue;

			try
			{
				Integer typeId = Integer.parseInt(itemData[0]);
				Integer amount = Integer.parseInt(itemData[1]);
				Short durability = Short.parseShort(itemData[2]);
				
				ItemStack is = new ItemStack(typeId);
				is.setAmount(amount);
				is.setDurability(durability);

				i.addItem(is);
			}
			catch(Exception e)
			{
				System.out.println("Oops...this line doesn't look right: " + invStr);
				System.out.println("Guess I'll just skip it.");
			}
		}
	}

	public boolean addMonster(Location l, String key, String type)
	{
		return addMonster(l, key , type, 1);
	}

	public boolean addMonster(Location l, String key, String type, int count)
	{
		MonsterInfo mi = new MonsterInfo(l, key, type, count);
		if(!savedMonsters.contains(mi))
		{
			savedMonsters.add(mi);
			addMonsterTrigger(start, key, key);

			return true;
		}

		return false;
	}

	public boolean removeMonster(String key)
	{
		for(int i = 0; i < savedMonsters.size(); i++)
		{
			if(savedMonsters.get(i).getAlias().equals(key))
			{
				savedMonsters.remove(i);
				for(LocationWrapper lw : monsterTriggers.values())
				{
					if(lw.getMetaData().equals(key))
					{
						monsterTriggers.remove(lw.getAlias());
						break;
					}
				}

				return true;
			}
		}

		return false;
	}

	public String listMonsters()
	{
		StringBuffer sb = new StringBuffer("");
		for(MonsterInfo monster : savedMonsters)
		{
			if(sb.length() > 0)
				sb.append(", ");
			sb.append(monster.getAlias());	
		}

		return sb.toString();
	}

	public void spawnMonsters()
		throws Exception
	{
		ArrayList<Chunk> loadedChunks = new ArrayList<Chunk>();

		allowSpawn = true;
		for(MonsterInfo monster : savedMonsters)
		{
			monster.killMonsters();

			Location l = monster.getLoc();

			Chunk c = world.getChunkAt(l);

			if(!loadedChunks.contains(c))
			{
				world.loadChunk(c);
				loadedChunks.add(c);
			}

			monster.spawnMonsters();
		}
		allowSpawn = false;
	}

	public void spawnMonster(String alias)
	{
		allowSpawn = true;
		for(MonsterInfo monster : savedMonsters)
		{
			if(!monster.getAlias().equals(alias))
				continue;

			monster.killMonsters();

			Location l = monster.getLoc();

			String type = monster.getType();
			int count = monster.getCount();

			monster.spawnMonsters();
		}
		allowSpawn = false;
	}

	public boolean spawnsAllowed()
	{
		return allowSpawn;
	}

	public void killMonsters()
	{
		for(MonsterInfo monster : savedMonsters)
			monster.killMonsters();
	}

	public void setDungeonReward(double reward)
	{
		this.reward = reward;
	}

	public double getDungeonReward()
	{
		return reward;
	}

	private void calculateSphere()
	{
		sphereCenter = center.clone();
		sphereCenter.setY(sphereCenter.getY() - 1.0 + (height / 2.0));

		Location corner = center.clone();
		corner.setY(corner.getY() - 1.0);
		corner.setX(corner.getX() - (depth / 2.0));
		corner.setZ(corner.getZ() - (width / 2.0));

		double halfheight = height / 2.0;
		double halfdepth = depth / 2.0;
		double halfwidth = width / 2.0;
		sphereRadius = Math.sqrt(halfheight*halfheight + halfdepth*halfdepth + halfwidth*halfwidth);
	}

	public boolean inDungeon(Location loc)
	{
		double xdiff = Math.abs(loc.getX() - sphereCenter.getX());
		double zdiff = Math.abs(loc.getZ() - sphereCenter.getZ());
		double ydiff = Math.abs(loc.getY() - sphereCenter.getY());

		double length = Math.sqrt(xdiff*xdiff + ydiff*ydiff + zdiff*zdiff);
		if(length <= sphereRadius + 10.0)
		{
			return containsLocation(loc, 10);	
		}

		return false;
	}

	public void setPartySize(int size)
	{
		partySize = size;
	}

	public int getPartySize()
	{
		return partySize;
	}

	public PartyStatus getStatus()
	{
		if(partyList.size() == 0)
			return PartyStatus.EMPTY;

		if(activeParty != null)
			return PartyStatus.FULL;

		for(DungeonParty dp : partyList)
		{
			if(dp.getSize() == partySize)
				return PartyStatus.READY;
		}

		return PartyStatus.MISSING_PLAYERS;
	}

	public DungeonParty addPlayer(Player p)
	{
		if(partyList.size() == 0)
		{
			DungeonParty party = new DungeonParty(p.getName(), plugin.server);
			partyList.add(party);

			return party;
		}
		else
		{
			for(DungeonParty dp : partyList)
			{
				if(!dp.containsMember(p))
					continue;

				return dp;
			}

			for(int i = 0; i < partyList.size(); i++)
			{
				DungeonParty party = partyList.get(i);
				if(party.getSize() < partySize)
				{
					party.addMember(p.getName());

					return party;
				}
			}

			DungeonParty party = new DungeonParty(p.getName(), plugin.server);
			partyList.add(party);

			return party;
		}
	}

	public PartyStatus addParty(DungeonParty party)
	{
		if(party.getSize() > partySize)
			return PartyStatus.TOOLARGE;

		checkActiveParty();

		if(!partyList.contains(party))
			partyList.add(party);
		else
		{
			//Trying to make sure the party reference in the party list is the most recent
			partyList.remove(party);
			partyList.add(party);
		}

		if(activeParty != null && !activeParty.equals(party))
		{
			return PartyStatus.INQUEUE;
		}

		if(party.getSize() == partySize)
			return PartyStatus.READY;
		else
			return PartyStatus.MISSING_PLAYERS;
	}

	public boolean removePlayer(Player p)
	{
		boolean retVal = false;

		ArrayList<DungeonParty> toRemove = new ArrayList<DungeonParty>();
		for(DungeonParty dp : partyList)
		{
			if(dp.containsMember(p))
			{
				dp.removeMember(p.getName());
				if(dp.getSize() == 0)
					toRemove.add(dp);
				retVal = true;
			}
		}

		for(DungeonParty dp : toRemove)
		{
			partyList.remove(dp);
			if(activeParty == dp)
				activeParty = null;
		}

		return retVal;
	}

	public void checkActiveParty(String ... ignorePlayers)
	{
		if(activeParty == null)
			return;

		partyloop: for(String pname : activeParty.listMembers())
		{
			for(String name : ignorePlayers)
			{
				if(pname.equals(name))
					continue partyloop;
			}

			if(plugin.isPlayerIdle(pname))
				continue partyloop;

			//The player is logged out but hasn't hit the idle timer yet, lets consider that as him being "in" the dungeon still
			if(plugin.idleTimer.containsKey(pname))
				return;

			Player p = plugin.server.getPlayer(pname);
			if(p == null)
				continue partyloop;

			Location loc = p.getLocation();
			if(containsLocation(loc))
			{
				return;
			}
		}

		removeParty(activeParty);
	}

	public boolean removeParty(DungeonParty dp)
	{
		if(partyList.contains(dp))
		{
			partyList.remove(dp);
			if(dp == activeParty)
			{
				activeParty = null;
				for(DungeonParty party : partyList)
				{
					for(String pname : party.listMembers())
					{
						Player p = plugin.server.getPlayer(pname);
						if(p != null)
							p.sendMessage("The dungeon is now available");
					}
				}
			}

			return true;
		}
		else
			return false;
	}

	public void setActiveParty(DungeonParty party)
	{
		activeParty = party;
	}

	public DungeonParty getActiveParty()
	{
		return activeParty;
	}

	//public PartyStatus addPlayer(Player p)
	//{
	//	if(currentPlayers.size() < partySize && !currentPlayers.contains(p))
	//	{
	//		currentPlayers.add(p);
	//		if(currentPlayers.size() == partySize)
	//		{
	//			currentStatus = PartyStatus.FULL;
	//			return PartyStatus.READY;
	//		}
	//		else
	//		{
	//			currentStatus = PartyStatus.MISSING_PLAYERS;
	//			return PartyStatus.MISSING_PLAYERS;
	//		}
	//	}
	//	else if(currentPlayers.size() < partySize)
	//	{
	//		currentStatus = PartyStatus.MISSING_PLAYERS;
	//		return currentStatus;
	//	}
	//	else if(currentPlayers.size() >= partySize)
	//	{
	//		if(currentPlayers.contains(p))
	//		{
	//			currentStatus = PartyStatus.FULL;
	//			return PartyStatus.READY;
	//		}
	//		else
	//		{
	//			currentStatus = PartyStatus.FULL;
	//			return currentStatus;
	//		}
	//	}

	//	return currentStatus;
	//}

	//public boolean removePlayer(Player p)
	//{
	//	if(currentPlayers.contains(p))
	//	{
	//		currentPlayers.remove(p);

	//		if(currentPlayers.size() == 0)
	//		{
	//			if(partySize > 1)
	//				currentStatus = PartyStatus.EMPTY;
	//			else
	//				currentStatus = PartyStatus.READY;
	//		}
	//		else if(currentPlayers.size() < partySize)
	//			currentStatus = PartyStatus.MISSING_PLAYERS;
	//		else if(currentPlayers.size() == partySize)
	//			currentStatus = PartyStatus.FULL;

	//		return true;
	//	}
	//	else
	//		return false;
	//}

	//public ArrayList<Player> listPlayers()
	//{
	//	return new ArrayList<Player>(currentPlayers);
	//}

	public void toggleAutoload(boolean enabled)
	{
		autoload = enabled;
	}

	public boolean getAutoload()
	{
		return autoload;
	}

	public void addMonsterTrigger(Player p, String alias, String monsterAlias)
	{
		addMonsterTrigger(p.getLocation(), alias, monsterAlias, 1);
	}

	public LocationWrapper addMonsterTrigger(Player p, String alias, String monsterAlias, int blockcount)
	{
		return addMonsterTrigger(p.getLocation(), alias, monsterAlias, blockcount);
	}

	private LocationWrapper addMonsterTrigger(Location l, String alias, String monsterAlias)
	{
		return addMonsterTrigger(l, alias, monsterAlias, 1);
	}

	private LocationWrapper addMonsterTrigger(Location l, String alias, String monsterAlias, int blockcount)
	{
		LocationWrapper lw = new LocationWrapper(alias, l, LocationWrapper.LocationType.MONSTER_TRIGGER, this);
		lw.setMetaData(monsterAlias);
		lw.setBlockCount(blockcount);

		addMonsterTrigger(lw);

		return lw;
	}

	private void addMonsterTrigger(LocationWrapper lw)
	{
		String monsterAlias = lw.getMetaData();
		String alias = lw.getAlias();

		ArrayList<String> toRemove = new ArrayList<String>();
		for(LocationWrapper lw2 : monsterTriggers.values())
		{
			if(lw2.getMetaData().equals(monsterAlias))
			{
				toRemove.add(lw2.getAlias());
			}
		}

		for(String ra : toRemove)
			monsterTriggers.remove(ra);

		monsterTriggers.put(alias, lw);
	}

	public boolean removeMonsterTrigger(String alias)
	{
		if(!monsterTriggers.containsKey(alias))
			return false;

		String monsterAlias = monsterTriggers.get(alias).getMetaData();

		monsterTriggers.remove(alias);

		boolean found = false;
		for(LocationWrapper lw : monsterTriggers.values())
		{	
			if(lw.getMetaData().equals(monsterAlias))
				found = true;
		}
		
		if(!found)
			addMonsterTrigger(start, monsterAlias, monsterAlias);

		return true;
	}

	public ArrayList<LocationWrapper> listMonsterTriggers()
	{
		return new ArrayList<LocationWrapper>(monsterTriggers.values());
	}

	public LocationWrapper addSavePoint(Player p, String alias, int blockcount)
	{
		LocationWrapper lw = new LocationWrapper(alias, p.getLocation(), LocationWrapper.LocationType.SAVE_POINT, this);
		lw.setBlockCount(blockcount);

		addSavePoint(lw);

		return lw;
	}

	public void addSavePoint(LocationWrapper lw)
	{
		savePoints.put(lw.getAlias(), lw);
	}

	public boolean removeSavePoint(String alias)
	{
		if(savePoints.containsKey(alias))
		{
			savePoints.remove(alias);
			return true;
		}

		return false;
	}

	public ArrayList<LocationWrapper> listSavePoints()
	{
		return new ArrayList<LocationWrapper>(savePoints.values());
	}

	public LocationWrapper addScriptTrigger(Player p, String alias, String methodname, int blockcount)
	{
		LocationWrapper lw = new LocationWrapper(alias, p.getLocation(), LocationWrapper.LocationType.SCRIPT_TRIGGER, this);
		lw.setBlockCount(blockcount);
		lw.setMetaData(methodname);

		addScriptTrigger(lw);

		return lw;
	}

	public void addScriptTrigger(LocationWrapper lw)
	{
		scriptTriggers.put(lw.getAlias(), lw);
	}

	public boolean removeScriptTrigger(String alias)
	{
		if(scriptTriggers.containsKey(alias))
		{
			scriptTriggers.remove(alias);
			return true;
		}

		return false;
	}

	public ArrayList<LocationWrapper> listScriptTriggers()
	{
		return new ArrayList<LocationWrapper>(scriptTriggers.values());
	}

	private class MonsterInfo
	{
		private Location loc;
		private String alias, type;
		private ArrayList<Entity> liveMonsters = new ArrayList<Entity>();
		private int count = 1;

		public MonsterInfo(String line)
			throws Exception
		{
			this(line, false);
		}
		
		public MonsterInfo(String line, boolean relative)
			throws Exception
		{
			line = line.substring(2);
			String [] comps = line.split(",");
			this.alias = comps[0];
			this.loc = createLocation(world, comps[1], comps[2], comps[3], relative);
			this.type = comps[4];

			try
			{
				if(comps.length == 6)
					this.count = Integer.parseInt(comps[5]);
			}
			catch(Exception e)
			{
				System.out.println("Invalid line: " + line);
			}
		}

		public MonsterInfo(Location l, String alias, String type, int count)
		{
			this.loc = l;
			this.alias = alias;
			this.type = type;
			this.count = count;
		}

		public String getLine(boolean relative)
		{
			String retVal = "M:" + alias + "," + createLocationString(loc, relative) + "," + type + "," + count;
			return retVal;
		}

		public String getAlias()
		{
			return alias;
		}

		public Location getLoc()
		{
			return loc;
		}

		public String getType()
		{
			return type;
		}
		
		public int getCount()
		{
			return count;
		}

		@Override public boolean equals(Object o2)
		{
			if(!(o2 instanceof MonsterInfo))
				return false;

			return this.getAlias().equals(((MonsterInfo)o2).getAlias());
		}

		public void spawnMonsters()
		{
			if(type.equals("pig-zombie"))
				type = "PIG_ZOMBIE";

			CreatureType ct = null;

			try
			{
				ct = CreatureType.valueOf(type.toUpperCase());
			}
			catch(Exception e)
			{
				System.out.println("Invalid creature type: " + type);
				return;
			}

			if(ct == null)
			{
				System.out.println("Null creature type found");
				return;
			}

			for(int i = 0; i < count; i++)
			{
				liveMonsters.add(world.spawnCreature(loc, ct));
			}
		}

		public void killMonsters()
		{
			for(Entity e : liveMonsters)
				e.remove();

			liveMonsters.clear();
		}
	}

}
