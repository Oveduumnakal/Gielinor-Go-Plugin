/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
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
	private static final Color GILDED_COLOR = new Color(0xFFD23F);

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

			Color outline = entry.gilded() ? GILDED_COLOR : config.outlineColor();
			outlineRenderer.drawOutline(npc, OUTLINE_WIDTH, outline, OUTLINE_FEATHER);
			String tag = (entry.gilded() ? "Gilded Lv " : "Wild Lv ") + entry.level();
			Point location = npc.getCanvasTextLocation(graphics, tag, npc.getLogicalHeight() + TAG_HEIGHT_OFFSET);
			Rarity rarity = entry.species().rarity();
			if (location != null)
				OverlayUtil.renderTextLocation(graphics, location, tag, entry.gilded() ? GILDED_COLOR : rarity.color());
		}

		return null;
	}
}
