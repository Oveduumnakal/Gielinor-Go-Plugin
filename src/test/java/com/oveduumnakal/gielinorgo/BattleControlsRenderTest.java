/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Paints the battle controls offline: every page, both themes, several scales, with no text wider than its box. */
public class BattleControlsRenderTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private static BattleView view()
	{
		Combatant mine = new Combatant(GOBLIN, "Archibald the Magnificent of the Northern Reaches", 42, 0.6)
			.moves(Arrays.asList(TestMoves.BITE, TestMoves.DRAGONFIRE));
		Combatant foe = new Combatant(GOBLIN, "Lesser demon with a very long name indeed", 17, 0.25);
		List<BattleView.Option> options = Arrays.asList(new BattleView.Option(1, "Imp", 9, 1.0),
			new BattleView.Option(3, "A creature with a rather long nickname", 4, 0.5));
		List<BattleView.ItemOption> items = Arrays.asList(new BattleView.ItemOption(ItemType.SMALL_SALVE, 3, options));
		return new BattleView(mine, foe, false, true, JarType.BRONZE, 12, 10, 0.7, options, items);
	}

	@Test
	public void everyPageDrawsInsideItsBox()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (double scale : new double[] {WindowFit.MIN_SCALE, 1.0, 2.0})
				for (String open : new String[] {"", "attack", "switch", "item"})
				{
					List<String> drawn = new ArrayList<>();
					BattleControlsModel model = new BattleControlsModel(choice ->
					{
					});
					model.show(view(), new BattleControlsModel.Keys("F1", "F2", "F3", "F5", "F4"));
					WindowStyle style = WindowStyle.of(theme, scale);
					BattleControlsModel.Layout layout = model.layout(style);
					if (!open.isEmpty())
					{
						Rectangle b = model.hits().stream().filter(h -> h.id().equals(open)).findFirst().get().bounds();
						model.onPressed(b.x + 2, b.y + 2, MouseEvent.BUTTON1);
						model.onReleased(b.x + 2, b.y + 2, MouseEvent.BUTTON1);
						layout = model.layout(style);
					}

					BufferedImage image = new BufferedImage(layout.size.width, layout.size.height,
						BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = image.createGraphics();
					WindowStyle.prepare(g);
					new BattleControlsPainter((text, width, height, box) ->
					{
						if (width > box.width)
							drawn.add(text + " is " + width + " in " + box.width);
					}).paint(g, style, model, layout);
					g.dispose();
					for (String line : drawn)
						problems.append(theme).append(' ').append(scale).append(' ').append(open).append(": ")
							.append(line).append('\n');

					int middle = image.getRGB(layout.size.width / 2, layout.size.height / 2);
					assertTrue("something was painted", middle != 0);
				}

		assertEquals(problems.toString(), "", problems.toString());
	}
}
