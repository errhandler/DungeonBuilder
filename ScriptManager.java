package net.virtuallyabstract.minecraft;

import javax.script.*;
import java.io.*;
import org.bukkit.*;
import org.bukkit.entity.*;

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

	public static void runDungeonStartScript(Dungeon d, Server s, Player p)
	{
		runScript(d, s, p, "dungeon_start");
	}

	public static void runDungeonExitScript(Dungeon d, Server s, Player p)
	{
		runScript(d, s, p, "dungeon_exit");
	}

	public static void runScript(Dungeon d, Server s, Player p, String methodName)
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
