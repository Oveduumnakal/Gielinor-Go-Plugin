/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A small box on the game screen while a battle waits for a choice: whose turn it is, how long is left, both health
 * values and the hotkeys that are set, so the side panel need not be open to follow the fight. What it says is worked
 * out by {@link BattlePrompt}; the text is handed in from the game thread and read on the render thread.
 */
final class BattlePromptOverlay extends OverlayPanel
{
	private static final Logger log = LoggerFactory.getLogger(BattlePromptOverlay.class);

	private volatile List<String> text = List.of();

	/** Creates the overlay, showing nothing. */
	BattlePromptOverlay()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/**
	 * Sets what the box says.
	 *
	 * @param lines the lines from {@link BattlePrompt#lines}, or an empty list to show nothing
	 */
	void show(List<String> lines)
	{
		text = List.copyOf(lines);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			List<String> lines = text;
			if (lines.isEmpty())
				return null;

			List<LayoutableRenderableEntity> children = panelComponent.getChildren();
			children.add(TitleComponent.builder()
				.text(lines.get(0))
				.build());
			for (String line : lines.subList(1, lines.size()))
				children.add(LineComponent.builder()
					.left(line)
					.build());

			return super.render(graphics);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the battle prompt", e);
			return null;
		}
	}
}
