/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks how long floating lines stay up, how many fit over one head, and battle playback. */
public class NoticeBoardTest
{
	@Test
	public void aLineShowsThenExpires()
	{
		NoticeBoard board = new NoticeBoard();
		board.show(Notice.player("+1 Bronze jar (walking)", Tone.FIND), 1_000);

		assertEquals(1, board.visible(1_000).size());
		assertEquals(1, board.visible(1_000 + NoticeBoard.LIFE_MS - 1).size());
		assertTrue(board.visible(1_000 + NoticeBoard.LIFE_MS).isEmpty());
	}

	@Test
	public void ageRunsFromZeroToOne()
	{
		NoticeBoard board = new NoticeBoard();
		board.show(Notice.player("x", Tone.INFO), 0);
		NoticeBoard.Shown line = board.visible(0).get(0);

		assertEquals(0.0, line.age(0), 0);
		assertEquals(0.5, line.age(NoticeBoard.LIFE_MS / 2), 0.001);
		assertEquals(1.0, line.age(NoticeBoard.LIFE_MS * 3), 0);
	}

	@Test
	public void eachHeadKeepsOnlyItsNewestLines()
	{
		NoticeBoard board = new NoticeBoard();
		board.show(Notice.creature(7, "other head", Tone.INFO), 0);
		for (int i = 0; i < NoticeBoard.MAX_PER_HEAD + 2; i++)
			board.show(Notice.player("line " + i, Tone.INFO), i);

		List<NoticeBoard.Shown> visible = board.visible(10);

		assertEquals(NoticeBoard.MAX_PER_HEAD + 1, visible.size());
		assertEquals("other head", noticeAt(visible, 0).text());
		assertEquals("line 2", noticeAt(visible, 1).text());
		assertEquals("line 5", noticeAt(visible, visible.size() - 1).text());
	}

	@Test
	public void stageFactoriesKeepTheFallbackHead()
	{
		Notice foe = Notice.stageFoe(7, "hit", Tone.GOOD);
		Notice lead = Notice.stageLead("ouch", Tone.BAD);

		assertEquals(Notice.Anchor.STAGE_FOE, foe.anchor());
		assertEquals(7, foe.npcIndex());
		assertFalse(foe.overPlayer());
		assertEquals(Notice.Anchor.STAGE_LEAD, lead.anchor());
		assertEquals(Notice.PLAYER, lead.npcIndex());
		assertTrue(lead.overPlayer());
		assertEquals(Notice.Anchor.HEAD, Notice.creature(7, "x", Tone.INFO).anchor());
		assertEquals(Notice.Anchor.HEAD, Notice.player("x", Tone.INFO).anchor());
	}

	@Test
	public void anArenaFoeNoticeHasNoMonsterAndStacksByItself()
	{
		Notice foe = Notice.stageFoe("hit", Tone.GOOD);

		assertEquals(Notice.Anchor.STAGE_FOE, foe.anchor());
		assertTrue("with no stage showing it falls back to the player's head", foe.overPlayer());
		assertTrue(foe.sameAnchor(Notice.stageFoe("another", Tone.BAD)));
		assertFalse(foe.sameAnchor(Notice.stageLead("ouch", Tone.BAD)));
		assertFalse(foe.sameAnchor(Notice.player("reward", Tone.FIND)));
		assertFalse(foe.sameAnchor(Notice.stageFoe(7, "wild", Tone.INFO)));

		NoticeBoard board = new NoticeBoard();
		for (int i = 0; i < NoticeBoard.MAX_PER_HEAD + 2; i++)
		{
			board.show(Notice.stageFoe("foe " + i, Tone.INFO), i);
			board.show(Notice.player("player " + i, Tone.INFO), i);
		}

		int foes = 0;
		for (NoticeBoard.Shown line : board.visible(10))
		{
			if (line.notice().anchor() == Notice.Anchor.STAGE_FOE)
				foes++;
		}

		assertEquals(NoticeBoard.MAX_PER_HEAD, foes);
		assertEquals(2 * NoticeBoard.MAX_PER_HEAD, board.visible(10).size());
	}

	@Test
	public void anchorsAreDifferentPlacesEvenOverTheSameMonster()
	{
		Notice head = Notice.creature(7, "a", Tone.INFO);

		assertTrue(head.sameAnchor(Notice.creature(7, "b", Tone.INFO)));
		assertFalse(head.sameAnchor(Notice.stageFoe(7, "b", Tone.INFO)));
		assertFalse(Notice.player("a", Tone.INFO).sameAnchor(Notice.stageLead("b", Tone.INFO)));
		assertTrue(Notice.stageLead("a", Tone.INFO).sameAnchor(Notice.stageLead("b", Tone.BAD)));
	}

