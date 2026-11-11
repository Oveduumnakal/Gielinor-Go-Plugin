/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;

import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/** Draws an outline and a level tag on every monster that is currently wild. */
final class WildOverlay extends Overlay
{
	private static final int OUTLINE_WIDTH = 2;
	private static final int OUTLINE_FEATHER = 4;
	private static final int TAG_HEIGHT_OFFSET = 40;

	private final GielinorGoPlugin plugin;
	private final GielinorGoConfig config;
	private final ModelOutlineRenderer outlineRenderer;

	WildOverlay(GielinorGoPlugin plugin, GielinorGoConfig config, ModelOutlineRenderer outlineRenderer)
	{
		this.plugin = plugin;
		this.config = config;
		this.outlineRenderer = outlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (WildEntry entry : plugin.wildCreatures())
		{
			NPC npc = plugin.npcFor(entry.npcIndex());
			if (npc == null)
				continue;

			outlineRenderer.drawOutline(npc, OUTLINE_WIDTH, config.outlineColor(), OUTLINE_FEATHER);
			String tag = "Wild Lv " + entry.level();
			Point location = npc.getCanvasTextLocation(graphics, tag, npc.getLogicalHeight() + TAG_HEIGHT_OFFSET);
			Rarity rarity = entry.species().rarity();
			if (location != null)
				OverlayUtil.renderTextLocation(graphics, location, tag, rarity.color());
		}

		return null;
	}
}
