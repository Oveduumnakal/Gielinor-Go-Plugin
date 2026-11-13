/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * A creature family such as all goblins: completing it, by catching every member, is a Creature Log milestone. One
 * entry of the bundled {@code families.json}, which {@code scripts/gen-families.py} writes.
 */
final class Family
{
	/** Resource path of the bundled family list, relative to this package. */
	static final String RESOURCE = "families.json";

	private static final Type LIST_TYPE = new TypeToken<List<Family>>()
	{
	}.getType();

	private String id;
	private String label;
	private String jar;
	private List<String> species;

	/** Used by Gson. */
	Family()
	{
	}

	Family(String id, String label, JarType jar, List<String> species)
	{
		this.id = id;
		this.label = label;
		this.jar = jar.name();
		this.species = species;
	}

	/**
	 * Reads the families from JSON.
	 *
	 * @param gson the Gson to parse with
	 * @param reader the JSON array of families
	 * @return the families, empty when the JSON is missing or malformed
	 */
	static List<Family> load(Gson gson, Reader reader)
	{
		try
		{
			List<Family> loaded = gson.fromJson(reader, LIST_TYPE);
			return loaded == null ? Collections.emptyList() : loaded;
		}
		catch (JsonParseException e)
		{
			return Collections.emptyList();
		}
	}

	/** @return the key the family's milestone is saved under, without its prefix */
	String id()
	{
		return id;
	}

	/** @return the family's name in the plural, such as {@code Goblins} */
	String label()
	{
		return label == null ? id : label;
	}

	/** @return the specialty jar paid for completing the family, Crush when the data names none the game knows */
	JarType jar()
	{
		for (JarType type : JarType.values())
		{
			if (type.specialty() && type.name().equals(jar))
				return type;
		}

		return JarType.CRUSH;
	}

	/** @return the species keys of the members, as {@link Species#key()} writes them */
	List<String> species()
	{
		return species == null ? Collections.emptyList() : new ArrayList<>(species);
	}
}
