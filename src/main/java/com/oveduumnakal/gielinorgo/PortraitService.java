/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;

/**
 * Draws and remembers creature pictures. The game's models can only be read on the game thread and only while
 * logged in, so requests wait in a queue and a few are drawn each game tick.
 */
final class PortraitService implements PortraitSource
{
	private static final Logger log = LoggerFactory.getLogger(PortraitService.class);

	/** Width and height of a portrait, in pixels. */
	static final int SIZE = 40;

	/** Most portraits drawn in one game tick, so a full team never stalls a frame. */
	static final int PER_TICK = 2;

	/** A picture someone is waiting for. */
	private static final class Request
	{
		private final String key;
		private final Consumer<BufferedImage> onReady;

		private Request(String key, Consumer<BufferedImage> onReady)
		{
			this.key = key;
			this.onReady = onReady;
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final Function<String, Species> speciesLookup;
	private final Map<String, BufferedImage> drawn = new ConcurrentHashMap<>();
	private final Set<String> undrawable = ConcurrentHashMap.newKeySet();
	private final Deque<Request> waiting = new ArrayDeque<>();

	/**
	 * Creates the service.
	 *
	 * @param client the game client
	 * @param clientThread runs work on the game thread
	 * @param speciesLookup finds a species by name or key, or returns null
	 */
	PortraitService(Client client, ClientThread clientThread, Function<String, Species> speciesLookup)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.speciesLookup = speciesLookup;
	}

	@Override
	public void request(String speciesName, Consumer<BufferedImage> onReady)
	{
		String key = Species.keyFor(speciesName);
		BufferedImage ready = drawn.get(key);
		if (ready != null)
		{
			onReady.accept(ready);
			return;
		}

		if (!undrawable.contains(key))
			clientThread.invokeLater(() -> waiting.add(new Request(key, onReady)));
	}

	/** Draws a few of the waiting portraits. Called once per game tick, on the game thread. */
	void tick()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		for (int i = 0; i < PER_TICK && !waiting.isEmpty(); i++)
			fulfil(waiting.poll());
	}

	/** Forgets every picture and every request, for when the plugin stops. */
	void clear()
	{
		drawn.clear();
		undrawable.clear();
		clientThread.invokeLater(waiting::clear);
	}

	private void fulfil(Request request)
	{
		BufferedImage image = drawn.get(request.key);
		if (image == null && !undrawable.contains(request.key))
		{
			image = draw(speciesLookup.apply(request.key));
			if (image == null)
				undrawable.add(request.key);
			else
				drawn.put(request.key, image);
		}

		if (image == null)
			return;

		BufferedImage ready = image;
		SwingUtilities.invokeLater(() -> request.onReady.accept(ready));
	}

	/**
	 * Draws a species from the first of its NPC ids that has a usable model.
	 *
	 * @param species the species, or null
	 * @return the picture, or null when none of its ids can be drawn
	 */
	private BufferedImage draw(Species species)
	{
		if (species == null)
			return null;

		for (int npcId : species.ids())
		{
			try
			{
				Model model = build(npcId);
				BufferedImage image = model == null ? null : PortraitPainter.paint(meshOf(model), SIZE);
				if (image != null)
					return image;
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not draw NPC {}", npcId, e);
			}
		}

		return null;
	}

	/**
	 * Builds the lit model the game would draw for an NPC id: its parts merged, recoloured and scaled.
	 *
	 * @param npcId the NPC id
	 * @return the model, or null when the NPC has no model the client can load
	 */
	private Model build(int npcId)
	{
		NpcModels.Parts parts = NpcModels.load(client, npcId);
		return parts == null ? null : parts.scaled().light();
	}

	private static PortraitPainter.Mesh meshOf(Model model)
	{
		int[][] corners = {model.getFaceIndices1(), model.getFaceIndices2(), model.getFaceIndices3()};
		int[][] colors = {model.getFaceColors1(), model.getFaceColors2(), model.getFaceColors3()};
		return new PortraitPainter.Mesh(model.getVerticesX(), model.getVerticesY(), model.getVerticesZ(), corners,
			colors, model.getFaceTransparencies());
	}
}
