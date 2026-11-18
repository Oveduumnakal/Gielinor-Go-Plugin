/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the bubble state machine with a fake clock: opening, grace, hold zones, the cluster, the card and presses. */
public class WildBubblesTest
{
	private static final Rectangle VIEW = new Rectangle(0, 0, 800, 600);
	private static final BubbleAnchors ANCHORS = new BubbleAnchors(400, 320, 400, 260);
	private static final BubbleAnchors OTHER = new BubbleAnchors(150, 320, 150, 260);
	private static final long T0 = 100_000;
	private static final int ID = 7;
	private static final int FAR_X = 700;
	private static final int FAR_Y = 560;

	private final WildBubbles bubbles = new WildBubbles();
	private final BubbleLayout.Arc arc = BubbleLayout.main(ANCHORS, VIEW);

	private static JarSuggest.JarChoice choice(JarType jar, int owned, double chance)
	{
		return new JarSuggest.JarChoice(jar, owned, chance, false);
	}

	private static BubbleOffer offer(int id, JarType jar, boolean ready, boolean knockedOut,
		JarSuggest.JarChoice... owned)
	{
		return new BubbleOffer(id, jar, Arrays.asList(owned), ready, knockedOut, null);
	}

	private static BubbleOffer offer()
	{
		return offer(ID, JarType.BRONZE, true, false, choice(JarType.BRONZE, 5, 0.5), choice(JarType.IRON, 2, 0.6),
			choice(JarType.RUNE, 1, 0.7));
	}

	private void tick(BubbleOffer offer, BubbleAnchors anchors, boolean near, double px, double py, long now,
		List<Rectangle> blockers)
	{
		bubbles.tick(offer, anchors, VIEW, near, blockers, (int) Math.round(px), (int) Math.round(py), now,
			MotionMode.FULL);
	}

	private void tick(boolean near, double px, double py, long now)
	{
		tick(offer(), ANCHORS, near, px, py, now, Collections.emptyList());
	}

	private BubbleFrame frame(long now)
	{
		return bubbles.frame(id -> id == ID ? ANCHORS : id == 8 ? OTHER : null, VIEW, new Dimension(190, 140), now,
			MotionMode.FULL);
	}

	private long open()
	{
		tick(true, 400, 320, T0);
		tick(true, 400, 320, T0 + 800);
		return T0 + 800;
	}

	private int generation(long now)
	{
		BubbleHits hits = frame(now).hits();
		return hits.generation();
	}

	private BubbleFrame.Disc disc(BubbleFrame frame, BubbleKind kind)
	{
		for (BubbleFrame.Disc disc : frame.discs())
		{
			if (disc.kind() == kind)
				return disc;
		}

		return null;
	}

	private BubbleFrame.Disc clusterDisc(BubbleFrame frame, JarType jar)
	{
		for (BubbleFrame.Disc disc : frame.discs())
		{
			if (disc.kind() == BubbleKind.CLUSTER_JAR && disc.jar() == jar)
				return disc;
		}

		return null;
	}

	private void hold(double px, double py, long from, long to)
	{
		for (long t = from; t <= to; t += 50)
		{
			tick(false, px, py, t);
			assertTrue("showing at " + (t - from), bubbles.showing(ID));
		}
	}

	@Test
	public void theBubblesOpenOnlyWhenThePointerIsNear()
	{
		tick(false, 400, 320, T0);

		assertEquals(WildBubbles.NO_TARGET, bubbles.wildId());
		assertSame(BubbleFrame.NONE, frame(T0));

		tick(true, 400, 320, T0 + 10);

		assertTrue(bubbles.showing(ID));
		assertEquals(ID, bubbles.wildId());
		assertEquals(3, frame(T0 + 10).discs().size());
	}

	@Test
	public void theBubblesDoNotOpenUnderABlocker()
	{
		List<Rectangle> blockers = Collections.singletonList(arc.bounds().getBounds());

		tick(offer(), ANCHORS, true, 400, 320, T0, blockers);

		assertFalse(bubbles.showing(ID));
		assertSame(BubbleFrame.NONE, frame(T0));
	}

