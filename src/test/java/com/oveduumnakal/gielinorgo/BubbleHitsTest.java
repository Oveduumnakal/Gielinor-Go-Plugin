/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the click targets a frame gives: nothing for none, topmost first, the slop, the clickable rule and modal. */
public class BubbleHitsTest
{
	private static BubbleFrame.Disc disc(BubbleKind kind, double x, double y, double radius, boolean clickable)
	{
		return new BubbleFrame.Disc(kind, x, y, radius, 1.0, 1.0, 0.0, null, 0, false, false, 0.0, clickable);
	}

	private static BubbleFrame.Card card(Rectangle box, boolean clickable)
	{
		return new BubbleFrame.Card(box, null, false, 1.0, 1.0, BubbleFrame.Side.RIGHT, clickable);
	}

	@Test
	public void emptyCoversNothingAndIsNotModal()
	{
		assertFalse(BubbleHits.EMPTY.covers(0, 0));
		assertFalse(BubbleHits.EMPTY.covers(400, 300));
		assertNull(BubbleHits.EMPTY.at(400, 300));
		assertFalse(BubbleHits.EMPTY.modal());
		assertEquals(0, BubbleHits.EMPTY.generation());
		assertEquals(0, BubbleHits.EMPTY.size());
	}

	@Test
	public void theNoneFrameHasEmptyHits()
	{
		assertEquals(0, BubbleFrame.NONE.hits().size());
		assertFalse(BubbleFrame.NONE.hits().modal());
	}

	@Test
	public void aPointOnADiscHitsItAndAPointFarAwayMisses()
	{
		BubbleHits hits = new BubbleFrame(Arrays.asList(disc(BubbleKind.JAR, 100, 100, 15, true)), null, 4, false)
			.hits();

		assertEquals(BubbleKind.JAR, hits.at(100, 100).kind());
		assertEquals(BubbleKind.JAR, hits.at(110, 100).kind());
		assertNull(hits.at(200, 200));
		assertEquals(4, hits.generation());
	}

	@Test
	public void theSlopGrowsEveryTargetByTwoPixels()
	{
		BubbleHits hits = new BubbleFrame(Arrays.asList(disc(BubbleKind.JAR, 100, 100, 15, true)),
			card(new Rectangle(200, 200, 50, 40), true), 1, false).hits();

		assertTrue(hits.covers(100 + 17, 100));
		assertFalse(hits.covers(100 + 17.5, 100));
		assertTrue(hits.covers(198, 200));
		assertFalse(hits.covers(197, 200));
		assertTrue(hits.covers(252, 242));
		assertFalse(hits.covers(253, 242));
	}

	@Test
	public void aDiscUsesItsPaintedSizeNotItsBaseRadius()
	{
		BubbleFrame.Disc big = new BubbleFrame.Disc(BubbleKind.JAR, 100, 100, 15, 1.15, 1.0, 1.0, null, 0, false,
			false, 0.0, true);

		BubbleHits hits = new BubbleFrame(Arrays.asList(big), null, 1, false).hits();

		assertTrue(hits.covers(100 + 15 * 1.15 + 1.5, 100));
		assertFalse(hits.covers(100 + 15 * 1.15 + 2.5, 100));
	}

	@Test
	public void theTopmostTargetWinsCardThenClusterThenMain()
	{
		List<BubbleFrame.Disc> discs = Arrays.asList(disc(BubbleKind.JAR, 100, 100, 15, true),
			disc(BubbleKind.CLUSTER_JAR, 105, 100, 12, true));
		BubbleFrame frame = new BubbleFrame(discs, card(new Rectangle(95, 90, 40, 40), true), 1, false);

		BubbleHits.Hit top = frame.hits().at(105, 100);

		assertEquals(BubbleKind.CARD, top.kind());

		BubbleFrame noCard = new BubbleFrame(discs, null, 1, false);

		BubbleHits hits = noCard.hits();

		assertEquals(BubbleKind.CLUSTER_JAR, hits.at(105, 100).kind());
		assertEquals(BubbleKind.JAR, hits.at(88, 100).kind());
	}

	@Test
	public void discsThatAreNotClickableAreLeftOut()
	{
		BubbleHits hits = new BubbleFrame(Arrays.asList(disc(BubbleKind.JAR, 100, 100, 15, false),
			disc(BubbleKind.BATTLE, 200, 100, 15, true)), card(new Rectangle(0, 0, 10, 10), false), 1, false).hits();

		assertEquals(1, hits.size());
		assertFalse(hits.covers(100, 100));
		assertFalse(hits.covers(5, 5));
		assertTrue(hits.covers(200, 100));
	}

	@Test
	public void aJarHitCarriesItsJar()
	{
		BubbleFrame.Disc slot = new BubbleFrame.Disc(BubbleKind.CLUSTER_JAR, 50, 50, 12, 1.0, 1.0, 0.0,
			JarType.RUNE, 3, false, false, 0.0, true);

		BubbleHits hits = new BubbleFrame(Arrays.asList(slot), null, 1, false).hits();

		assertEquals(JarType.RUNE, hits.at(50, 50).jar());
	}

	@Test
	public void modalIsKeptEvenWhenNothingIsClickable()
	{
		BubbleHits hits = new BubbleFrame(new ArrayList<BubbleFrame.Disc>(), null, 3, true).hits();

		assertTrue(hits.modal());
		assertEquals(3, hits.generation());
	}

	@Test
	public void theHitsDoNotChangeWhenTheListTheyWereBuiltFromChanges()
	{
		List<BubbleHits.Hit> source = new ArrayList<>();
		source.add(BubbleHits.Hit.disc(BubbleKind.JAR, null, 10, 10, 5));
		BubbleHits hits = new BubbleHits(1, false, source);

		source.clear();

		assertTrue(hits.covers(10, 10));
		assertEquals(1, hits.size());
	}

	@Test
	public void theFramesDiscsAndCardRectangleAreCopies()
	{
		List<BubbleFrame.Disc> source = new ArrayList<>();
		source.add(disc(BubbleKind.JAR, 100, 100, 15, true));
		Rectangle box = new Rectangle(200, 200, 50, 40);
		BubbleFrame frame = new BubbleFrame(source, card(box, true), 1, false);

		source.clear();
		box.x = 900;

		assertEquals(1, frame.discs().size());
		Rectangle kept = frame.card().bounds();

		assertEquals(200, kept.x);
		assertTrue(frame.hits().covers(225, 220));
	}
}
