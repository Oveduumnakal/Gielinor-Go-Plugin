/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
	public void queuedLinesComeOutOnePerTick()
	{
		NoticeBoard board = new NoticeBoard();
		board.queue(Arrays.asList(
			Notice.creature(3, "first", Tone.INFO),
			Notice.player("second", Tone.BAD)));

		assertTrue(board.visible(0).isEmpty());

		board.tick(0);
		assertEquals(1, board.visible(0).size());

		board.tick(600);
		board.tick(1_200);
		assertEquals(2, board.visible(1_200).size());
		assertTrue(noticeAt(board.visible(1_200), 1).overPlayer());
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
}