	@Test
	public void aBlockerThatAppearsDismissesTheOpenBubbles()
	{
		long t = open();
		List<Rectangle> blockers = Collections.singletonList(arc.bounds().getBounds());

		tick(offer(), ANCHORS, true, 400, 320, t + 50, blockers);

		assertFalse(bubbles.showing(ID));
		assertEquals(0, frame(t + 60).hits().size());
	}

	@Test
	public void theBubblesGoBackOnlyAfterTheGraceTime()
	{
		long t = open();

		tick(false, FAR_X, FAR_Y, t);
		tick(false, FAR_X, FAR_Y, t + 349);

		assertTrue(bubbles.showing(ID));

		tick(false, FAR_X, FAR_Y, t + 350);

		assertFalse(bubbles.showing(ID));
		assertEquals(ID, bubbles.wildId());
	}

	@Test
	public void comingBackInsideTheGraceTimeStartsItAgain()
	{
		long t = open();
		tick(false, FAR_X, FAR_Y, t);
		tick(false, FAR_X, FAR_Y, t + 300);

		tick(true, 400, 320, t + 340);
		tick(false, FAR_X, FAR_Y, t + 400);
		tick(false, FAR_X, FAR_Y, t + 700);

		assertTrue(bubbles.showing(ID));
	}

	@Test
	public void reEnteringWhileTheyGoBackReversesFromWhereTheyAre()
	{
		long t = open();
		tick(false, FAR_X, FAR_Y, t);
		tick(false, FAR_X, FAR_Y, t + 350);
		long mid = t + 550;
		double before = disc(frame(mid), BubbleKind.JAR).scale();

		tick(true, 400, 320, mid);

		double after = disc(frame(mid), BubbleKind.JAR).scale();
		assertTrue("partway " + before, before < 0.99 && before > 0.3);
		assertEquals(before, after, 1e-9);
		assertTrue(bubbles.showing(ID));

		tick(true, 400, 320, mid + 3000);

		assertTrue(bubbles.showing(ID));
		assertEquals(1.0, disc(frame(mid + 3000), BubbleKind.JAR).scale(), 1e-9);
	}

	@Test
	public void theCorridorKeepsTheBubblesOpenWithTheMouseNotNearTheBody()
	{
		long t = open();

		hold(400, 290, t, t + 5000);
	}