	@Test
	public void stageAnchorsStackAndAreCountedEach()
	{
		NoticeBoard board = new NoticeBoard();
		board.show(Notice.player("reward", Tone.FIND), 0);
		for (int i = 0; i < NoticeBoard.MAX_PER_HEAD + 2; i++)
		{
			board.show(Notice.stageFoe(7, "foe " + i, Tone.INFO), i);
			board.show(Notice.stageLead("lead " + i, Tone.INFO), i);
		}

		List<NoticeBoard.Shown> visible = board.visible(10);
		int foes = 0;
		int leads = 0;
		for (NoticeBoard.Shown line : visible)
		{
			if (line.notice().anchor() == Notice.Anchor.STAGE_FOE)
				foes++;
			else if (line.notice().anchor() == Notice.Anchor.STAGE_LEAD)
				leads++;
		}

		assertEquals(NoticeBoard.MAX_PER_HEAD, foes);
		assertEquals(NoticeBoard.MAX_PER_HEAD, leads);
		assertEquals(2 * NoticeBoard.MAX_PER_HEAD + 1, visible.size());
		assertEquals("reward", noticeAt(visible, 0).text());
		assertEquals("foe 2", noticeAt(visible, 1).text());
	}

	@Test
	public void queuedLinesComeOutOnePerTick()
	{
		NoticeBoard board = new NoticeBoard();
		board.queue(Arrays.asList(
			Notice.creature(3, "first", Tone.INFO),
			Notice.player("second", Tone.BAD)));

		assertTrue(board.visible(0).isEmpty());

		board.tick(0);
		assertEquals(1, board.visible(0).size());

		for (int tick = 1; tick < NoticeBoard.LINE_TICKS; tick++)
		{
			board.tick(tick * 600L);
			assertEquals(1, board.visible(tick * 600L).size());
		}

		long due = NoticeBoard.LINE_TICKS * 600L;
		board.tick(due);
		assertEquals(2, board.visible(due).size());
		assertTrue(noticeAt(board.visible(due), 1).overPlayer());
	}

	private static Notice noticeAt(List<NoticeBoard.Shown> lines, int index)
	{
		return lines.get(index).notice();
	}

	@Test
	public void clearRemovesShownAndQueuedLines()
	{
		NoticeBoard board = new NoticeBoard();
		board.show(Notice.player("shown", Tone.INFO), 0);
		board.queue(Arrays.asList(Notice.player("queued", Tone.INFO)));

		board.clear();
		board.tick(0);

		assertTrue(board.visible(0).isEmpty());
	}

	@Test
	public void aNewQueueAfterTheLastOneFinishedShowsAtOnce()
	{
		NoticeBoard board = new NoticeBoard();
		board.queue(Arrays.asList(Notice.player("old", Tone.INFO)));
		board.tick(0);
		board.tick(600);
		board.tick(1_200);

		board.queue(Arrays.asList(Notice.player("fresh", Tone.INFO)));
		board.tick(1_800);

		assertEquals("fresh", noticeAt(board.visible(1_800), 1).text());
	}

	@Test
	public void aSecondBattleIsHeldBehindTheFirst()
	{
		NoticeBoard board = new NoticeBoard();
		board.enqueue(Arrays.asList(Notice.player("first", Tone.INFO)), true);
		board.enqueue(Arrays.asList(Notice.player("second", Tone.INFO)), false);
		board.tick(0);

		assertTrue(board.holding());
		assertTrue(board.visible(0).isEmpty());

		board.release();
		board.tick(0);
		for (int tick = 1; tick <= NoticeBoard.LINE_TICKS; tick++)
			board.tick(tick * 600L);

		List<NoticeBoard.Shown> lines = board.visible(NoticeBoard.LINE_TICKS * 600L);
		assertFalse(board.holding());
		assertEquals(2, lines.size());
		assertEquals("first", noticeAt(lines, 0).text());
		assertEquals("second", noticeAt(lines, 1).text());
	}

	@Test
	public void linesWithNothingHeldGoStraightToTheQueue()
	{
		NoticeBoard board = new NoticeBoard();
		board.enqueue(Arrays.asList(Notice.player("now", Tone.INFO)), false);
		board.tick(0);

		assertEquals(1, board.visible(0).size());
	}

	@Test
	public void clearDropsHeldLines()
	{
		NoticeBoard board = new NoticeBoard();
		board.enqueue(Arrays.asList(Notice.player("held", Tone.INFO)), true);

		board.clear();
		board.release();
		board.tick(0);

		assertFalse(board.holding());
		assertTrue(board.visible(0).isEmpty());
	}
}
