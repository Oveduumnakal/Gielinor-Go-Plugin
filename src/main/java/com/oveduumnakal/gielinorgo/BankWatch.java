/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Finds banks for {@link BankSpots} from the client: bankers, bank booths and chests that spawn, the ones already in
 * the scene when the plugin starts or a save loads (which never fire a spawn event), and whether the bank interface
 * is open. The decision about what counts as a bank is {@link BankKinds}.
 */
final class BankWatch
{
	private static final Logger log = LoggerFactory.getLogger(BankWatch.class);

	private final Client client;
	private final BankSpots spots;
	private final Map<Integer, Boolean> objectKinds = new HashMap<>();

	/**
	 * Creates a watch.
	 *
	 * @param client the game client
	 * @param spots where found banks are kept
	 */
	BankWatch(Client client, BankSpots spots)
	{
		this.client = client;
		this.spots = spots;
	}

	/**
	 * Remembers a monster if it is a banker.
	 *
	 * @param npc the monster
	 */
	void noteNpc(NPC npc)
	{
		try
		{
			NPCComposition composition = npc.getTransformedComposition();
			WorldPoint point = npc.getWorldLocation();
			if (composition != null && point != null && BankKinds.npcOffersBank(composition.getActions()))
				spots.add(npc, point.getX(), point.getY(), point.getPlane());
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not check a monster for a bank", e);
		}
	}

	/**
	 * Remembers an object if it is a bank booth or chest. What an object id offers is looked up once and kept.
	 *
	 * @param object a game, wall, decorative or ground object
	 */
	void noteObject(TileObject object)
	{
		try
		{
			if (object == null || !objectKinds.computeIfAbsent(object.getId(), this::objectOffersBank))
				return;

			WorldPoint point = object.getWorldLocation();
			if (point != null)
				spots.add(object, point.getX(), point.getY(), point.getPlane());
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not check an object for a bank", e);
		}
	}

	/**
	 * Looks at everything within {@link BankSpots#NEAR_TILES} tiles of the player, on the player's plane, and
	 * remembers each bank it finds: every kind of scene object and every monster in view. This finds banks that were
	 * already on screen when the plugin started or a save loaded.
	 */
	void scanAroundPlayer()
	{
		try
		{
			Player player = client.getLocalPlayer();
			WorldView view = client.getTopLevelWorldView();
			if (player == null || view == null)
				return;

			scanTiles(view, player);
			for (NPC npc : view.npcs())
				noteNpc(npc);
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not look around for a bank", e);
		}
	}

	/** @return whether the bank interface is open */
	boolean bankOpen()
	{
		try
		{
			Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
			return bank != null && !bank.isHidden();
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not check the bank interface", e);
			return false;
		}
	}

	private void scanTiles(WorldView view, Player player)
	{
		Scene scene = view.getScene();
		LocalPoint here = player.getLocalLocation();
		if (scene == null || here == null)
			return;

		Tile[][][] tiles = scene.getTiles();
		int plane = view.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
			return;

		Tile[][] level = tiles[plane];
		int[] xs = BankSpots.window(here.getSceneX(), BankSpots.NEAR_TILES, level.length);
		for (int x = xs[0]; x <= xs[1]; x++)
		{
			int[] ys = BankSpots.window(here.getSceneY(), BankSpots.NEAR_TILES, level[x].length);
			for (int y = ys[0]; y <= ys[1]; y++)
				scanTile(level[x][y]);
		}
	}

	private void scanTile(Tile tile)
	{
		if (tile == null)
			return;

		for (GameObject object : tile.getGameObjects())
			noteObject(object);

		WallObject wall = tile.getWallObject();
		noteObject(wall);
		DecorativeObject decoration = tile.getDecorativeObject();
		noteObject(decoration);
		GroundObject ground = tile.getGroundObject();
		noteObject(ground);
	}

	private boolean objectOffersBank(int id)
	{
		ObjectComposition composition = client.getObjectDefinition(id);
		if (composition != null && composition.getImpostorIds() != null)
			composition = composition.getImpostor();

		return composition != null && BankKinds.objectOffersBank(composition.getName(), composition.getActions());
	}
}
