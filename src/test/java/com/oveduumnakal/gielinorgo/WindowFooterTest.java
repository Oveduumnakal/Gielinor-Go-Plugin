/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the footer additions of the painted window: the Change log page with its links, scrolling and Back, the
 * Feedback menu, and that the five footer buttons fit at every window scale without overlap or cut text.
 */
public class WindowFooterTest
{
	private static final int[][] SIZES = {{535, 348}, {765, 497}, {1344, 874}};

	private final List<String> opened = new ArrayList<>();
	private int seen;
	private boolean whatsNew;
	private int width = 535;
	private int height = 348;
	private final long now = 1_000_000L;
	private final WindowShell shell = new WindowShell(new WindowShell.Host()
	{
		@Override
		public WindowTheme theme()
		{
			return WindowTheme.RUNELITE_DARK;
		}

		@Override
		public MotionMode motion()
		{
			return MotionMode.OFF;
		}

		@Override
		public boolean keyHints()
		{
			return true;
		}

		@Override
		public boolean keyboard()
		{
			return true;
		}

		@Override
		public String sizeLabel()
		{
			return "Size: Medium";
		}

		@Override
		public void resetWindow()
		{
			return;
		}

		@Override
		public void nextSize()
		{
			return;
		}

		@Override
		public void notice(NoticeMessage message)
		{
			return;
		}

		@Override
		public boolean whatsNew()
		{
			return whatsNew;
		}

		@Override
		public void whatsNewSeen()
		{
			seen++;
		}

		@Override
		public void openLink(String url)
		{
			opened.add(url);
		}
	});

