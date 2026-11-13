/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the game's news as text floating over bodies: over a wild creature's body for news about that creature, and
 * over the player's head for things the player found, earned or cannot do. While a battle is acted out on screen,
 * battle lines float over the fighters' bodies. Each line drifts upward and fades out.
 */
final class FloatingTextOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(FloatingTextOverlay.class);
	private static final int LINE_HEIGHT = 15;
	private static final int HEAD_OFFSET = 40;
	private static final int RISE_PIXELS = 14;
	private static final double FADE_FROM_AGE = 0.75;

	private final Client client;
	private final NoticeBoard board;
	private static final Notice LEAD_BODY = Notice.stageLead("", Tone.INFO);
	private static final Notice FOE_BODY = Notice.stageFoe("", Tone.INFO);

	private final Function<Notice, StageAnchors.Spot> bodies;
	private final Function<Notice.Anchor, Status> tags;

	/**
	 * Creates the overlay.
	 *
	 * @param client the game client
	 * @param board the lines on screen
	 * @param bodies finds where the body a line is about is drawn, or null to float over the player's head
	 * @param tags finds the status a body on a battle stage shows as a tag, or null for none
	 */
	FloatingTextOverlay(Client client, NoticeBoard board, Function<Notice, StageAnchors.Spot> bodies,
		Function<Notice.Anchor, Status> tags)
	{
		this.client = client;
		this.board = board;
		this.bodies = bodies;
		this.tags = tags;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			drawAll(graphics);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the floating text", e);
		}

		return null;
	}

	private void drawAll(Graphics2D graphics)
	{
		long now = System.currentTimeMillis();
		graphics.setFont(FontManager.getRunescapeBoldFont());
		drawTag(graphics, LEAD_BODY, Notice.Anchor.STAGE_LEAD);
		drawTag(graphics, FOE_BODY, Notice.Anchor.STAGE_FOE);
		List<NoticeBoard.Shown> lines = board.visible(now);
		if (lines.isEmpty())
			return;

		Map<Object, Integer> stacked = new HashMap<>();
		for (int i = lines.size() - 1; i >= 0; i--)
		{
			NoticeBoard.Shown line = lines.get(i);
			Notice notice = line.notice();
			StageAnchors.Spot spot = spotFor(notice);
			Actor head = spot == null ? client.getLocalPlayer() : null;
			if (spot == null && head == null)
				continue;

			Object place = spot == null ? head : Arrays.asList(notice.anchor(), notice.wildId());
			int row = stacked.merge(place, 1, Integer::sum);
			Point anchor = spot == null ? head.getCanvasTextLocation(graphics, notice.text(),
				head.getLogicalHeight() + HEAD_OFFSET) : stageLocation(graphics, spot, notice.text());
			draw(graphics, anchor, line, row, now);
		}
	}

	/**
	 * Draws the tag of a status, such as PSN, over a body on a battle stage for as long as the status lasts. The
	 * tag sits just over the body, under the floating lines.
	 *
	 * @param graphics the canvas
	 * @param body a notice that stands for the body, to find where it is drawn
	 * @param anchor which body
	 */
	private void drawTag(Graphics2D graphics, Notice body, Notice.Anchor anchor)
	{
		Status status = tags == null ? null : tags.apply(anchor);
		StageAnchors.Spot spot = status == null ? null : spotFor(body);
		if (spot == null)
			return;

		String text = status.tag();
		Point at = stageLocation(graphics, spot, text);
		if (at == null)
			return;

		graphics.setColor(Color.BLACK);
		graphics.drawString(text, at.getX() + 1, at.getY() + 1);
		graphics.setColor(status.color());
		graphics.drawString(text, at.getX(), at.getY());
	}

	/**
	 * Finds the place of the body a notice is about, if it has one.
	 *
	 * @param notice the notice
	 * @return where its body is drawn, or null to float over the player's head
	 */
	private StageAnchors.Spot spotFor(Notice notice)
	{
		if (notice.anchor() == Notice.Anchor.HEAD)
			return null;

		try
		{
			StageAnchors.Spot spot = bodies.apply(notice);
			WorldView view = client.getTopLevelWorldView();
			return spot != null && view != null && spot.plane() == view.getPlane() ? spot : null;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not find the body a line is about", e);
			return null;
		}
	}

	/**
	 * Finds where a line is drawn over a body on the stage.
	 *
	 * @param graphics the canvas
	 * @param spot where the body stands
	 * @param text the line
	 * @return the canvas place, or null when it is off screen
	 */
	private Point stageLocation(Graphics2D graphics, StageAnchors.Spot spot, String text)
	{
		LocalPoint ground = spot.point();
		return Perspective.getCanvasTextLocation(client, graphics, ground, text, spot.height() + HEAD_OFFSET);
	}

	/**
	 * Draws one line.
	 *
	 * @param graphics the canvas
	 * @param anchor where the text is centred over its head, or null when it is off screen
	 * @param line the line and its age
	 * @param row its place in the stack over that head: 1 is the newest and sits lowest
	 * @param now the current wall-clock time in milliseconds
	 */
	private void draw(Graphics2D graphics, Point anchor, NoticeBoard.Shown line, int row, long now)
	{
		Notice notice = line.notice();
		String text = notice.text();
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
		drawJar(graphics, notice.jar(), x, y, alpha);
		drawItem(graphics, notice.item(), x, y, alpha);
	}

	/**
	 * Draws a jar's picture just left of a line's text, fading with it.
	 *
	 * @param graphics the canvas
	 * @param jar the jar the line is about, or null
	 * @param x the left edge of the text
	 * @param y the baseline of the text
	 * @param alpha the line's opacity, from 0 to 255
	 */
	private static void drawJar(Graphics2D graphics, JarType jar, int x, int y, int alpha)
	{
		BufferedImage image = JarIcons.image(jar);
		if (image == null)
			return;

		Composite before = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
		graphics.drawImage(image, x - image.getWidth() - 2, y - image.getHeight() + 3, null);
		graphics.setComposite(before);
	}

	/**
	 * Draws a healing item's picture just left of a line's text, fading with it.
	 *
	 * @param graphics the canvas
	 * @param item the item the line is about, or null
	 * @param x the left edge of the text
	 * @param y the baseline of the text
	 * @param alpha the line's opacity, from 0 to 255
	 */
	private static void drawItem(Graphics2D graphics, ItemType item, int x, int y, int alpha)
	{
		BufferedImage image = ItemIcons.image(item);
		if (image == null)
			return;

		Composite before = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
		graphics.drawImage(image, x - image.getWidth() - 2, y - image.getHeight() + 3, null);
		graphics.setComposite(before);
	}
}
