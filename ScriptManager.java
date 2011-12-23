package net.virtuallyabstract.minecraft;

import javax.script.*;
import java.io.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;

public class ScriptManager
{
	private static ScriptEngineManager factory;

	static
	{
		factory = new ScriptEngineManager();
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

		engine.put("dungeon", d);
		engine.put("server", s);
		engine.put("player", p);
		engine.put("plugin", plugin);
		try
		{
			engine.eval(new FileReader(script));
			if(engine instanceof Invocable)
			{
				Invocable inv = (Invocable)engine;
				inv.invokeFunction(methodName);	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
