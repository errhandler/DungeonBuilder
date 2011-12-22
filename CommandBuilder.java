package net.virtuallyabstract.minecraft;

import org.bukkit.entity.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.util.*;

public class CommandBuilder
{
	private static Category rootNode;
	private Category activeCat = null;
	private Command activeCom = null;

	static
	{
		parseXML();
	}

	public CommandBuilder()
	{
	}

	public void update(Player p, String value)
	{
		update(p, value, true);
	}

	public boolean isDone()
	{
		if(activeCom == null)
			return false;

		return activeCom.isDone();
	}

	public String getCommand()
	{
		if(activeCom == null)
			return null;

		return activeCom.getCommand();
	}

	public String [] getArgs()
	{
		if(activeCom == null)
			return null;

		return activeCom.getArgs();
	}

	public void setActiveCategory(Category c)
	{
		activeCat = c;
		activeCom = null;
	}

	public void setActiveCommand(Command c)
	{
		activeCom = new Command(c);
		activeCat = null;
	}

	public void reset()
	{
		activeCom = null;
		activeCat = null;
	}

	public void update(Player p, String value, boolean showPrompt)
	{
		if(value.equals("reset"))
		{
			reset();
			return;
		}

		if(activeCom != null)
		{
			activeCom.update(this, value);
			if(showPrompt)
				nextPrompt(p);
		}

		if(activeCat != null)
		{
			activeCat.update(this, value);
			if(showPrompt)
				nextPrompt(p);
		}
	
		if(activeCom == null && activeCat == null)
		{
			rootNode.update(this, value);
			if(showPrompt)
				nextPrompt(p);
		}
	}

	public void nextPrompt(Player p)
	{
		if(activeCat != null)
			displayCategory(activeCat, p);

		if(activeCom != null)
			displayCommand(activeCom, p);

		if(activeCom == null && activeCat == null)
			displayCategory(rootNode, p);
	}

	private void displayCategory(Category c, Player p)
	{
		p.sendMessage("----Available Commands and Sub-Categories----");
		p.sendMessage("-----------------------------------------");
		for(Command com : c.listCommands())
		{
			p.sendMessage(com.toString());
		}

		for(Category subCat : c.listCategories())
		{
			p.sendMessage(subCat.toString());
		}
	}

	private void displayCommand(Command c, Player p)
	{
		p.sendMessage("-------------Command Arguments---------------");
		p.sendMessage("--------------------------------------------");
		for(Argument arg : c.listArguments())
		{
			p.sendMessage(arg.toString());
		}
	}

	private static void parseXML()
	{
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(CommandBuilder.class.getResourceAsStream("/CommandMap.xml"));
			NodeList nodes = doc.getElementsByTagName("CommandMap");
			for(int i = 0; i < nodes.getLength(); i++)
			{
				Node n = nodes.item(i);

				NamedNodeMap atts = n.getAttributes();
				String rootCommand = atts.getNamedItem("rootCommand").getNodeValue();
				rootNode = new Category(rootCommand, "");
			
				NodeList children = n.getChildNodes();
				for(int p = 0; p < children.getLength(); p++)
				{
					parseNode(children.item(p), rootNode);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void parseNode(Node n, Category parent)
	{
		String name = n.getNodeName();
		if(name.equals("Category"))
		{
			Category cat = parseCategory(n);
			parent.addCategory(cat);
		}

		if(name.equals("Command"))
		{
			Command com = parseCommand(n);
			parent.addCommand(com);
		}
	}

	private static Category parseCategory(Node n)
	{
		NamedNodeMap atts = n.getAttributes();
		String name = atts.getNamedItem("name").getNodeValue();
		String desc = atts.getNamedItem("description").getNodeValue();

		Category cat = new Category(name, desc);

		NodeList children = n.getChildNodes();
		for(int i = 0; i < children.getLength(); i++)
		{
			parseNode(children.item(i), cat);
		}

		return cat;
	}

	private static Command parseCommand(Node n)
	{
		NamedNodeMap atts = n.getAttributes();
		String name = atts.getNamedItem("name").getNodeValue();
		String desc = atts.getNamedItem("description").getNodeValue();
		String command = atts.getNamedItem("command").getNodeValue();

		Command com = new Command(name, desc, command);

		NodeList children = n.getChildNodes();
		for(int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);

			if(!child.getNodeName().equals("Argument"))
				continue;

			NamedNodeMap childAtts = child.getAttributes();
			String childName = childAtts.getNamedItem("name").getNodeValue();
			String childDesc = childAtts.getNamedItem("description").getNodeValue();
			String childVal = "";
			if(childAtts.getNamedItem("default") != null)
				childVal = childAtts.getNamedItem("default").getNodeValue();

			Argument arg = new Argument(childName, childDesc, childVal);
			
			com.addArgument(arg);
		}

		return com;
	}
}
