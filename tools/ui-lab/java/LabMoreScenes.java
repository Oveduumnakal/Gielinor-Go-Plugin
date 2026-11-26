package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

/**
 * The text above heads, the small overlays, the dialogs and the catch. What the game would place in the world is placed
 * at fixed spots here, and drawn with the lines of the plugin's own overlays.
 */
final class LabMoreScenes
{
	private static final int LOOP_MS = 5000;

	private LabMoreScenes()
	{
	}

	static void add(Map<String, Supplier<LabScenes.Scene>> scenes)
	{
		scenes.put("World: damage numbers, health bars, status", Damage::new);
		scenes.put("World: name tags over creatures", () -> still(LabMoreScenes::tags));
		scenes.put("Motion: catch (throw, wait, result)", Catch::new);
		scenes.put("HUD: pinned tracker", () -> still(LabMoreScenes::tracker));
		scenes.put("HUD: side button", Toggle::new);
		scenes.put("Dialog: confirm", Confirm::new);
		scenes.put("Dialog: right-click menu", Menu::new);
		scenes.put("Dialog: tooltip", Tip::new);
		scenes.put("Window: Changelog", Changes::new);
	}

	private interface Still
	{
		BufferedImage draw(LabScenes.Scene scene);
	}

	private static LabScenes.Scene still(Still drawing)
	{
		return new LabScenes.Scene()
		{
			@Override
			BufferedImage frame(long t)
			{
				return drawing.draw(this);
			}
		};
	}

	private static BufferedImage canvas(int width, int height)
	{
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D begin(BufferedImage image)
	{
		Graphics2D g = image.createGraphics();
		g.setFont(FontManager.getRunescapeBoldFont());
		return g;
	}

	private static void creature(Graphics2D g, String species, int x, int y, int side)
	{
		BufferedImage picture = LabPortraits.of(species, null);
		if (picture != null)
		{
			g.drawImage(picture, x - side / 2, y - side, side, side, null);
			return;
		}

		g.setColor(new Color(40, 40, 40, 160));
		g.fillOval(x - side / 4, y - side, side / 2, side);
	}

	/** Damage lines rising over two creatures, their health bars and a status above one. */
	private static final class Damage extends LabScenes.Scene
	{
		private final NoticeBoard board = new NoticeBoard();
		private final FloatingTextOverlay overlay = new FloatingTextOverlay(null, board, notice -> null,
			anchor -> null, anchor -> null);

		@Override
		BufferedImage frame(long t)
		{
			BufferedImage image = canvas(420, 260);
			Graphics2D g = begin(image);
			creature(g, "Goblin", 110, 230, 110);
			creature(g, "Lesser demon", 310, 230, 130);
			overlay.labBar(g, new Point(110, 100), 0.7);
			overlay.labBar(g, new Point(310, 80), 0.3);
			FloatingTextOverlay.labTag(g, new Point(292, 52), Status.values()[(int) (t / LOOP_MS % Status.values().length)]);
			board.clear();
			long age = t % NoticeBoard.LIFE_MS;
			board.show(Notice.stageFoe(1, "-12", Tone.GOOD), 0);
			board.show(Notice.player("Bob was hit for 4", Tone.BAD), 0);
			board.show(Notice.player("You found an egg", Tone.FIND, JarType.BRONZE), 0);
			List<NoticeBoard.Shown> lines = board.visible(age);
			for (int i = 0; i < lines.size(); i++)
				overlay.draw(g, new Point(i == 0 ? 300 : 70, i == 0 ? 95 : 120 - 18 * (i - 1)), lines.get(i), 1, age);

			g.dispose();
			return image;
		}
	}

	private static BufferedImage tags(LabScenes.Scene scene)
	{
		BufferedImage image = canvas(520, 240);
		Graphics2D g = begin(image);
		String[] names = {"Goblin", "Imp", "Cow"};
		for (int i = 0; i < names.length; i++)
		{
			int x = 100 + i * 160;
			creature(g, names[i], x, 210, 100);
			WildEntry entry = new WildEntry(i, new Species(names[i], 13, CombatType.STAB), 4 + i * 9, 2);
			if (i < 2)
				entry.record(WildRecord.of(i, i == 0));

			WildOverlay.labTag(g, entry, new Point(x - 40, 96));
		}

		FollowerTagOverlay.labTag(g, FollowerTagOverlay.nameFor("Bob", "Goblin", 12), new Point(210, 40));
		g.dispose();
		return image;
	}

	private static BufferedImage tracker(LabScenes.Scene scene)
	{
		List<TrackerView.Line> lines = Arrays.asList(
			new TrackerView.Line(TrackerView.Kind.TASK, "a", "Catch 3 Ranged creatures", "1 / 3", 0.33, null, false),
			new TrackerView.Line(TrackerView.Kind.EGG, "b", "Rare egg", "2200 steps", 0.45, Rarity.RARE, false),
			new TrackerView.Line(TrackerView.Kind.TASK, "c", "Win 2 battles", "Done", 1.0, null, true));
		BufferedImage image = canvas((int) (300 * scene.scale), (int) (180 * scene.scale));
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		new TrackerPainter((text, width, height, box) ->
		{
		}).paint(g, scene.style(), lines);
		g.dispose();
		return image;
	}

	/** The side button: shaken as the scene starts, then showing its waiting dot. */
	private static final class Toggle extends LabScenes.Scene
	{
		private long now;
		private WindowToggle toggle;

		@Override
		BufferedImage frame(long t)
		{
			now = t;
			if (toggle == null)
			{
				toggle = new WindowToggle(() -> theme, () -> MotionMode.FULL, () -> now,
					ImageUtil.loadImageResource(WindowToggle.class, "panel_icon_large.png"));
				now = 0;
				toggle.shake();
				toggle.waiting(true);
				now = t;
			}

			BufferedImage image = canvas(120, 80);
			Graphics2D g = image.createGraphics();
			g.translate(40, 20);
			toggle.render(g);
			g.dispose();
			return image;
		}
	}

	/** A confirm dialog over the window's size. */
	private static final class Confirm extends LabScenes.Scene
	{
		private Modal modal;

		private Rectangle area()
		{
			return new Rectangle(0, 0, (int) (560 * scale), (int) (320 * scale));
		}

		@Override
		BufferedImage frame(long t)
		{
			if (modal == null)
			{
				modal = new Modal("Release Goblin?", "Goblin, level 5, will be gone for good. You get 1 Bronze jar back.",
					Arrays.asList("Keep", "Release"), 0, choice ->
					{
					}, () ->
					{
					});
				modal.open(0);
			}

			BufferedImage image = canvas(area().width, area().height);
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			modal.paint(g, style(), area(), MotionMode.FULL, t);
			g.dispose();
			return image;
		}
	}

	/** A right-click menu with an item that cannot be used. */
	private static final class Menu extends LabScenes.Scene
	{
		private ContextMenu menu;

		@Override
		BufferedImage frame(long t)
		{
			BufferedImage image = canvas((int) (420 * scale), (int) (280 * scale));
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			Rectangle area = new Rectangle(0, 0, image.getWidth(), image.getHeight());
			if (menu == null)
			{
				menu = new ContextMenu();
				List<ContextMenu.Item> items = new ArrayList<>(Arrays.asList(ContextMenu.Item.of("Summary", () ->
				{
				}), ContextMenu.Item.of("Make lead", () ->
				{
				}), ContextMenu.Item.disabled("Use item", "Nothing to use"), ContextMenu.Item.of("Move to storage", () ->
				{
				})));
				menu.open(items, 40, 30, area, style(),
					g.getFontMetrics(style().font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS)), false, 0);
			}

			menu.paint(g, style(), area, MotionMode.FULL, t);
			g.dispose();
			return image;
		}

		@Override
		void input(String type, int x, int y, int button, long t)
		{
			if (menu != null && "click".equals(type) && button != MouseEvent.BUTTON1)
				menu = null;
		}
	}

