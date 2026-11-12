/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import org.junit.Test;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Checks the bundled animation list and the sums that size and colour the follower. */
public class FollowerDataTest
{
	private static final double CLOSE = 0.0001;

	/** The skeleton_update_mage_casting sequence, the cast of a Skeleton Mage (a MAGIC species on its own skeleton). */
	private static final int SKELETON_MAGE_CAST = 5523;

	private final Gson gson = new Gson();

	@Test
	public void theBundledListKnowsAStandingAndAWalkingAnimation() throws IOException
	{
		AnimationTable table = bundled();

		assertTrue("expected at least 1,000 NPC ids, got " + table.size(), table.size() >= 1000);
		assertEquals(1506, table.idle(2));
		assertEquals(1505, table.walk(2));
	}

	@Test
	public void mostSpeciesCanBeAnimated() throws IOException
	{
		AnimationTable table = bundled();
		SpeciesTable species;
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		int drawable = 0;
		int animated = 0;
		for (Species entry : species.all())
		{
			int[] ids = entry.ids();
			if (ids.length == 0)
				continue;

			drawable++;
			if (table.idle(ids[0]) != AnimationTable.NONE)
				animated++;
		}

		assertTrue("only " + animated + " of " + drawable + " species have a standing animation",
			animated >= drawable * 95 / 100);
	}

	@Test
	public void theBundledListKnowsCombatAnimations() throws IOException
	{
		AnimationTable table = bundled();

		assertEquals(6188, table.attack(3046));
		assertEquals(AnimationTable.NONE, table.defend(3046));
		assertEquals(250, table.attack(1792));
		assertEquals(251, table.defend(1792));
		assertEquals(253, table.death(1792));
	}

	@Test
	public void theAttackAnimationFitsTheCombatStyle() throws IOException
	{
		AnimationTable table = bundled();

		assertEquals(711, table.attack(2056));
		assertEquals(426, table.attack(4096));
		assertEquals(422, table.attack(2841));
		assertEquals(SKELETON_MAGE_CAST, table.attack(4319));
		assertEquals("a crossbow user uses the crossbow", 427, table.attack(1675));
		assertEquals("a thrower throws", 385, table.attack(13664));
		assertEquals("a melee monkey does not shoot a bow", 1392, table.attack(5258));
		assertEquals("a skeletal wyvern has an attack", 2989, table.attack(468));
		assertEquals("a wyrm casts", 8271, table.attack(8610));
	}

	@Test
	public void aDragonBitesAndBreathesFire() throws IOException
	{
		AnimationTable table = bundled();

		assertEquals("the green dragon's bite", 80, table.attack(7868));
		assertEquals("the green dragon's breath", 81, table.secondAttack(7868));
		assertEquals("Vorkath's second attack", 7952, table.secondAttack(8061));
		assertEquals("a wyvern's breath", 7653, table.secondAttack(7795));
		assertEquals("a baby dragon only bites", AnimationTable.NONE, table.secondAttack(244));
		assertEquals("a goblin has no second attack", AnimationTable.NONE, table.secondAttack(3046));
		SpeciesTable species;
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		assertEquals(CombatType.MELEE, species.find("Green dragon").style());
		assertEquals(CombatType.MAGIC, species.find("Green dragon").secondStyle());
		assertEquals(0.25, species.find("Green dragon").secondChance(), 0.0001);
		assertNull(species.find("Baby green dragon").secondStyle());
		assertNull(species.find("Goblin").secondStyle());
	}

	@Test
	public void mostSpeciesHaveAnAttackAndADeathAnimation() throws IOException
	{
		AnimationTable table = bundled();
		SpeciesTable species;
		try (Reader reader = open(SpeciesTable.RESOURCE))
		{
			species = SpeciesTable.load(gson, reader);
		}

		int drawable = 0;
		int attacking = 0;
		int dying = 0;
		for (Species entry : species.all())
		{
			int[] ids = entry.ids();
			if (ids.length == 0)
				continue;

			drawable++;
			if (table.attack(ids[0]) != AnimationTable.NONE)
				attacking++;

			if (table.death(ids[0]) != AnimationTable.NONE)
				dying++;
		}

		assertTrue("only " + attacking + " of " + drawable + " species attack", attacking >= drawable * 70 / 100);
		assertTrue("only " + dying + " of " + drawable + " species die", dying >= drawable * 70 / 100);
	}

	@Test
	public void anOlderListWithOnlyStandingAndWalkingStillLoads()
	{
		AnimationTable table = AnimationTable.load(gson, new StringReader("{\"5\":[100,200],\"6\":[1,2,3,4,5]}"));

		assertEquals(100, table.idle(5));
		assertEquals(AnimationTable.NONE, table.attack(5));
		assertEquals(AnimationTable.NONE, table.defend(5));
		assertEquals(AnimationTable.NONE, table.death(5));
		assertEquals(3, table.attack(6));
		assertEquals(4, table.defend(6));
		assertEquals(5, table.death(6));
		assertEquals(AnimationTable.NONE, table.secondAttack(5));
		assertEquals(AnimationTable.NONE, table.secondAttack(6));
	}

	@Test
	public void aRowWithASecondAttackLoads()
	{
		AnimationTable table = AnimationTable.load(gson,
			new StringReader("{\"7\":[1,2,3,4,5,6],\"8\":[1,2,3,4,5,-1]}"));

		assertEquals(6, table.secondAttack(7));
		assertEquals(AnimationTable.NONE, table.secondAttack(8));
	}

