package net.virtuallyabstract.minecraft;

public class Argument
{
	private String name, desc, defaultValue;

	public Argument(String name, String description, String defaultValue)
	{
		this.name = name;
		this.desc = description;
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public String toString()
	{
		if(defaultValue.length() > 0)
			return name + " - " + desc + " (Recommended: " + defaultValue + ")";
		else
			return name + " - " + desc;
	}
}
