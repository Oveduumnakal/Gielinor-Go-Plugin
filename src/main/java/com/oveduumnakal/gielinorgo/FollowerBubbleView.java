/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Puts the follower bubbles on the game screen and takes the clicks on them. It paints on the game thread and
 * publishes where the bubbles are for the mouse listener on the UI thread; only a press on a bubble is taken, and
 * with RuneLite's drag key held even that passes through.
 */
final class FollowerBubbleView extends Overlay implements MouseListener
{
	private static final Logger log = LoggerFactory.getLogger(FollowerBubbleView.class);

	private final Client client;
	private final FollowerBubbles model;
	private final FollowerBubblePainter painter;
	private final Supplier<WindowStyle> style;
	private final BooleanSupplier dragHeld;
	private final Consumer<int[]> pressed;
	private final Runnable broke;
	private final Tooltip tooltip = new Tooltip();
	private volatile List<FollowerBubbles.Dot> shown = Collections.emptyList();
	private boolean taken;

	/**
	 * Creates the view.
	 *
	 * @param client the game client
	 * @param model the bubbles
	 * @param painter paints them
	 * @param style the window's look now
	 * @param dragHeld whether RuneLite's drag key is down for a mouse event
	 * @param pressed hears a taken press as x, y and button, on the UI thread
	 * @param broke told once when painting threw
	 */
	FollowerBubbleView(Client client, FollowerBubbles model, FollowerBubblePainter painter, Supplier<WindowStyle> style,
		BooleanSupplier dragHeld, Consumer<int[]> pressed, Runnable broke)
	{
		this.client = client;
		this.model = model;
		this.painter = painter;
		this.style = style;
		this.dragHeld = dragHeld;
		this.pressed = pressed;
		this.broke = broke;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/**
	 * @param x across
	 * @param y down
	 * @return whether a painted bubble is there; safe from any thread
	 */
	boolean covers(double x, double y)
	{
		return shown.stream().anyMatch(dot -> dot.contains(x, y));
	}

	/** Forgets what was painted, so nothing is clickable. */
	void clear()
	{
		shown = Collections.emptyList();
		tooltip.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			List<FollowerBubbles.Dot> dots = model.dots();
			shown = new ArrayList<>(dots);
			if (dots.isEmpty())
			{
				tooltip.clear();
				return null;
			}

			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			FollowerBubbles.Dot under = mouse == null ? null : model.at(mouse.getX(), mouse.getY());
			WindowStyle look = style.get();
			painter.paint(graphics, dots, under, look);
			if (under == null)
			{
				tooltip.clear();
				return null;
			}

			long now = System.currentTimeMillis();
			tooltip.hover(under.kind() + ":" + under.x(), model.tip(under), mouse.getX(), mouse.getY(), now);
			Graphics2D copy = (Graphics2D) graphics.create();
			WindowStyle.prepare(copy);
			tooltip.paint(copy, look, new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()), now);
			copy.dispose();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the follower bubbles", e);
			clear();
			broke.run();
		}

		return null;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		taken = false;
		int button = event.getButton();
		if (dragHeld.getAsBoolean() || button != MouseEvent.BUTTON1 && button != MouseEvent.BUTTON3
			|| !covers(event.getX(), event.getY()))
			return event;

		taken = true;
		event.consume();
		pressed.accept(new int[] {event.getX(), event.getY(), button});
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (taken)
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (taken)
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (taken)
			event.consume();

		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}
}
