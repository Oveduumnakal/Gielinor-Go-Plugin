/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Paints bubble frames headless: appearing, hovered, greyed, knocked out, a full cluster in a corner, and a pinned
 * card in both themes. It fails on any exception, on a missing or stray pixel, and on graphics left changed. Set the
 * environment variable {@code BUBBLE_PNG_DIR} to also write each canvas as a PNG for a look.
 */
public class WildBubblesRenderSmokeTest
{
	private static final int W = 420;
	private static final int H = 420;
	private final BubblePainter painter = new BubblePainter();

	private static BubbleFrame.Disc disc(BubbleKind kind, double x, double y, double r, double scale, double alpha,
		double hover, JarType jar, int count, boolean disabled, boolean selected, double pulse)
	{
		return new BubbleFrame.Disc(kind, x, y, r, scale, alpha, hover, jar, count, disabled, selected, pulse, true);
	}

	private static List<BubbleFrame.Disc> main(double scale, double alpha, double hover, boolean disabledJar,
		boolean disabledBattle, double pulse, int count)
	{
		List<BubbleFrame.Disc> discs = new ArrayList<>();
		discs.add(disc(BubbleKind.JAR, 150, 150, 15, scale, alpha, hover, disabledJar ? null : JarType.RUNE, count,
			disabledJar, false, pulse));
		discs.add(disc(BubbleKind.BATTLE, 190, 140, 15, scale, alpha, 0, null, 0, disabledBattle, false, 0));
		discs.add(disc(BubbleKind.INFO, 230, 150, 15, scale, alpha, 0, null, 0, false, false, 0));
		return discs;
	}

	private static WildCardContent content(boolean knockedOut, boolean record)
	{
		Species species = new Species("Imp", 5, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER, Trait.MAGIC)
			.withFactors(1.25, 0.75, 1.0, 1.12);
		WildEntry entry = new WildEntry(3, species, 14, species.level(), false, false);
		if (record)
			entry.record(WildRecord.of(2, false));

		JarSuggest.JarChoice shown = new JarSuggest.JarChoice(JarType.RUNE, 2, 0.456, false);
		return WildCardContent.of(entry, shown, knockedOut);
	}

