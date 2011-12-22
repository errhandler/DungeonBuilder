package net.virtuallyabstract.minecraft;

import java.util.*;

public class Category implements Comparable<Category>
{
	private String name, desc;
	private ArrayList<Category> subCats;
	private ArrayList<Command> commands;

	public Category(String name, String description)
	{
		this.name = name;
		this.desc = description;
		this.subCats = new ArrayList<Category>();
		this.commands = new ArrayList<Command>();
	}

	public void addCategory(Category c)
	{
		subCats.add(c);
		Collections.sort(subCats);
	}

	public void addCommand(Command c)
	{
		commands.add(c);
		Collections.sort(commands);
	}

	public void update(CommandBuilder cb, String value)
	{
		for(Category c : subCats)
		{
			if(c.equals(value))
			{
				cb.setActiveCategory(c);
				return;
			}
		}

		for(Command c : commands)
		{
			if(c.equals(value))
			{
				cb.setActiveCommand(c);
				return;
			}
		}
	}

	public boolean equals(String name)
	{
		return this.name.equals(name);
	}

	@Override public int compareTo(Category c2)
	{
		return name.compareTo(c2.getName());
	}

	public String toString()
	{
		return name + " - " + desc;
	}

	public String getName()
	{
		return name;
	}

	public ArrayList<Command> listCommands()
	{
		return commands;
	}

	public ArrayList<Category> listCategories()
	{
		return subCats;
	}
}
