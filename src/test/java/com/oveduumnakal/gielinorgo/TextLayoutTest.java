/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the text layout: wrapping by measured width, the ellipsis, the markup and link hit areas. */
public class TextLayoutTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final FontMetrics REGULAR = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
	private static final FontMetrics BOLD = WidgetKit.metrics(STYLE, WindowFonts.Face.BOLD);
	private static final String SENTENCE = "The quick brown fox jumps over the lazy dog and keeps on running far away";

	private static TextLayout markup(String text, int width)
	{
		return TextLayout.markup(text, REGULAR, BOLD, width, 0);
	}

	@Test
	public void everyWrappedLineFitsTheWidthAndNoWordIsLost()
	{
		int width = REGULAR.stringWidth("The quick brown fox");
		TextLayout layout = TextLayout.plain(SENTENCE, REGULAR, width, 0);

		assertTrue(layout.lineCount() > 3);
		StringBuilder joined = new StringBuilder();
		for (int i = 0; i < layout.lineCount(); i++)
		{
			assertTrue("line " + i, REGULAR.stringWidth(layout.lineText(i)) <= width);
			joined.append(i == 0 ? "" : " ").append(layout.lineText(i));
		}

		assertEquals(SENTENCE, joined.toString());
	}

	@Test
	public void aWiderLayoutNeedsFewerLinesAndTextThatFitsIsOneLine()
	{
		int narrow = TextLayout.plain(SENTENCE, REGULAR, 120, 0).lineCount();
		int wide = TextLayout.plain(SENTENCE, REGULAR, 300, 0).lineCount();
		TextLayout fits = TextLayout.plain("Short", REGULAR, 300, 0);

		assertTrue(narrow > wide);
		assertEquals(1, fits.lineCount());
		assertEquals("Short", fits.lineText(0));
		assertFalse(fits.truncated());
	}

	@Test
	public void plainTextKeepsItsLineBreaksAndReadsNoMarkup()
	{
		TextLayout layout = TextLayout.plain("One\nTwo **not bold**\n- not a bullet", REGULAR, 400, 0);

		assertEquals(3, layout.lineCount());
		assertEquals("Two **not bold**", layout.lineText(1));
		assertEquals("- not a bullet", layout.lineText(2));
		assertFalse(layout.isBullet(2));
		assertFalse(layout.hasBold(1));
	}

	@Test
	public void aWordWiderThanTheLineIsBrokenAcrossLines()
	{
		int width = REGULAR.stringWidth("Antidisest");
		TextLayout layout = TextLayout.plain("Antidisestablishmentarianism", REGULAR, width, 0);

		assertTrue(layout.lineCount() > 1);
		StringBuilder joined = new StringBuilder();
		for (int i = 0; i < layout.lineCount(); i++)
		{
			assertTrue(REGULAR.stringWidth(layout.lineText(i)) <= width);
			joined.append(layout.lineText(i));
		}

		assertEquals("Antidisestablishmentarianism", joined.toString());
	}

	@Test
	public void aTinyWidthStillEndsAndPlacesAtLeastOneCharacterPerLine()
	{
		TextLayout layout = TextLayout.plain("abc def", REGULAR, 1, 0);

		assertEquals(6, layout.lineCount());
	}

	@Test
	public void emptyTextHasNoLinesAndNoHeight()
	{
		TextLayout layout = TextLayout.plain("", REGULAR, 200, 0);

		assertEquals(0, layout.lineCount());
		assertEquals(0, layout.height());
	}

	@Test
	public void everyLineHasTheSameHeightSoLinesCanBeScrollRows()
	{
		TextLayout layout = markup("# Title\n\nSome text that wraps around a few times over here\n- bullet", 150);

		assertTrue(layout.lineHeight() > 0);
		assertEquals(layout.lineCount() * layout.lineHeight(), layout.height());
	}

	@Test
	public void ellipsisKeepsTextThatFitsAndCutsTextThatDoesNot()
	{
		int width = REGULAR.stringWidth("Giant rat");

		assertEquals("Giant rat", TextLayout.ellipsis("Giant rat", REGULAR, width));
		String cut = TextLayout.ellipsis("Giant rat with a very long name", REGULAR, width);
		assertTrue(cut.endsWith("..."));
		assertTrue(REGULAR.stringWidth(cut) <= width);
		assertEquals("", TextLayout.ellipsis("Giant rat", REGULAR, 2));
	}

	@Test
	public void aLineLimitEndsTheLastLineWithAnEllipsisThatStillFits()
	{
		int width = 140;
		TextLayout full = TextLayout.plain(SENTENCE, REGULAR, width, 0);
		TextLayout limited = TextLayout.plain(SENTENCE, REGULAR, width, 2);

		assertTrue(full.lineCount() > 2);
		assertFalse(full.truncated());
		assertTrue(limited.truncated());
		assertEquals(2, limited.lineCount());
		assertEquals(full.lineText(0), limited.lineText(0));
		assertTrue(limited.lineText(1).endsWith("..."));
		assertTrue(REGULAR.stringWidth(limited.lineText(1)) <= width);
	}

	@Test
	public void aLineLimitThatIsNotReachedChangesNothing()
	{
		TextLayout layout = TextLayout.plain("Two words", REGULAR, 300, 3);

		assertFalse(layout.truncated());
		assertEquals("Two words", layout.lineText(0));
	}

	@Test
	public void headingsAreBoldAndBulletsAreMarked()
	{
		TextLayout layout = markup("## Fixed\n- one\n- two\n\nplain", 400);

		assertEquals(5, layout.lineCount());
		assertTrue(layout.isHeading(0));
		assertTrue(layout.hasBold(0));
		assertEquals("Fixed", layout.lineText(0));
		assertTrue(layout.isBullet(1));
		assertTrue(layout.isBullet(2));
		assertEquals("two", layout.lineText(2));
		assertFalse(layout.isBullet(3));
		assertEquals("plain", layout.lineText(4));
	}

	@Test
	public void aBulletTakesTheFollowingPlainLinesAndWrapsWithAHangingIndent()
	{
		TextLayout layout = markup("- The first part\n  and the second part of one bullet that is long\n- next", 160);

		assertTrue(layout.isBullet(0));
		assertFalse("continuation lines are not bullets", layout.isBullet(1));
		assertTrue(layout.lineCount() >= 3);
		assertTrue(layout.lineText(0).startsWith("The first"));
		assertEquals("The first part and the second part of one bullet that is long",
			layout.lineText(0) + " " + layout.lineText(1) + " " + layout.lineText(2));
		assertTrue(layout.isBullet(layout.lineCount() - 1));
		assertEquals("next", layout.lineText(layout.lineCount() - 1));
		for (int i = 0; i < layout.lineCount(); i++)
			assertTrue("line " + i, layout.lineWidth(i) <= 160);
	}

	@Test
	public void doubleAsterisksMakeBoldAndTheMarkersAreNotShown()
	{
		TextLayout layout = markup("A **bold move**, then plain", 400);

		assertEquals(1, layout.lineCount());
		assertTrue(layout.hasBold(0));
		assertEquals("A bold move, then plain", layout.lineText(0));
	}

	@Test
	public void plainTextKeepsEveryBlankLine()
	{
		TextLayout layout = TextLayout.plain("\nLine one\n\n\nLine two\n\n", REGULAR, 400, 0);

		assertEquals(5, layout.lineCount());
	}

	@Test
	public void aLinkMayHaveBracketsInItsAddress()
	{
		TextLayout layout = markup("See [Fire rune](https://oldschool.runescape.wiki/w/Fire_(rune)) now", 400);

		List<TextLayout.Link> links = layout.links(0, 0);
		assertEquals(1, links.size());
		assertEquals("https://oldschool.runescape.wiki/w/Fire_(rune)", links.get(0).url);
		assertEquals("See Fire rune now", layout.lineText(0));
	}

	@Test
	public void blankLinesBecomeOneEmptyLineAndEdgesAreTrimmed()
	{
		TextLayout layout = markup("\n\nFirst\n\n\n\nSecond\n\n", 400);

		assertEquals(3, layout.lineCount());
		assertEquals("", layout.lineText(1));
		assertEquals(0, layout.lineWidth(1));
	}

	@Test
	public void aLinkReportsItsUrlAndItsHitAreaMatchesItsText()
	{
		TextLayout layout = markup("See [the wiki page](https://oldschool.runescape.wiki/w/Cow) now", 400);

		List<TextLayout.Link> links = layout.links(10, 20);
		TextLayout.Link link = links.get(0);
		assertEquals(1, links.size());
		assertEquals("https://oldschool.runescape.wiki/w/Cow", link.url);
		assertEquals("See the wiki page now", layout.lineText(0));
		assertEquals(10 + REGULAR.stringWidth("See") + REGULAR.charWidth(' '), link.box.x);
		assertEquals(REGULAR.stringWidth("the wiki page"), link.box.width);
		assertEquals(20, link.box.y);
		assertEquals(layout.lineHeight(), link.box.height);
		assertEquals(link.url, layout.linkAt(link.box.x + 3, 25, 10, 20));
		assertNull("before the link", layout.linkAt(11, 25, 10, 20));
		assertNull("after the link", layout.linkAt(link.box.x + link.box.width + 8, 25, 10, 20));
		assertNull("below the text", layout.linkAt(link.box.x + 3, 20 + layout.height() + 5, 10, 20));
		assertNull("above the text", layout.linkAt(link.box.x + 3, 5, 10, 20));
	}

	@Test
	public void aLinkThatWrapsHasOneHitAreaPerLine()
	{
		TextLayout layout = markup("[one two three four five six](https://example.org/x)",
			REGULAR.stringWidth("one two three"));

		List<TextLayout.Link> links = layout.links(0, 0);
		assertTrue(links.size() >= 2);
		for (TextLayout.Link link : links)
			assertEquals("https://example.org/x", link.url);

		assertNotNull(layout.linkAt(2, layout.lineHeight() + 2, 0, 0));
	}

	@Test
	public void twoLinksOnOneLineAreSeparateHitAreas()
	{
		TextLayout layout = markup("[a](https://a.example) and [b](https://b.example)", 400);

		List<TextLayout.Link> links = layout.links(0, 0);
		assertEquals(2, links.size());
		assertEquals("https://a.example", links.get(0).url);
		assertEquals("https://b.example", links.get(1).url);
		assertTrue(links.get(0).box.x + links.get(0).box.width < links.get(1).box.x);
	}

	@Test
	public void aBoldLinkAndAPunctuationNeighbourKeepTheirSpacing()
	{
		TextLayout layout = markup("**[Bold link](https://x.example)**, then", 400);

		assertEquals("Bold link, then", layout.lineText(0));
		assertTrue(layout.hasBold(0));
		assertEquals(1, layout.links(0, 0).size());
	}

	@Test
	public void aBracketThatIsNotALinkIsPlainText()
	{
		TextLayout layout = markup("An array [1] and (2)", 400);

		assertEquals("An array [1] and (2)", layout.lineText(0));
		assertTrue(layout.links(0, 0).isEmpty());
	}

	@Test
	public void theChangeLogSampleLaysOutWithoutLosingText()
	{
		String log = "### Fixed\n- Catching works **again** when a battle ends early, see\n"
			+ "  [the issue](https://x.example/1).\n\n### Added\n- Nicknames.";
		TextLayout layout = markup(log, 220);

		assertTrue(layout.isHeading(0));
		assertEquals("Fixed", layout.lineText(0));
		assertEquals(1, layout.links(0, 0).size());
		assertEquals("Nicknames.", layout.lineText(layout.lineCount() - 1));
	}

	@Test
	public void paintDrawsOnlyTheLinesInsideTheClip()
	{
		TextLayout layout = markup("First line\nSecond line\nThird line", 300);
		int lh = layout.lineHeight();
		BufferedImage image = WidgetKit.canvas(320, 6 * lh);
		Graphics2D g = WidgetKit.graphics(image);
		g.clipRect(0, lh, 320, lh);
		layout.paint(g, STYLE, 4, 0, ThemeColor.TEXT);
		g.dispose();

		assertEquals("first line is clipped", 0, WidgetKit.painted(image, 0, 0, 320, lh));
		assertTrue("second line shows", WidgetKit.painted(image, 0, lh, 320, lh) > 0);
		assertEquals("third line is clipped", 0, WidgetKit.painted(image, 0, 2 * lh, 320, lh));
	}

	@Test
	public void paintLineDrawsOneLineWherePutAndALinkIsUnderlined()
	{
		TextLayout layout = markup("[link](https://x.example)", 300);
		int lh = layout.lineHeight();
		BufferedImage image = WidgetKit.canvas(320, 3 * lh);
		Graphics2D g = WidgetKit.graphics(image);
		layout.paintLine(g, STYLE, 0, 4, lh, ThemeColor.TEXT);
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, 320, lh - 1));
		assertTrue(WidgetKit.painted(image, 0, lh, 320, lh) > 0);
		int underline = lh + REGULAR.getAscent() + 1;
		assertTrue("underline under the link", WidgetKit.painted(image, 4, underline, 10, 1) > 0);
	}
}