	@Test
	public void aDeathAnimationHeldOnItsLastFrameStaysThere()
	{
		Animation death = mock(Animation.class);
		when(death.getFrameLengths()).thenReturn(new int[]{5, 5, 5, 5});
		when(death.getFrameStep()).thenReturn(-1);
		when(death.getDuration()).thenReturn(20);
		AnimationController controller = new AnimationController(null, death);
		controller.setOnFinished(CreatureBody::hold);

		for (int clientTick = 0; clientTick < 2000; clientTick++)
			controller.tick(1);

		assertSame(death, controller.getAnimation());
		assertEquals(3, controller.getFrame());
	}

	@Test
	public void anUnknownNpcHasNoAnimation()
	{
		AnimationTable table = AnimationTable.load(gson, new StringReader("{\"5\":[100,200]}"));

		assertEquals(1, table.size());
		assertEquals(AnimationTable.NONE, table.idle(6));
		assertEquals(AnimationTable.NONE, table.walk(6));
	}

	@Test
	public void aCreatureWithNoWalkUsesItsStandingAnimation()
	{
		AnimationTable table = AnimationTable.load(gson, new StringReader("{\"5\":[100,-1],\"6\":[-1,200]}"));

		assertEquals(100, table.walk(5));
		assertEquals(AnimationTable.NONE, table.idle(6));
		assertEquals(200, table.walk(6));
	}

	@Test
	public void aBrokenListLoadsAsEmpty()
	{
		assertEquals(0, AnimationTable.load(gson, new StringReader("[1,2")).size());
		assertEquals(0, AnimationTable.load(gson, new StringReader("")).size());
	}

	@Test
	public void oneTileCreaturesKeepTheirSize()
	{
		assertEquals(1.0, FollowerService.fit(1, 35, 196), CLOSE);
		assertEquals(1.0, FollowerService.fit(1, FollowerService.MAX_REACH, FollowerService.MAX_HEIGHT), CLOSE);
	}

	@Test
	public void bigCreaturesShrinkToAboutOneTile()
	{
		assertEquals(FollowerService.MAX_TILES / 2, FollowerService.fit(2, 125, 150), CLOSE);
		assertEquals(FollowerService.MAX_TILES / 5, FollowerService.fit(5, 349, 532), CLOSE);
	}

	@Test
	public void wideWingsAndTallHeadsShrinkFurther()
	{
		assertEquals(0.25, FollowerService.fit(2, 4 * FollowerService.MAX_REACH, 100), CLOSE);
		assertEquals(0.5, FollowerService.fit(1, 40, 2 * FollowerService.MAX_HEIGHT), CLOSE);
		assertEquals(0.125, FollowerService.fit(3, 4 * FollowerService.MAX_REACH, 8 * FollowerService.MAX_HEIGHT),
			CLOSE);
	}

	@Test
	public void followersAreShownAtThreeQuartersOfTheirFittedSize()
	{
		assertEquals(0.75, FollowerService.SHOWN_SIZE, CLOSE);
	}

	@Test
	public void gildingKeepsLightAndDarkButNotTheColour()
	{
		short darkBlue = (short) (43 << 10 | 5 << 7 | 30);
		short paleGreen = (short) (21 << 10 | 2 << 7 | 90);

		assertEquals(7 << 10 | 6 << 7 | 30, FollowerService.gilded(darkBlue));
		assertEquals(7 << 10 | 6 << 7 | 90, FollowerService.gilded(paleGreen));
	}

	@Test
	public void gildingBlackOrWhiteStillLooksGold()
	{
		assertEquals(7 << 10 | 6 << 7 | 24, FollowerService.gilded((short) 0));
		assertEquals(7 << 10 | 6 << 7 | 104, FollowerService.gilded((short) 127));
	}

	@Test
	public void gildingAGoldChangesNothing()
	{
		for (int color = Short.MIN_VALUE; color <= Short.MAX_VALUE; color++)
		{
			short once = FollowerService.gilded((short) color);
			assertEquals(once, FollowerService.gilded(once));
		}
	}

	@Test
	public void anAnimationWithNoLoopPointStillPlaysForever()
	{
		Animation humanWalk = mock(Animation.class);
		when(humanWalk.getFrameLengths()).thenReturn(new int[]{6, 6, 6, 6, 6, 6, 6, 6});
		when(humanWalk.getFrameStep()).thenReturn(-1);
		when(humanWalk.getDuration()).thenReturn(48);
		AnimationController controller = FollowerService.looping(null, humanWalk);

		for (int clientTick = 0; clientTick < 1000; clientTick++)
		{
			controller.tick(1);
			assertSame("the animation was dropped at client tick " + clientTick, humanWalk, controller.getAnimation());
			assertTrue("frame " + controller.getFrame(), controller.getFrame() >= 0 && controller.getFrame() < 8);
		}
	}

	@Test
	public void aSkippedFrameDoesNotEndTheAnimation()
	{
		Animation humanWalk = mock(Animation.class);
		when(humanWalk.getFrameLengths()).thenReturn(new int[]{6, 6});
		when(humanWalk.getFrameStep()).thenReturn(-1);
		when(humanWalk.getDuration()).thenReturn(12);
		AnimationController controller = FollowerService.looping(null, humanWalk);

		controller.tick(500);

		assertSame(humanWalk, controller.getAnimation());
	}

	private AnimationTable bundled() throws IOException
	{
		try (Reader reader = open(AnimationTable.RESOURCE))
		{
			return AnimationTable.load(gson, reader);
		}
	}

	private static Reader open(String resource)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(resource);
		assertNotNull(resource + " is missing from the resources", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}
}
