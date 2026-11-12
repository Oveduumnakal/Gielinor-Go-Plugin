/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.Client;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;

/**
 * Loads the model the game would draw for an NPC id. Shared by the portraits and the follower. Models can only be
 * read on the game thread, and only while logged in.
 */
final class NpcModels
{
	/** The game's scale value that means "normal size". */
	static final int NORMAL_SCALE = 128;

	/** An NPC's model before it is lit, with what is still needed to size and animate it. */
	static final class Parts
	{
		private final ModelData data;
		private final int npcId;
		private final int size;
		private final int widthScale;
		private final int heightScale;

		private Parts(ModelData data, NPCComposition composition)
		{
			this.data = data;
			this.npcId = composition.getId();
			this.size = composition.getSize();
			this.widthScale = composition.getWidthScale();
			this.heightScale = composition.getHeightScale();
		}

		/** @return the merged and recoloured model, at the size it was modelled; the caller may change it */
		ModelData data()
		{
			return data;
		}

		/** @return the id of the NPC the model belongs to, after the game's own swap for multi-form NPCs */
		int npcId()
		{
			return npcId;
		}

		/** @return how many tiles wide the NPC stands in the game: 1 for a goblin, 5 for a boss dragon */
		int size()
		{
			return size;
		}

		/** @return how wide the game draws this NPC, where {@link #NORMAL_SCALE} is as modelled */
		int widthScale()
		{
			return widthScale;
		}

		/** @return how tall the game draws this NPC, where {@link #NORMAL_SCALE} is as modelled */
		int heightScale()
		{
			return heightScale;
		}

		/** @return the model at the size the game draws it, the same object as {@link #data()} */
		ModelData scaled()
		{
			if (widthScale != NORMAL_SCALE || heightScale != NORMAL_SCALE)
				data.scale(widthScale, heightScale, widthScale);

			return data;
		}
	}

	private NpcModels()
	{
	}

	/**
	 * Loads an NPC's parts, merges them and applies its recolours.
	 *
	 * @param client the game client
	 * @param npcId the NPC id
	 * @return the model, or null when the NPC has no model the client can load
	 */
	static Parts load(Client client, int npcId)
	{
		NPCComposition composition = client.getNpcDefinition(npcId);
		if (composition != null && composition.getConfigs() != null)
			composition = composition.transform();

		int[] parts = composition == null ? null : composition.getModels();
		if (parts == null || parts.length == 0)
			return null;

		ModelData[] loaded = new ModelData[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			loaded[i] = client.loadModelData(parts[i]);
			if (loaded[i] == null)
				return null;
		}

		ModelData whole = loaded.length == 1 ? loaded[0].shallowCopy() : client.mergeModels(loaded);
		whole.cloneVertices();
		whole.cloneColors();
		short[] from = composition.getColorToReplace();
		short[] to = composition.getColorToReplaceWith();
		for (int i = 0; from != null && to != null && i < Math.min(from.length, to.length); i++)
			whole.recolor(from[i], to[i]);

		return new Parts(whole, composition);
	}
}