	private BufferedImage render(String name, BubbleFrame frame, WindowStyle style)
	{
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = WidgetKit.graphics(image);
		painter.paint(g, frame, style, 1000L);
		g.dispose();
		String dir = System.getenv("BUBBLE_PNG_DIR");
		if (dir != null)
		{
			try
			{
				ImageIO.write(image, "png", new File(dir, name + ".png"));
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
		}

		return image;
	}

	private static int alphaAt(BufferedImage image, double x, double y)
	{
		return image.getRGB((int) x, (int) y) >>> 24;
	}

	private static void assertDiscsPainted(BubbleFrame frame, BufferedImage image)
	{
		for (BubbleFrame.Disc d : frame.discs())
			assertTrue("centre of " + d.kind(), alphaAt(image, d.x(), d.y()) > 0);
	}

	private static void assertFar(BufferedImage image)
	{
		assertEquals(0, alphaAt(image, 2, 2));
		assertEquals(0, alphaAt(image, W - 3, H - 3));
		assertEquals(0, alphaAt(image, 2, H - 3));
	}

	@Test
	public void fullMidAppearPaintsSmallFadedDiscs()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame half = new BubbleFrame(main(0.5, 0.5, 0, false, false, 0, 0), null, 1, false);
		BubbleFrame whole = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0, 0), null, 1, false);

		BufferedImage small = render("appear-half", half, style);
		BufferedImage big = render("appear-whole", whole, style);

		assertDiscsPainted(half, small);
		assertFar(small);
		assertTrue(WidgetKit.painted(small, 0, 0, W, H) < WidgetKit.painted(big, 0, 0, W, H));
		assertTrue(alphaAt(small, 150, 150) < alphaAt(big, 150, 150));
	}

	@Test
	public void openWithHoverDrawsTheRingAndTheBadge()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame plain = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0, 120), null, 1, false);
		BubbleFrame hovered = new BubbleFrame(main(1.0, 1.0, 1.0, false, false, 0, 120), null, 1, false);

		BufferedImage a = render("open-plain", plain, style);
		BufferedImage b = render("open-hover", hovered, style);

		assertDiscsPainted(hovered, b);
		assertFar(b);
		assertTrue(WidgetKit.painted(b, 120, 120, 60, 60) > WidgetKit.painted(a, 120, 120, 60, 60));
		assertTrue("badge pixel", alphaAt(a, 150 + 10, 150 + 10) > 0);
	}

	@Test
	public void disabledDiscsAreFainterThanLiveOnes()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame live = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0, 0), null, 1, false);
		BubbleFrame grey = new BubbleFrame(main(1.0, 1.0, 0, true, true, 0, 0), null, 1, false);

		BufferedImage a = render("live", live, style);
		BufferedImage b = render("disabled", grey, style);

		assertDiscsPainted(grey, b);
		assertFar(b);
		assertTrue(alphaAt(b, 190, 150) < alphaAt(a, 190, 150));
	}

	@Test
	public void aKnockedOutPulseDrawsARingOutsideTheDisc()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame still = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0, 0), null, 1, false);
		BubbleFrame pulsing = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0.3, 0), null, 1, false);

		BufferedImage a = render("still", still, style);
		BufferedImage b = render("pulse", pulsing, style);

		assertDiscsPainted(pulsing, b);
		assertTrue(WidgetKit.painted(b, 120, 120, 60, 60) > WidgetKit.painted(a, 120, 120, 60, 60));
	}

	@Test
	public void aFullClusterNearACornerPaintsEveryDisc()
	{
		WindowStyle style = WidgetKit.style(1.0);
		List<BubbleFrame.Disc> discs = new ArrayList<>();
		JarType[] jars = JarType.values();
		for (int i = 0; i < jars.length; i++)
			discs.add(disc(BubbleKind.CLUSTER_JAR, 20 + (i % 6) * 28, 20 + (i / 6) * 28, 12, 1.0, 1.0,
				i == 4 ? 1.0 : 0, jars[i], i % 3 == 0 ? 120 : i, false, i == 2, 0));

		BubbleFrame frame = new BubbleFrame(discs, null, 1, true);

		BufferedImage image = render("cluster", frame, style);

		assertDiscsPainted(frame, image);
		assertEquals(0, alphaAt(image, W - 3, H - 3));
	}

	@Test
	public void aPinnedCardPaintsInBothThemesAtTwoScales()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			for (double scale : new double[] {1.0, 1.5})
			{
				WindowStyle style = WindowStyle.of(theme, scale);
				WildCardContent content = content(true, true);
				BufferedImage probe = WidgetKit.canvas(1, 1);
				Graphics2D pg = WidgetKit.graphics(probe);
				Dimension size = painter.cardSize(pg, content, style);
				pg.dispose();
				Rectangle bounds = new Rectangle(40, 20, size.width, size.height);
				BubbleFrame.Card card = new BubbleFrame.Card(bounds, content, true, 1.0, 1.0,
					BubbleFrame.Side.RIGHT, true);
				BubbleFrame frame = new BubbleFrame(Collections.emptyList(), card, 1, true);
				String name = "card-" + theme.name() + "-" + scale;

				BufferedImage image = render(name, frame, style);

				assertTrue(name, size.width >= style.px(BubblePainter.CARD_UNITS));
				assertTrue(name, alphaAt(image, bounds.x + 4, bounds.y + 4) > 0);
				assertEquals(name, 0, alphaAt(image, 2, 2));
				assertEquals(name, 0, WidgetKit.painted(image, 0, 0, bounds.x - 4, H));
			}
		}
	}

	@Test
	public void theCardGrowsFromTheEdgeFacingTheBubbles()
	{
		WindowStyle style = WidgetKit.style(1.0);
		WildCardContent content = content(false, false);
		Rectangle bounds = new Rectangle(100, 60, 190, 170);
		BubbleFrame left = new BubbleFrame(Collections.emptyList(),
			new BubbleFrame.Card(bounds, content, false, 0.85, 1.0, BubbleFrame.Side.LEFT, false), 1, false);
		BubbleFrame right = new BubbleFrame(Collections.emptyList(),
			new BubbleFrame.Card(bounds, content, false, 0.85, 1.0, BubbleFrame.Side.RIGHT, false), 1, false);

		BufferedImage a = render("grow-left", left, style);
		BufferedImage b = render("grow-right", right, style);

		assertTrue(alphaAt(a, 289, 140) > 0);
		assertEquals(0, alphaAt(b, 289, 140));
		assertTrue(alphaAt(b, 101, 140) > 0);
		assertEquals(0, alphaAt(a, 101, 140));
	}

	@Test
	public void aCardWithNoContentPaintsNothingAndGetsADefaultSize()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame.Card card = new BubbleFrame.Card(new Rectangle(10, 10, 100, 50), null, false, 1.0, 1.0,
			BubbleFrame.Side.ABOVE, false);
		BubbleFrame frame = new BubbleFrame(Collections.emptyList(), card, 1, false);

		BufferedImage image = render("nocontent", frame, style);
		Graphics2D g = WidgetKit.graphics(WidgetKit.canvas(1, 1));
		Dimension size = painter.cardSize(g, null, style);
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, W, H));
		assertTrue(size.width > 0 && size.height > 0);
	}

	@Test
	public void reducedAndOffMotionAlphasPaintAtTheirOpacity()
	{
		WindowStyle style = WidgetKit.style(1.0);
		BubbleFrame faint = new BubbleFrame(main(1.0, 0.4, 0, false, false, 0, 0), null, 1, false);
		BubbleFrame solid = new BubbleFrame(main(1.0, 1.0, 0, false, false, 0, 0), null, 1, false);
		BubbleFrame over = new BubbleFrame(main(1.04, 1.7, 0, false, false, 0, 0), null, 1, false);

		BufferedImage a = render("reduced", faint, style);
		BufferedImage b = render("off", solid, style);
		BufferedImage c = render("overshoot", over, style);

		assertTrue(alphaAt(a, 190, 140) < alphaAt(b, 190, 140));
		assertEquals(255, alphaAt(c, 190, 140));
		assertDiscsPainted(over, c);
	}

	@Test
	public void theGraphicsAreLeftAsFound()
	{
		WindowStyle style = WidgetKit.style(1.5);
		WildCardContent content = content(true, true);
		Rectangle bounds = new Rectangle(200, 40, 190, 180);
		BubbleFrame.Card card = new BubbleFrame.Card(bounds, content, true, 0.9, 0.8, BubbleFrame.Side.ABOVE, true);
		List<BubbleFrame.Disc> discs = main(1.0, 1.0, 0.5, false, false, 0.5, 5);
		BubbleFrame frame = new BubbleFrame(discs, card, 1, true);
		BufferedImage image = WidgetKit.canvas(W, H);
		Graphics2D g = WidgetKit.graphics(image);
		AffineTransform transform = AffineTransform.getTranslateInstance(3, 4);
		g.setTransform(transform);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
		g.setClip(new Rectangle(0, 0, W, H));
		g.setFont(style.font(WindowFonts.Face.BOLD, 24));
		g.setColor(new Color(10, 20, 30));
		Stroke stroke = new BasicStroke(3f);
		g.setStroke(stroke);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		painter.paint(g, frame, style, 5L);

		assertEquals(transform, g.getTransform());
		assertEquals(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f), g.getComposite());
		assertEquals(new Rectangle(0, 0, W, H), g.getClip().getBounds());
		assertEquals(style.font(WindowFonts.Face.BOLD, 24), g.getFont());
		assertEquals(new Color(10, 20, 30), g.getColor());
		assertEquals(stroke, g.getStroke());
		assertEquals(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
			g.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
		assertNotNull(g.getRenderingHints());
		g.dispose();
	}
}
