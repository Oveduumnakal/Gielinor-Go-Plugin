/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the game's news as text floating over heads: over a wild creature for news about that creature, and over
 * the player for things the player found, earned or cannot do. Each line drifts upward and fades out.
 */
final class FloatingTextOverlay extends Overlay
{
	private static final int LINE_HEIGHT = 15;
	private static final int HEAD_OFFSET = 40;
	private static final int RISE_PIXELS = 14;
	private static final double FADE_FROM_AGE = 0.75;

	private final Client client;
	private final GielinorGoPlugin plugin;
	private final NoticeBoard board;

	FloatingTextOverlay(Client client, GielinorGoPlugin plugin, NoticeBoard board)
	{
		this.client = client;
		this.plugin = plugin;
		this.board = board;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long now = System.currentTimeMillis();
		List<NoticeBoard.Shown> lines = board.visible(now);
		if (lines.isEmpty())
			return null;

		graphics.setFont(FontManager.getRunescapeBoldFont());
		Map<Actor, Integer> stacked = new HashMap<>();
		for (int i = lines.size() - 1; i >= 0; i--)
		{
			NoticeBoard.Shown line = lines.get(i);
			Actor head = headFor(line.notice());
			if (head == null)
				continue;

			int row = stacked.merge(head, 1, Integer::sum);
			draw(graphics, head, line, row, now);
		}

		return null;
	}

	/**
	 * Finds whose head a notice floats over.
	 *
	 * @param notice the notice
	 * @return the monster it is about while that monster is in view, otherwise the player; null when logged out
	 */
	private Actor headFor(Notice notice)
	{
		Actor monster = notice.overPlayer() ? null : plugin.npcFor(notice.npcIndex());
		return monster != null ? monster : client.getLocalPlayer();
	}

	/**
	 * Draws one line.
	 *
	 * @param graphics the canvas
	 * @param head the actor it floats over
	 * @param line the line and its age
	 * @param row its place in the stack over that head: 1 is the newest and sits lowest
	 * @param now the current wall-clock time in milliseconds
	 */
	private void draw(Graphics2D graphics, Actor head, NoticeBoard.Shown line, int row, long now)
	{
		Notice notice = line.notice();
		String text = notice.text();
		Point anchor = head.getCanvasTextLocation(graphics, text, head.getLogicalHeight() + HEAD_OFFSET);
		if (anchor == null)
			return;

		double age = line.age(now);
		int alpha = age < FADE_FROM_AGE ? 255 : (int) Math.round(255 * (1.0 - age) / (1.0 - FADE_FROM_AGE));
		int x = anchor.getX();
		int y = anchor.getY() - row * LINE_HEIGHT - (int) Math.round(age * RISE_PIXELS);
		Color color = notice.tone().color();
		graphics.setColor(new Color(0, 0, 0, alpha));
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
		graphics.drawString(text, x, y);
	}
}
