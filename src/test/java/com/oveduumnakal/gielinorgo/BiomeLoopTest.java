/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests the bundled backgrounds, which species uses which, and their playing. */
public class BiomeLoopTest
{
	private static final List<String> BIOMES = Arrays.asList("cave-dungeon", "coast-sea", "crypt-haunted", "desert",
		"forest-woodland", "grassland-farmland", "otherworldly", "snow-mountain", "swamp-jungle", "town-castle",
		"volcano-fire", "wilderness");

	@Test
	public void aFrameIsTheReferencePlusTwiceTheDifference()
	{
		assertEquals(0xFF646464, BiomeLoop.rebuild(0xFF646464, 0xFF808080));
		assertEquals(0xFF6E5A64, BiomeLoop.rebuild(0xFF646464, 0xFF857B80));
		assertEquals(0xFFFF0000, BiomeLoop.rebuild(0xFFC83214, 0xFFFF0000));
	}

	@Test
	public void theFrameFollowsTheClockAndLoops()
	{
		assertEquals(0, BiomeLoop.frameAt(0, 16, 65));
		assertEquals(16, BiomeLoop.frameAt(1000, 16, 65));
		assertEquals(0, BiomeLoop.frameAt(4063, 16, 65));
		assertEquals(64, BiomeLoop.frameAt(-1, 16, 65));
	}

	@Test
	public void everyBundledLoopLoadsAndDecodes() throws IOException
	{
		for (String biome : BIOMES)
		{
			BiomeLoop loop = BiomeLoop.load(new Gson(), biome);
			assertTrue(biome, loop.frames() >= 57);
			for (int index : new int[] {0, loop.frames() - 1})
			{
				BufferedImage frame = loop.decode(index);
				assertEquals(biome, 480, frame.getWidth());
				assertEquals(biome, 264, frame.getHeight());
			}
		}
	}

	@Test
	public void everySpeciesHasOneOfTheTwelveBiomes() throws IOException
	{
		BiomeTable table = BiomeTable.bundled(new Gson());
		Set<String> used = new HashSet<>();
		try (InputStream in = BiomeTable.class.getResourceAsStream(SpeciesTable.RESOURCE);
			Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			NamedSpecies[] all = new Gson().fromJson(reader, NamedSpecies[].class);
			assertTrue(all.length > 1000);
			for (NamedSpecies species : all)
			{
				String biome = table.of(species.name);
				assertTrue(species.name + " " + biome, BIOMES.contains(biome));
				used.add(biome);
			}
		}

		assertEquals(BIOMES.size(), used.size());
		assertNull(table.of("No such monster"));
		assertNull(table.of(null));
	}

	@Test
	public void aPlayerGivesNothingUntilTheLoopIsReadAndLetsItGoWhenNotAsked()
	{
		List<Runnable> queued = new ArrayList<>();
		BiomePlayer player = BiomePlayer.bundled(queued::add, new Gson());

		assertNull(player.frame("desert", 1000, true));
		assertNull("only one decode is queued at a time", player.frame("desert", 1000, true));
		assertEquals(1, queued.size());

		queued.remove(0).run();
		assertNotNull(player.frame("desert", 1000, false));

		player.sweep(1000 + BiomePlayer.IDLE_MS);
		assertTrue(player.holding());
		player.sweep(1001 + BiomePlayer.IDLE_MS);
		assertTrue(!player.holding());
	}

	@Test
	public void aStillSceneHoldsTheFirstFrame()
	{
		BiomePlayer player = BiomePlayer.bundled(Runnable::run, new Gson());
		player.frame("coast-sea", 0, false);
		BufferedImage first = player.frame("coast-sea", 0, false);

		assertTrue(first == player.frame("coast-sea", 5000, false));
	}

	@Test
	public void aLoopThatCannotBeReadGivesNoPicture()
	{
		BiomePlayer player = BiomePlayer.bundled(Runnable::run, new Gson());

		assertNull(player.frame("no-such-biome", 0, true));
		assertNull(player.frame("no-such-biome", 100, true));
	}

	/** The one field of a species this test reads. */
	private static final class NamedSpecies
	{
		private String name;
	}
}
