package net.virtuallyabstract.minecraft;

import javax.script.*;
import java.io.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;
import java.util.*;

public class ScriptManager
{
	private static ScriptEngineManager factory;
	private static HashMap<String, HashMap<String, Object>> persistedObjects;

	static
	{
		factory = new ScriptEngineManager();
		persistedObjects = new HashMap<String, HashMap<String, Object>>();
	}

	public static boolean scriptingEnabled()
	{
		ScriptEngine engine = factory.getEngineByName("groovy");
		if(engine == null)
			return false;
		else
			return true;
	}

	private static String findDungeonScript(Dungeon d)
	{
		File test = new File(d.getFileName() + ".groovy");
		if(test.exists())
			return test.getPath();

		test = new File(d.getFileName() + ".js");
		if(test.exists())
			return test.getPath();

		return null;
	}

	public static void runDungeonStartScript(Dungeon d, Server s, Player p, Plugin plugin)
	{
		runScript(d, s, p, plugin, "dungeon_start");
	}

	public static void runDungeonExitScript(Dungeon d, Server s, Player p, Plugin plugin)
	{
		runScript(d, s, p, plugin, "dungeon_exit");
	}

	public static void runScript(Dungeon d, Server s, Player p, Plugin plugin, String methodName)
	{
		String script = findDungeonScript(d);
		if(script == null)
			return;

		ScriptEngine engine = null;
		if(script.endsWith("groovy"))
			engine = factory.getEngineByName("groovy");
		if(script.endsWith("js"))
			engine = factory.getEngineByName("js");

		if(engine == null)
		{
			System.out.println("Hmm...found a script: " + script + " but couldn't find an engine for it");
			return;
		}

		if(!persistedObjects.containsKey(script))
			persistedObjects.put(script, new HashMap<String, Object>());

		if(d != null)
			engine.put("dungeon", d);
		if(s != null)
			engine.put("server", s);
		if(p != null)
			engine.put("player", p);
		if(plugin != null)
			engine.put("plugin", plugin);
		engine.put("persistedObjects", persistedObjects.get(script));
		try
		{
			engine.eval(new FileReader(script));
			if(engine instanceof Invocable)
			{
				Invocable inv = (Invocable)engine;
				inv.invokeFunction(methodName);	
			}
		}
		catch(NoSuchMethodException nsme)
		{
			//Ignore
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void runMonsterScript(Dungeon d, Server s, Player p, Plugin plugin, Entity m, String scriptName, String methodName)
	{
		File script = new File("plugins/dungeons/monsters/" + scriptName);
		if(!script.exists())
		{
			System.out.println("Unable to locate script: " + script.getPath());
			return;
		}

		ScriptEngine engine = null;
		if(scriptName.endsWith("groovy"))
			engine = factory.getEngineByName("groovy");
		if(scriptName.endsWith("js"))
			engine = factory.getEngineByName("js");

		if(engine == null)
		{
			System.out.println("Unable to find engine for script: " + script.getPath());
			return;
		}

		if(!persistedObjects.containsKey(scriptName))
			persistedObjects.put(scriptName, new HashMap<String, Object>());

		if(d != null)
			engine.put("dungeon", d);
		if(s != null)
			engine.put("server", s);
		if(p != null)
			engine.put("player", p);
		if(plugin != null)
			engine.put("plugin", plugin);
		if(m != null)
			engine.put("monster", m);
		engine.put("persistedObjects", persistedObjects.get(scriptName));

		try
		{
			engine.eval(new FileReader(script));
			if(engine instanceof Invocable)
			{
				Invocable inv = (Invocable)engine;
				inv.invokeFunction(methodName);
			}
		}
		catch(NoSuchMethodException nsme)
		{
			//Ignore
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