	/** A tooltip, which appears after its delay. */
	private static final class Tip extends LabScenes.Scene
	{
		private final Tooltip tip = new Tooltip();

		@Override
		BufferedImage frame(long t)
		{
			if (t < 50)
				tip.clear();

			tip.hover("pin", "Pin this task to the screen. Up to 3 can be pinned.", 60, 40, 0);
			BufferedImage image = canvas((int) (420 * scale), (int) (140 * scale));
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			tip.paint(g, style(), new Rectangle(0, 0, image.getWidth(), image.getHeight()), t);
			g.dispose();
			return image;
		}
	}

	/** The changelog page inside the window. */
	private static final class Changes extends LabScenes.Scene
	{
		private PartyRig rig;
		private long base;

		private void build()
		{
			rig = new PartyRig();
			rig.theme = theme;
			rig.motion = MotionMode.FULL;
			rig.width = (int) Math.round(800 * scale);
			rig.height = (int) Math.round(520 * scale);
			rig.publish(LabData.state(), true);
			rig.open();
			rig.now += 2_000;
			rig.paint();
			rig.shell.openChangelog();
			base = rig.now;
		}

		@Override
		BufferedImage frame(long t)
		{
			if (rig == null)
				build();

			rig.now = base + t;
			LabDepth.frame();
			LabElements.frame();
			return rig.paint();
		}
	}

	/** A jar thrown, lying while the game decides, then the line that says how it went. */
	private static final class Catch extends LabScenes.Scene
	{
		private final NoticeBoard board = new NoticeBoard();
		private final FloatingTextOverlay overlay = new FloatingTextOverlay(null, board, notice -> null,
			anchor -> null, anchor -> null);

		@Override
		BufferedImage frame(long t)
		{
			int tick = (int) (t / 20 % (CatchScene.REACT_TICK + 100));
			int flight = JarFlight.flightTicks(8);
			BufferedImage image = canvas(520, 260);
			Graphics2D g = begin(image);
			CatchScene.Phase phase = CatchScene.phaseAt(tick);
			if (phase == CatchScene.Phase.THROWING || phase == CatchScene.Phase.WAITING)
				creature(g, "Imp", 420, 220, 90);

			double progress = tick >= JarFlight.RELEASE_TICK ? JarFlight.progress(tick, flight) : 0;
			double x = 80 + 340 * Math.min(1.0, progress);
			double y = 200 - JarFlight.height(0, 0, Math.min(1.0, progress), JarFlight.arc(8)) * 0.4;
			JarIcons.draw(g, JarType.BRONZE, x - 15, y - 15, 30);
			if (phase == CatchScene.Phase.REACTING || phase == CatchScene.Phase.DONE)
			{
				board.clear();
				board.show(Notice.player("Caught an Imp!", Tone.GOOD, JarType.BRONZE), 0);
				long age = Math.min(NoticeBoard.LIFE_MS - 1, (tick - CatchScene.REACT_TICK) * 20L);
				for (NoticeBoard.Shown line : board.visible(age))
					overlay.draw(g, new Point(360, 140), line, 1, age);
			}

			g.setColor(Color.WHITE);
			g.drawString(phase.name().toLowerCase(java.util.Locale.ROOT), 10, 18);
			g.dispose();
			return image;
		}
	}
}