	private void paint()
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			shell.paint(g, width, height, now);
		}
		finally
		{
			g.dispose();
		}
	}

	private void openPainted()
	{
		shell.open(now);
		paint();
	}

	private Hit hit(String id)
	{
		for (Hit hit : shell.hits())
		{
			if (hit.id().equals(id))
				return hit;
		}

		return null;
	}

	private void click(String id)
	{
		Rectangle b = hit(id).bounds();
		shell.onMoved(b.x + b.width / 2, b.y + b.height / 2, now);
		click(b.x + b.width / 2, b.y + b.height / 2);
	}

	private void click(int x, int y)
	{
		shell.onPressed(x, y, MouseEvent.BUTTON1, 0, now);
		shell.onReleased(x, y, MouseEvent.BUTTON1, now);
	}

	private void key(int code)
	{
		shell.onKey(code, 0, now);
	}

	private void openChangelog()
	{
		openPainted();
		click(WindowShell.ID_CHANGELOG);
		paint();
	}

	private Point firstLinkOnScreen()
	{
		ChangelogPage page = shell.changelogPage();
		TextLayout text = page.text();
		List<TextLayout.Link> links = text.links(0, 0);
		assertFalse(links.isEmpty());
		TextLayout.Link first = links.get(0);
		page.list().ensureVisible(first.box.y / text.lineHeight());
		paint();
		ScrollList list = page.list();
		int top = list.viewport().y - list.offset();
		for (TextLayout.Link link : text.links(page.textX(), top))
		{
			if (link.url.equals(first.url))
				return new Point(link.box.x + link.box.width / 2, link.box.y + link.box.height / 2);
		}

		throw new AssertionError("no link in the change log");
	}

	@Test
	public void theFooterFitsEveryWindowScaleWithoutOverlapOrCutText()
	{
		for (int[] size : SIZES)
		{
			WindowLayout layout = WindowLayout.of(size[0], size[1]);
			WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, layout.scale);
			List<Rectangle> boxes = Arrays.asList(layout.size, layout.changelog,
				layout.feedback);
			List<String> labels = Arrays.asList("Size: Fit canvas", "What's New", "Feedback");
			BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			for (int i = 0; i < boxes.size(); i++)
			{
				Rectangle a = boxes.get(i);
				assertTrue(size[0] + " inside " + labels.get(i), layout.window.contains(a));
				assertFalse(size[0] + " under the hints " + labels.get(i), a.intersects(layout.hints));
				Font font = style.fitted(g, WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS, labels.get(i),
					a.width - 2 * style.metrics().chipSlant);
				FontMetrics fm = g.getFontMetrics(font);
				assertTrue(size[0] + " text of " + labels.get(i), fm.stringWidth(labels.get(i))
					<= a.width - 2 * style.metrics().chipSlant);
				for (int j = i + 1; j < boxes.size(); j++)
					assertFalse(size[0] + " " + labels.get(i) + " over " + labels.get(j), a.intersects(boxes.get(j)));
			}

			g.dispose();
			assertEquals(layout.size.y, layout.changelog.y);
			assertEquals(layout.window.width - layout.px(WindowLayout.MARGIN),
				layout.feedback.x + layout.feedback.width);
		}
	}

	@Test
	public void theShellPublishesTheFooterHitsInsideTheWindowAtEverySize()
	{
		for (int[] size : SIZES)
		{
			width = size[0];
			height = size[1];
			WindowShell fresh = shell;
			openPainted();
			for (String id : Arrays.asList(WindowShell.ID_CHANGELOG, WindowShell.ID_FEEDBACK))
			{
				Hit h = hit(id);
				assertNotNull(id, h);
				assertTrue(id, new Rectangle(0, 0, width, height).contains(h.bounds()));
			}

			fresh.close(now, ShellMotion.Phase.CLOSE);
		}
	}

	@Test
	public void theChangeLogButtonReadsWhatsNewAndNamesItInItsTip()
	{
		openPainted();
		assertEquals("Read the change log", hit(WindowShell.ID_CHANGELOG).tip());

		whatsNew = true;
		paint();
		assertEquals("See what changed in this version", hit(WindowShell.ID_CHANGELOG).tip());
	}

	@Test
	public void clickingChangeLogOpensThePageAndMarksItSeenOnce()
	{
		openChangelog();

		assertNotNull(shell.changelogPage());
		assertEquals(1, seen);
		assertNotNull(hit(ChangelogPage.ID_BACK));
		TextLayout text = shell.changelogPage().text();
		assertTrue(text.lineCount() > 0);
	}

	@Test
	public void escAndBackReturnFromTheChangeLogAndAnotherEscCloses()
	{
		openChangelog();
		key(KeyEvent.VK_ESCAPE);
		assertNull(shell.changelogPage());
		assertTrue(shell.isOpen());

		paint();
		click(WindowShell.ID_CHANGELOG);
		key(KeyEvent.VK_BACK_SPACE);
		assertNull(shell.changelogPage());
		assertTrue(shell.isOpen());

		paint();
		click(WindowShell.ID_CHANGELOG);
		paint();
		click(ChangelogPage.ID_BACK);
		assertNull(shell.changelogPage());
		paint();
		assertNull(hit(ChangelogPage.ID_BACK));

		key(KeyEvent.VK_ESCAPE);
		assertFalse(shell.isOpen());
	}

	@Test
	public void aTabClickAndClosingLeaveTheChangeLog()
	{
		openChangelog();
		click("tab:BAG");
		assertNull(shell.changelogPage());
		assertEquals(WindowTab.BAG, shell.tab());

		paint();
		click(WindowShell.ID_CHANGELOG);
		shell.close(now, ShellMotion.Phase.CLOSE);
		shell.open(now);
		assertNull(shell.changelogPage());
	}

	@Test
	public void aLinkOpensTheBrowserOnceAndOnlyOnAFullClick()
	{
		openChangelog();
		Point p = firstLinkOnScreen();

		shell.onPressed(p.x, p.y, MouseEvent.BUTTON1, 0, now);
		shell.onReleased(p.x + 200, p.y, MouseEvent.BUTTON1, now);
		assertTrue(opened.isEmpty());

		click(p.x, p.y);
		assertEquals(1, opened.size());
		assertTrue(opened.get(0), opened.get(0).startsWith(
			"https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/"));
		assertFalse(opened.get(0).contains("/new"));

		assertEquals(1, seen);
	}

	@Test
	public void aDragOnALinkScrollsInsteadOfOpeningIt()
	{
		openChangelog();
		Point p = firstLinkOnScreen();
		ScrollList list = shell.changelogPage().list();
		int before = list.offset();

		shell.onPressed(p.x, p.y, MouseEvent.BUTTON1, 0, now);
		shell.onDragged(p.x, p.y - 40);
		shell.onReleased(p.x, p.y - 40, MouseEvent.BUTTON1, now);

		assertTrue(opened.isEmpty());
		assertTrue(list.offset() != before);
	}

	@Test
	public void theWheelScrollsTheChangeLogAndNotTheTabPage()
	{
		openChangelog();
		ScrollList list = shell.changelogPage().list();
		assertTrue(list.scrollable());
		Rectangle view = list.viewport();

		shell.onWheel(view.x + 10, view.y + 10, 3);
		assertTrue(list.offset() > 0);
	}

	@Test
	public void feedbackOpensAMenuWithBothFormsAndTheItemsOpenGitHub()
	{
		openPainted();
		click(WindowShell.ID_FEEDBACK);
		assertEquals(WindowKeyRules.KeyMode.MENU, shell.keyMode());

		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_ENTER);
		assertEquals(1, opened.size());
		assertTrue(opened.get(0), opened.get(0).contains("template=" + IssueLinks.BUG_TEMPLATE));
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());

		paint();
		click(WindowShell.ID_FEEDBACK);
		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_DOWN);
		key(KeyEvent.VK_ENTER);
		assertEquals(2, opened.size());
		assertTrue(opened.get(1), opened.get(1).contains("template=" + IssueLinks.FEATURE_TEMPLATE));
	}

	@Test
	public void escClosesTheFeedbackMenuBeforeAnythingElse()
	{
		openPainted();
		click(WindowShell.ID_FEEDBACK);
		key(KeyEvent.VK_ESCAPE);

		assertTrue(shell.isOpen());
		assertEquals(WindowKeyRules.KeyMode.WINDOW, shell.keyMode());
		assertTrue(opened.isEmpty());
	}

	@Test
	public void theFeedbackAddressesCarryTheVersionOnlyForBugs()
	{
		assertTrue(FeedbackLinks.bugUrl("1.2.3").contains("plugin-version=1.2.3"));
		assertFalse(FeedbackLinks.bugUrl(null).contains("plugin-version"));
		assertFalse(FeedbackLinks.featureUrl().contains("plugin-version"));
		List<ContextMenu.Item> items = FeedbackLinks.menu(opened::add, null);
		assertEquals("Report a bug", items.get(0).label());
		assertEquals("Request a feature", items.get(1).label());
	}

	@Test
	public void issueNumbersBecomeLinksAndHeadingsAreLeftAlone()
	{
		String linked = ChangelogPage.linkIssues("- A fix. (#17, #32)");
		assertTrue(linked, linked.contains("[#17](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/17)"));
		assertTrue(linked, linked.contains("[#32](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/32)"));
		assertEquals("### Wild creatures", ChangelogPage.linkIssues("### Wild creatures"));
		assertEquals("Plain text", ChangelogPage.linkIssues("Plain text"));
	}

	@Test
	public void anEmptyChangeLogSaysSoAndAPopulatedOneListsTheNewestFirst()
	{
		assertEquals(ChangelogPage.EMPTY, ChangelogPage.markup(Changelog.parse("")));
		String markup = ChangelogPage.markup(Changelog.parse("# 2.0.0 - Later\n- b\n# 1.0.0 - Early\n- a\n"));
		assertTrue(markup.indexOf("2.0.0") < markup.indexOf("1.0.0"));
		assertTrue(markup.startsWith("# 2.0.0 - Later"));
	}
}
