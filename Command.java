package net.virtuallyabstract.minecraft;

import java.util.*;

public class Command implements Comparable<Command>
{
	private ArrayList<Argument> argumentList;
	private ArrayList<Argument> remainingArgs;
	private String name, desc, command;
	private ArrayList<String> args;

	public Command(String name, String description, String command)
	{
		argumentList = new ArrayList<Argument>();
		remainingArgs = new ArrayList<Argument>();
		args = new ArrayList<String>();
		this.name = name;
		this.desc = description;
		this.command = command;
	}

	public Command(Command c)
	{
		argumentList = new ArrayList<Argument>(c.listArguments());
		remainingArgs = new ArrayList<Argument>(c.listArguments());
		args = new ArrayList<String>();
		this.command = c.getCommand();
		this.name = c.getName();
		this.desc = c.getDescription();
	}

	public ArrayList<Argument> listArguments()
	{
		return argumentList;
	}

	public String getName()
	{
		return name;
	}

	@Override public int compareTo(Command c2)
	{
		return name.compareTo(c2.getName());
	}

	public String getDescription()
	{
		return desc;
	}

	public void addArgument(Argument a)
	{
		argumentList.add(a);
	}

	public void reset()
	{
		remainingArgs = new ArrayList<Argument>(argumentList);
		args = new ArrayList<String>();
	}

	public boolean isDone()
	{
		return remainingArgs.size() == 0;
	}

	public String getCommand()
	{
		return command;
	}

	public String [] getArgs()
	{
		return args.toArray(new String[0]);
	}

	public void update(CommandBuilder cb, String value)
	{
		if(remainingArgs.size() == 0)
			return;

		Argument arg = remainingArgs.remove(0);
		String val = arg.getDefaultValue();
		if(value.length() != 0)
			val = value;

		args.add(val);
	}

	public boolean equals(String name)
	{
		return this.name.equals(name);
	}

	public String toString()
	{
		return name + " - " + desc;
	}

	public Argument nextArgument()
	{
		if(remainingArgs.size() > 0)
			return remainingArgs.get(0);
		else
			return null;
	}
}