	@Test
	public void theClusterZoneKeepsTheBubblesOpen()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);
		BubbleFrame.Disc slot = clusterDisc(frame(t + 800), JarType.IRON);

		hold(slot.x(), slot.y(), t + 800, t + 5000);

		assertTrue(bubbles.clusterOpen());
	}

	@Test
	public void theCardZoneKeepsTheBubblesOpen()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.INFO, null, 1, t);
		BubbleFrame.Card card = frame(t + 500).card();
		Rectangle box = card.bounds();

		hold(box.getCenterX(), box.getCenterY(), t + 500, t + 5000);

		assertTrue(bubbles.cardPinned());
	}

	@Test
	public void aNullOfferDismissesAtOnce()
	{
		long t = open();

		tick(null, ANCHORS, true, 400, 320, t + 10, Collections.emptyList());

		assertEquals(WildBubbles.NO_TARGET, bubbles.wildId());
		assertFalse(bubbles.showing(ID));
	}

	@Test
	public void pickKeepsTheCurrentCreatureEvenWhenAnotherIsNearer()
	{
		List<WildBubbles.Candidate> all = Arrays.asList(new WildBubbles.Candidate(1, true, 900),
			new WildBubbles.Candidate(2, true, 4));

		WildBubbles.Pick pick = WildBubbles.pick(1, true, true, all);

		assertEquals(1, pick.wildId());
		assertTrue(pick.inside());
	}

	@Test
	public void pickSwitchesToTheNearestWhenTheCurrentIsNotInside()
	{
		List<WildBubbles.Candidate> all = Arrays.asList(new WildBubbles.Candidate(1, false, 100),
			new WildBubbles.Candidate(2, true, 400), new WildBubbles.Candidate(3, true, 50));

		WildBubbles.Pick pick = WildBubbles.pick(1, true, false, all);

		assertEquals(3, pick.wildId());
		assertTrue(pick.inside());
	}

	@Test
	public void pickBreaksATieByTheLowerId()
	{
		List<WildBubbles.Candidate> all = Arrays.asList(new WildBubbles.Candidate(9, true, 25),
			new WildBubbles.Candidate(4, true, 25));

		assertEquals(4, WildBubbles.pick(WildBubbles.NO_TARGET, false, false, all).wildId());
	}

	@Test
	public void pickKeepsTheCurrentOutsideSoTheGraceTimeRuns()
	{
		List<WildBubbles.Candidate> all = Collections.singletonList(new WildBubbles.Candidate(2, false, 4));

		WildBubbles.Pick pick = WildBubbles.pick(1, true, false, all);

		assertEquals(1, pick.wildId());
		assertFalse(pick.inside());
	}

	@Test
	public void pickGivesNoneWhenNothingIsNearAndTheCurrentIsGone()
	{
		List<WildBubbles.Candidate> all = Collections.singletonList(new WildBubbles.Candidate(2, false, 4));

		assertEquals(WildBubbles.NO_TARGET, WildBubbles.pick(1, false, true, all).wildId());
		assertEquals(WildBubbles.NO_TARGET, WildBubbles.pick(WildBubbles.NO_TARGET, false, false, all).wildId());
	}

	@Test
	public void aSwitchRetiresTheOldGroupWithNoHitsAndOpensTheNewOneAtOnce()
	{
		long t = open();
		BubbleFrame.Disc old = disc(frame(t), BubbleKind.JAR);
		BubbleOffer second = offer(8, JarType.BRONZE, true, false, choice(JarType.BRONZE, 1, 0.5));

		tick(second, OTHER, true, 150, 320, t + 10, Collections.emptyList());

		assertEquals(8, bubbles.wildId());
		assertFalse(bubbles.showing(ID));
		BubbleFrame during = frame(t + 100);
		assertEquals(6, during.discs().size());
		assertNull(during.hits().at(old.x(), old.y()));
		assertEquals(3, frame(t + 900).discs().size());
		assertNotNull(frame(t + 900).hits().at(OTHER.headX(), disc(frame(t + 900), BubbleKind.BATTLE).y()));
	}

	@Test
	public void aLeavingGroupKeepsPaintingUntilItHasFlownBackThenVanishes()
	{
		long t = open();

		bubbles.dismiss(t);

		assertFalse(bubbles.showing(ID));
		assertEquals(WildBubbles.NO_TARGET, bubbles.wildId());
		assertEquals(3, frame(t + 100).discs().size());
		assertEquals(0, frame(t + 100).hits().size());
		assertSame(BubbleFrame.NONE, frame(t + 2000));
	}

	@Test
	public void theClusterOpensAfterTheJarIsHoveredForNineHundredMilliseconds()
	{
		long t = open();
		double x = arc.x(0);
		double y = arc.y(0);

		tick(true, x, y, t);
		tick(true, x, y, t + 899);

		assertFalse(bubbles.clusterOpen());

		tick(true, x, y, t + 900);

		assertTrue(bubbles.clusterOpen());
	}

	@Test
	public void leavingTheJarResetsTheHoverTimer()
	{
		long t = open();

		tick(true, arc.x(0), arc.y(0), t);
		tick(true, 400, 320, t + 500);
		tick(true, arc.x(0), arc.y(0), t + 600);
		tick(true, arc.x(0), arc.y(0), t + 1400);

		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void aRightClickOnTheJarTogglesTheClusterAndAsksForNothing()
	{
		long t = open();
		int gen = generation(t);

		BubbleAction first = bubbles.press(gen, BubbleKind.JAR, null, 3, t);

		assertEquals(BubbleAction.Kind.NONE, first.kind());
		assertTrue(bubbles.clusterOpen());
		assertTrue(frame(t + 800).hits().modal());

		BubbleAction second = bubbles.press(gen, BubbleKind.JAR, null, 3, t + 800);

		assertEquals(BubbleAction.Kind.NONE, second.kind());
		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void aRightClickOnTheJarOpensNothingWhenNoJarIsOwned()
	{
		tick(offer(ID, null, true, false), ANCHORS, true, 400, 320, T0, Collections.emptyList());
		tick(offer(ID, null, true, false), ANCHORS, true, 400, 320, T0 + 800, Collections.emptyList());

		BubbleAction action = bubbles.press(generation(T0 + 800), BubbleKind.JAR, null, 3, T0 + 800);

		assertEquals(BubbleAction.Kind.NONE, action.kind());
		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void clickingAClusterJarSelectsItClosesTheClusterAndNeverThrows()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);
		long later = t + 800;
		BubbleFrame.Disc slot = clusterDisc(frame(later), JarType.IRON);
		assertTrue(slot.clickable());

		BubbleAction action = bubbles.press(generation(later), BubbleKind.CLUSTER_JAR, JarType.IRON, 1, later);

		assertEquals(BubbleAction.Kind.SELECT_JAR, action.kind());
		assertEquals(JarType.IRON, action.jar());
		assertEquals(ID, action.wildId());
		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void aRightClickOnAClusterJarSelectsItToo()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);

		BubbleAction action = bubbles.press(generation(t + 800), BubbleKind.CLUSTER_JAR, JarType.RUNE, 3, t + 800);

		assertEquals(BubbleAction.Kind.SELECT_JAR, action.kind());
		assertEquals(JarType.RUNE, action.jar());
	}

	@Test
	public void aJarThatIsNotOwnedCannotBeSelected()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);

		BubbleAction action = bubbles.press(generation(t + 800), BubbleKind.CLUSTER_JAR, JarType.DRAGON, 1, t + 800);

		assertEquals(BubbleAction.Kind.NONE, action.kind());
		assertTrue(bubbles.clusterOpen());
	}

	@Test
	public void afterASelectionTheHoverStaysDisarmedUntilThePointerLeavesTheJar()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);
		long later = t + 800;
		bubbles.press(generation(later), BubbleKind.CLUSTER_JAR, JarType.IRON, 1, later);
		double x = arc.x(0);
		double y = arc.y(0);

		for (long u = later; u <= later + 3000; u += 100)
			tick(true, x, y, u);

		assertFalse(bubbles.clusterOpen());

		tick(true, 400, 320, later + 3100);
		tick(true, x, y, later + 3200);
		tick(true, x, y, later + 4099);

		assertFalse(bubbles.clusterOpen());

		tick(true, x, y, later + 4100);

		assertTrue(bubbles.clusterOpen());
	}

	@Test
	public void aLeftClickOnTheJarThrowsTheShownJarAndClosesTheCluster()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);

		BubbleAction action = bubbles.press(generation(t + 800), BubbleKind.JAR, null, 1, t + 800);

		assertEquals(BubbleAction.Kind.THROW, action.kind());
		assertEquals(ID, action.wildId());
		assertEquals(JarType.BRONZE, action.jar());
		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void aLeftClickOnTheJarStillThrowsWhenNoJarIsLeftAndTheBubbleIsGrey()
	{
		BubbleOffer none = offer(ID, null, true, false);
		tick(none, ANCHORS, true, 400, 320, T0, Collections.emptyList());
		tick(none, ANCHORS, true, 400, 320, T0 + 800, Collections.emptyList());

		BubbleAction action = bubbles.press(generation(T0 + 800), BubbleKind.JAR, null, 1, T0 + 800);

		assertEquals(BubbleAction.Kind.THROW, action.kind());
		assertNull(action.jar());
		assertTrue(disc(frame(T0 + 800), BubbleKind.JAR).disabled());
		assertEquals(0, disc(frame(T0 + 800), BubbleKind.JAR).count());
	}

	@Test
	public void theJarBubbleShowsTheCountOfTheJarItThrows()
	{
		long t = open();

		BubbleFrame.Disc jar = disc(frame(t), BubbleKind.JAR);

		assertEquals(5, jar.count());
		assertEquals(JarType.BRONZE, jar.jar());
		assertFalse(jar.disabled());
	}

	@Test
	public void aLeftClickOnBattleAsksForABattleEvenWhenItIsGrey()
	{
		BubbleOffer grey = offer(ID, JarType.BRONZE, false, false, choice(JarType.BRONZE, 1, 0.5));
		tick(grey, ANCHORS, true, 400, 320, T0, Collections.emptyList());
		tick(grey, ANCHORS, true, 400, 320, T0 + 800, Collections.emptyList());

		BubbleAction left = bubbles.press(generation(T0 + 800), BubbleKind.BATTLE, null, 1, T0 + 800);
		BubbleAction right = bubbles.press(generation(T0 + 800), BubbleKind.BATTLE, null, 3, T0 + 800);

		assertEquals(BubbleAction.Kind.BATTLE, left.kind());
		assertEquals(ID, left.wildId());
		assertEquals(BubbleAction.Kind.NONE, right.kind());
		assertTrue(disc(frame(T0 + 800), BubbleKind.BATTLE).disabled());
	}

	@Test
	public void aReadyBattleBubbleIsNotGrey()
	{
		long t = open();

		assertFalse(disc(frame(t), BubbleKind.BATTLE).disabled());
		assertFalse(disc(frame(t), BubbleKind.INFO).disabled());
	}

	@Test
	public void aKnockedOutCreatureMakesTheJarRingPulse()
	{
		BubbleOffer ko = offer(ID, JarType.BRONZE, false, true, choice(JarType.BRONZE, 1, 0.9));
		tick(ko, ANCHORS, true, 400, 320, T0, Collections.emptyList());
		tick(ko, ANCHORS, true, 400, 320, T0 + 800, Collections.emptyList());

		double a = disc(frame(T0 + 800), BubbleKind.JAR).pulse();
		double b = disc(frame(T0 + 800 + 300), BubbleKind.JAR).pulse();

		assertTrue(a != b);
		assertEquals(0.0, disc(frame(T0 + 800), BubbleKind.BATTLE).pulse(), 0.0);
	}

	@Test
	public void theCardPeeksAfterTheInfoBubbleIsHoveredForOneHundredFiftyMilliseconds()
	{
		long t = open();
		double x = arc.x(2);
		double y = arc.y(2);

		tick(true, x, y, t);
		tick(true, x, y, t + 149);

		assertFalse(bubbles.cardShown());

		tick(true, x, y, t + 150);

		assertTrue(bubbles.cardShown());
		assertFalse(bubbles.cardPinned());
		assertNotNull(frame(t + 500).card());
	}

	@Test
	public void anUnpinnedCardHidesTwoHundredMillisecondsAfterThePointerLeaves()
	{
		long t = open();
		tick(true, arc.x(2), arc.y(2), t);
		tick(true, arc.x(2), arc.y(2), t + 150);
		long u = t + 600;

		tick(false, 400, 290, u);
		tick(false, 400, 290, u + 199);

		assertTrue(bubbles.cardShown());

		tick(false, 400, 290, u + 200);

		assertFalse(bubbles.cardShown());
		assertTrue(bubbles.showing(ID));
		assertNull(frame(u + 1000).card());
	}

	@Test
	public void aLeftClickOnInfoPinsTheCardAndAnotherUnpinsIt()
	{
		long t = open();
		int gen = generation(t);

		BubbleAction first = bubbles.press(gen, BubbleKind.INFO, null, 1, t);

		assertEquals(BubbleAction.Kind.NONE, first.kind());
		assertTrue(bubbles.cardPinned());
		assertTrue(frame(t + 500).card().pinned());
		assertTrue(frame(t + 500).hits().modal());

		bubbles.press(gen, BubbleKind.INFO, null, 1, t + 500);

		assertFalse(bubbles.cardPinned());
		assertTrue(bubbles.cardShown());
	}

	@Test
	public void aRightClickOnInfoDoesNothing()
	{
		long t = open();

		BubbleAction action = bubbles.press(generation(t), BubbleKind.INFO, null, 3, t);

		assertEquals(BubbleAction.Kind.NONE, action.kind());
		assertFalse(bubbles.cardShown());
	}

	@Test
	public void aPressOnTheCardKeepsItPinned()
	{
		long t = open();
		int gen = generation(t);
		bubbles.press(gen, BubbleKind.INFO, null, 1, t);

		BubbleAction left = bubbles.press(gen, BubbleKind.CARD, null, 1, t + 500);
		BubbleAction right = bubbles.press(gen, BubbleKind.CARD, null, 3, t + 500);

		assertEquals(BubbleAction.Kind.NONE, left.kind());
		assertEquals(BubbleAction.Kind.NONE, right.kind());
		assertTrue(bubbles.cardPinned());
	}

	@Test
	public void aPressOutsideUnpinsAndHidesTheCardAndClosesTheCluster()
	{
		long t = open();
		int gen = generation(t);
		bubbles.press(gen, BubbleKind.INFO, null, 1, t);

		bubbles.pressedOutside(gen, t + 500);

		assertFalse(bubbles.cardShown());
		assertFalse(frame(t + 500).hits().modal());

		bubbles.press(gen, BubbleKind.JAR, null, 3, t + 600);
		assertTrue(bubbles.clusterOpen());

		bubbles.pressedOutside(gen, t + 700);

		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void leavingTheRadiusClosesAPinnedCardWithTheGroup()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.INFO, null, 1, t);
		long u = t + 500;

		tick(false, FAR_X, FAR_Y, u);
		tick(false, FAR_X, FAR_Y, u + 350);

		assertFalse(bubbles.showing(ID));
		assertFalse(bubbles.cardShown());
		assertEquals(0, frame(u + 360).hits().size());
	}

	@Test
	public void openingTheClusterHidesAndUnpinsTheCard()
	{
		long t = open();
		int gen = generation(t);
		bubbles.press(gen, BubbleKind.INFO, null, 1, t);

		bubbles.press(gen, BubbleKind.JAR, null, 3, t + 500);

		assertTrue(bubbles.clusterOpen());
		assertFalse(bubbles.cardShown());
		assertFalse(bubbles.cardPinned());
	}

	@Test
	public void pinningTheCardClosesTheCluster()
	{
		long t = open();
		int gen = generation(t);
		bubbles.press(gen, BubbleKind.JAR, null, 3, t);

		bubbles.press(gen, BubbleKind.INFO, null, 1, t + 800);

		assertTrue(bubbles.cardPinned());
		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void theClusterClosesAfterThePointerStaysOutsideItForThreeHundredMilliseconds()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);
		long u = t + 800;

		tick(true, FAR_X, FAR_Y, u);
		tick(true, FAR_X, FAR_Y, u + 299);

		assertTrue(bubbles.clusterOpen());

		tick(true, FAR_X, FAR_Y, u + 300);

		assertFalse(bubbles.clusterOpen());
	}

	@Test
	public void theClusterHoldsTheSelectedJarFirstThenTheSuggestedOnesThenTheRest()
	{
		long t = open();
		bubbles.press(generation(t), BubbleKind.JAR, null, 3, t);

		BubbleFrame frame = frame(t + 800);

		List<JarType> order = new ArrayList<>();
		for (BubbleFrame.Disc disc : frame.discs())
		{
			if (disc.kind() == BubbleKind.CLUSTER_JAR)
				order.add(disc.jar());
		}

		assertEquals(Arrays.asList(JarType.BRONZE, JarType.RUNE, JarType.IRON), order);
		assertTrue(clusterDisc(frame, JarType.BRONZE).selected());
		assertFalse(clusterDisc(frame, JarType.RUNE).selected());
		assertEquals(2, clusterDisc(frame, JarType.IRON).count());
	}

	@Test
	public void aPressWithAStaleGenerationOrAfterTheGroupClosedDoesNothing()
	{
		long t = open();
		int gen = generation(t);

		assertEquals(BubbleAction.Kind.NONE, bubbles.press(gen + 1, BubbleKind.BATTLE, null, 1, t).kind());
		assertEquals(BubbleAction.Kind.NONE, bubbles.press(gen - 1, BubbleKind.JAR, null, 1, t).kind());

		tick(false, FAR_X, FAR_Y, t);
		tick(false, FAR_X, FAR_Y, t + 350);

		assertEquals(BubbleAction.Kind.NONE, bubbles.press(gen, BubbleKind.BATTLE, null, 1, t + 360).kind());
	}

	@Test
	public void aMiddleButtonPressDoesNothing()
	{
		long t = open();

		assertEquals(BubbleAction.Kind.NONE, bubbles.press(generation(t), BubbleKind.BATTLE, null, 2, t).kind());
	}

	@Test
	public void aNewGroupGetsANewGeneration()
	{
		long t = open();
		int first = generation(t);
		bubbles.dismiss(t);
		tick(true, 400, 320, t + 2000);

		int second = generation(t + 2000 + 800);

		assertTrue(second != first);
	}

	@Test
	public void pausedBubblesPaintNothingAndTakeNoClicksThenReturnWithoutReopening()
	{
		long t = open();
		int gen = generation(t);

		bubbles.pause(true);

		assertSame(BubbleFrame.NONE, frame(t + 10));
		assertSame(BubbleHits.EMPTY, frame(t + 10).hits());
		assertFalse(bubbles.covers(arc.x(0), arc.y(0)));
		assertFalse(bubbles.holds(arc.x(0), arc.y(0)));
		assertEquals(BubbleAction.Kind.NONE, bubbles.press(gen, BubbleKind.BATTLE, null, 1, t + 10).kind());

		bubbles.pause(false);

		assertTrue(bubbles.showing(ID));
		assertEquals(1.0, disc(frame(t + 20), BubbleKind.JAR).scale(), 1e-9);
		assertEquals(3, frame(t + 20).hits().size());
	}

	@Test
	public void theGraceTimeCountsFromTheResumeAfterAPause()
	{
		long t = open();
		tick(false, FAR_X, FAR_Y, t);
		bubbles.pause(true);
		bubbles.pause(false);
		long u = t + 10_000;

		tick(false, FAR_X, FAR_Y, u);
		tick(false, FAR_X, FAR_Y, u + 349);

		assertTrue(bubbles.showing(ID));

		tick(false, FAR_X, FAR_Y, u + 350);

		assertFalse(bubbles.showing(ID));
	}

	@Test
	public void ticksWhilePausedChangeNothing()
	{
		long t = open();
		bubbles.pause(true);

		tick(false, FAR_X, FAR_Y, t + 5000);
		bubbles.pause(false);

		assertTrue(bubbles.showing(ID));
	}

	@Test
	public void hideNowDropsEverythingAtOnce()
	{
		long t = open();

		bubbles.hideNow();

		assertEquals(WildBubbles.NO_TARGET, bubbles.wildId());
		assertSame(BubbleFrame.NONE, frame(t + 1));
	}

	@Test
	public void aBubbleIsClickableOnlyOnceItHasFlownHalfWay()
	{
		tick(true, 400, 320, T0);

		BubbleHits early = frame(T0).hits();

		assertEquals(0, early.size());
		assertEquals(0, frame(T0 + 50).hits().size());

		BubbleHits late = frame(T0 + 800).hits();

		assertEquals(3, late.size());
		assertEquals(BubbleKind.JAR, late.at(arc.x(0), arc.y(0)).kind());
		assertEquals(BubbleKind.BATTLE, late.at(arc.x(1), arc.y(1)).kind());
		assertEquals(BubbleKind.INFO, late.at(arc.x(2), arc.y(2)).kind());
	}

	@Test
	public void theModelCoversABubbleButNotTheCorridor()
	{
		open();

		assertTrue(bubbles.covers(arc.x(0), arc.y(0)));
		assertFalse(bubbles.covers(400, 290));
		assertTrue(bubbles.holds(400, 290));
		assertFalse(bubbles.holds(FAR_X, FAR_Y));
	}

	@Test
	public void theNearConstantsMatchTheSpec()
	{
		assertEquals(2, WildBubbles.NEAR_TILES);
		assertEquals(350, WildBubbles.GRACE_MS);
		assertEquals(900, WildBubbles.CLUSTER_HOVER_MS);
		assertEquals(150, WildBubbles.CARD_HOVER_MS);
		assertEquals(2, WildBubbles.HIT_SLOP);
		assertEquals(0.5, WildBubbles.CLICKABLE_AT, 0.0);
	}
}
