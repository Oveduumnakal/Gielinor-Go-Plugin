/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Draws an outline and a level tag on every wild creature's body, with a small mark beside the name that says whether
 * its species is new, seen or caught.
 */
final class WildOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(WildOverlay.class);
	private static final int OUTLINE_WIDTH = 2;
	private static final int OUTLINE_FEATHER = 4;
	private static final int TAG_HEIGHT_OFFSET = 40;
	private static final int MARK_SIZE = 12;
	private static final int MARK_GAP = 3;
	private static final Color GILDED_COLOR = new Color(0xFFD23F);

	private final Client client;
	private final WildBodies bodies;
	private final GielinorGoConfig config;
	private final ModelOutlineRenderer outlineRenderer;

	WildOverlay(Client client, WildBodies bodies, GielinorGoConfig config, ModelOutlineRenderer outlineRenderer)
	{
		this.client = client;
		this.bodies = bodies;
		this.config = config;
		this.outlineRenderer = outlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (WildBody body : bodies.all())
		{
			try
			{
				draw(graphics, body);
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not draw a wild creature's tag", e);
			}
		}

		return null;
	}

	private void draw(Graphics2D graphics, WildBody body)
	{
		StageAnchors.Spot spot = body.spot();
		if (spot == null || !body.drawn() || body.life().leaving())
			return;

		WildEntry entry = body.entry();
		Color outline = entry.gilded() ? GILDED_COLOR : config.outlineColor();
		outlineRenderer.drawOutline(body.body(), OUTLINE_WIDTH, outline, OUTLINE_FEATHER);
		String tag = entry.prefix() + entry.species().name() + " Lv " + entry.level();
		Point location = Perspective.getCanvasTextLocation(client, graphics, spot.point(), tag,
			spot.height() + TAG_HEIGHT_OFFSET);
		Rarity rarity = entry.species().rarity();
		if (location == null)
			return;

		WildRecord record = entry.record();
		int shift = record == null ? 0 : (MARK_SIZE + MARK_GAP) / 2;
		Point text = new Point(location.getX() + shift, location.getY());
		OverlayUtil.renderTextLocation(graphics, text, tag, entry.gilded() ? GILDED_COLOR : rarity.color());
		if (record != null)
			drawMark(graphics, record, location.getX() - shift, location.getY() - MARK_SIZE + 1);
	}

	private static void drawMark(Graphics2D graphics, WildRecord record, int x, int y)
	{
		Color saved = graphics.getColor();
		graphics.setColor(Color.BLACK);
		Icons.mark(graphics, record.kind(), x + 1, y + 1, MARK_SIZE);
		graphics.setColor(new Color(record.rgb()));
		Icons.mark(graphics, record.kind(), x, y, MARK_SIZE);
		graphics.setColor(saved);
	}
}
