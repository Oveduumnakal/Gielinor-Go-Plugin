/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

/**
 * Builds the models of the projectile looks, each one once. A built model is never changed afterwards, as it may be
 * drawn by the see-through path of the GPU plugin, which sorts its faces; a look that cannot be built is remembered
 * and shows no projectile. Every method must be called on the game thread.
 */
final class ProjectileModels
{
	private static final Logger log = LoggerFactory.getLogger(ProjectileModels.class);

	/** Light direction the game uses for a spot animation. */
	private static final int LIGHT_X = -30;

	private static final int LIGHT_Y = -50;

	private static final int LIGHT_Z = -30;

	private final Client client;
	private final Map<ProjectileLook, Model> built = new EnumMap<>(ProjectileLook.class);
	private final Set<ProjectileLook> failed = EnumSet.noneOf(ProjectileLook.class);

	/**
	 * Creates the cache with nothing built.
	 *
	 * @param client the game client
	 */
	ProjectileModels(Client client)
	{
		this.client = client;
	}

	/**
	 * Gives the model of a look, building it the first time.
	 *
	 * @param look the look
	 * @return the lit model, or null when it cannot be built, so no projectile is shown for that look
	 */
	Model modelOf(ProjectileLook look)
	{
		Model model = built.get(look);
		if (model != null || failed.contains(look))
			return model;

		model = build(look);
		if (model == null)
			failed.add(look);
		else
			built.put(look, model);

		return model;
	}

	/**
	 * Builds a look's model: load, copy (the client's own arrays are shared and must stay as they are),
	 * recolour, resize (only for a model that does not animate, as an animated one is resized after it is posed, see
	 * {@link BattleProjectile}), light.
	 *
	 * @param look the look
	 * @return the model, or null when the cache has no such model or building it fails
	 */
	private Model build(ProjectileLook look)
	{
		try
		{
			ModelData loaded = client.loadModelData(look.modelId());
			if (loaded == null)
				return null;

			ModelData data = loaded.shallowCopy();
			data.cloneColors();

			short[] from = look.recolorFrom();
			short[] to = look.recolorTo();
			for (int i = 0; i < Math.min(from.length, to.length); i++)
				data.recolor(from[i], to[i]);

			if (!look.animated() && look.size() != ProjectileLook.NORMAL_SIZE)
			{
				data.cloneVertices();
				data.scale(look.size(), look.size(), look.size());
			}

			return data.light(look.ambient(), look.contrast(), LIGHT_X, LIGHT_Y, LIGHT_Z);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not build the projectile model {}", look, e);
			return null;
		}
	}
}
