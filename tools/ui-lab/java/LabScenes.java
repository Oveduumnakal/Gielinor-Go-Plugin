package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.Gson;

/** Everything the lab can draw, each with the plugin's own painters. */
final class LabScenes
{
	/** One thing to look at. Time is milliseconds since the scene started. */
	abstract static class Scene
	{
		WindowTheme theme;
		double scale = 1.0;

		abstract BufferedImage frame(long t);

		void input(String type, int x, int y, int button, long t)
		{
		}

		WindowStyle style()
		{
			return WindowStyle.of(theme, scale);
		}
	}

	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private LabScenes()
	{
	}

	static Map<String, Supplier<Scene>> all()
	{
		Map<String, Supplier<Scene>> scenes = new LinkedHashMap<>();
		for (String tab : new String[] {"Party", "Bag", "Storage", "Collection", "Tasks", "Battle"})
			scenes.put("Window: " + tab, () -> new Window(tab, false, false));

		scenes.put("Window: Summary", () -> new Window("Party", true, false));
		scenes.put("Window: Summary with background", () -> new Window("Party", true, true));
		scenes.put("Bubbles: follower (hover and click)", Follower::new);
		scenes.put("Bubbles: wild creature", () -> still(s -> LookSheet.bubbles(s.theme, s.scale)));
		scenes.put("Bubbles: info card", () -> still(s -> LookSheet.card(s.theme, s.scale)));
		scenes.put("Style: buttons, rows, bars, chips", () -> still(s -> LookSheet.primitives(s.theme, s.scale)));
		scenes.put("HUD: battle controls", BattleBox::new);
		scenes.put("HUD: pinned party box", PartyBox::new);
		scenes.put("HUD: notice", Notices::new);
		scenes.put("Motion: jar throw", Throw::new);
		scenes.put("Motion: jar and item pictures", () -> still(LabScenes::pictures));
		return scenes;
	}

	private interface Still
	{
		BufferedImage draw(Scene scene);
	}

	private static Scene still(Still drawing)
	{
		return new Scene()
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
		return new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
	}

	/** The painted window, driven the way the tests drive it. */
	private static final class Window extends Scene
	{
		private final String tab;
		private final boolean summary;
		private final boolean background;
		private PartyRig rig;
		private long base;

		private Window(String tab, boolean summary, boolean background)
		{
			this.tab = tab;
			this.summary = summary;
			this.background = background;
		}

		private void build()
		{
			rig = new PartyRig();
			rig.theme = theme;
			rig.motion = MotionMode.FULL;
			rig.width = (int) Math.round(800 * scale);
			rig.height = (int) Math.round(520 * scale);
			if (background)
			{
				Gson gson = new Gson();
				rig.pages.scenery(new SummaryScene(BiomeTable.bundled(gson), BiomePlayer.bundled(Runnable::run, gson),
					() -> true));
			}

			rig.publish(PartyRig.state(PartyRig.team(6)), true);
			rig.open();
			rig.now += 2_000;
			rig.paint();
			if (!"Party".equals(tab))
				click(rig.ids().stream().filter(id -> id.toLowerCase().endsWith(tab.toLowerCase())).findFirst().orElse(null));

			if (summary)
				click("row:0");

			base = rig.now;
		}

		private void click(String id)
		{
			if (id != null && rig.ids().contains(id))
				rig.click(id);
		}

		@Override
		BufferedImage frame(long t)
		{
			if (rig == null)
				build();

			rig.now = base + t;
			return rig.paint();
		}

		@Override
		void input(String type, int x, int y, int button, long t)
		{
			if (rig == null)
				build();

			rig.now = base + t;
			if ("move".equals(type))
			{
				rig.shell.onMoved(x, y, rig.now);
			}
			else
			{
				rig.shell.onPressed(x, y, button, 0, rig.now);
				rig.shell.onReleased(x, y, button, rig.now);
			}
		}
	}

	/** The bubbles over the follower, out from the start and answering the pointer. */
	private static final class Follower extends Scene
	{
		private final FollowerBubbles bubbles = new FollowerBubbles();
		private double[] pointer = {200, 230};

		private FollowerOffer offer()
		{
			FollowerOffer.Member bob = new FollowerOffer.Member(1, "Bob", "goblin", 0, 12, 0.55);
			FollowerOffer.Member eve = new FollowerOffer.Member(2, "Eve", "chicken", 0, 7, 1.0);
			return new FollowerOffer.Builder().battle(true)
				.switching(Arrays.asList(bob, eve))
				.items(Arrays.asList(new FollowerOffer.Item(ItemType.SMALL_SALVE, 3, Arrays.asList(bob, eve)),
					new FollowerOffer.Item(ItemType.REVIVE, 1, Collections.emptyList()),
					new FollowerOffer.Item(ItemType.ANTIDOTE, 2, Arrays.asList(bob))))
				.running(0.7)
				.build();
		}

		@Override
		BufferedImage frame(long t)
		{
			BufferedImage image = canvas(400, 260);
			double[] head = {200, 190};
			Rectangle view = new Rectangle(0, 0, 400, 260);
			if (!bubbles.open())
			{
				bubbles.tick(0, offer(), head, true, head, view);
				bubbles.tick(FollowerBubbles.OPEN_MS, offer(), head, true, head, view);
			}

			bubbles.tick(FollowerBubbles.OPEN_MS + t, offer(), head, true, pointer, view);
			Graphics2D g = image.createGraphics();
			g.setColor(new Color(0, 0, 0, 90));
			g.fillOval(180, 215, 40, 12);
			FollowerBubbles.Dot under = bubbles.at(pointer[0], pointer[1]);
			new FollowerBubblePainter(null).paint(g, bubbles.dots(), under, style());
			g.dispose();
			return image;
		}

		@Override
		void input(String type, int x, int y, int button, long t)
		{
			pointer = new double[] {x, y};
			if ("click".equals(type))
				bubbles.press(x, y, button);
		}
	}

	/** The battle controls box. */
	private static final class BattleBox extends Scene
	{
		private final BattleControlsModel model = new BattleControlsModel(choice ->
		{
		});
		private boolean shown;

		private BattleControlsModel.Layout layout()
		{
			if (!shown)
			{
				Combatant mine = new Combatant(GOBLIN, "Archibald", 42, 0.6);
				Combatant foe = new Combatant(GOBLIN, "Lesser demon", 17, 0.25);
				List<BattleView.Option> options = Arrays.asList(new BattleView.Option(1, "Imp", 9, 1.0),
					new BattleView.Option(3, "Bob", 4, 0.5));
				List<BattleView.ItemOption> items = Collections.singletonList(
					new BattleView.ItemOption(ItemType.SMALL_SALVE, 3, options));
				model.show(new BattleView(mine, foe, false, true, JarType.BRONZE, 12, 10, 0.7, options, items),
					new BattleControlsModel.Keys("F1", "F2", "F3", "F5", "F4"));
				shown = true;
			}

			return model.layout(style());
		}

		@Override
		BufferedImage frame(long t)
		{
			BattleControlsModel.Layout layout = layout();
			BufferedImage image = canvas(layout.size.width, layout.size.height);
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			new BattleControlsPainter((text, width, height, box) ->
			{
			}).paint(g, style(), model, layout);
			g.dispose();
			return image;
		}

		@Override
		void input(String type, int x, int y, int button, long t)
		{
			layout();
			if ("click".equals(type))
			{
				model.onPressed(x, y, button == 3 ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
				model.onReleased(x, y, button == 3 ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1);
			}
		}
	}

	/** The pinned party box. */
	private static final class PartyBox extends Scene
	{
		@Override
		BufferedImage frame(long t)
		{
			List<RowModel> rows = PartyBoxModel.rows(PartyRig.team(4));
			BufferedImage image = canvas((int) (260 * scale), (int) (260 * scale));
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			new PartyBoxPainter(new PartyRig().pictures, (text, width, height, box) ->
			{
			}).paint(g, style(), rows, t);
			g.dispose();
			return image;
		}
	}

	/** A notice popping in. */
	private static final class Notices extends Scene
	{
		@Override
		BufferedImage frame(long t)
		{
			BufferedImage image = canvas((int) (520 * scale), (int) (90 * scale));
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			double in = Math.min(1.0, t / (double) Math.max(1, WindowTimes.OPEN));
			style().notice(g, "You can only change your team in a city or near a bank", image.getWidth() / 2.0,
				image.getHeight() / 2.0, 0.9 + 0.1 * in, in);
			g.dispose();
			return image;
		}
	}

	/** A jar thrown ten tiles, over and over, seen from behind and above. */
	private static final class Throw extends Scene
	{
		@Override
		BufferedImage frame(long t)
		{
			double tiles = 10;
			int flight = JarFlight.flightTicks(tiles);
			int arc = JarFlight.arc(tiles);
			int age = JarFlight.RELEASE_TICK + (int) (t / 20 % (flight + 15));
			double progress = JarFlight.progress(age, flight);
			BufferedImage image = canvas(640, 400);
			Graphics2D g = image.createGraphics();
			double pitch = JarLiveView.radians(1500);
			double distance = 900 / scale;
			double[] position = {0, -distance * Math.cos(pitch), -distance * Math.sin(pitch)};
			double[] at = {-420 + 840 * progress, 0, -JarFlight.height(0, 0, progress, arc) + 60};
			JarLiveView view = new JarLiveView(new JarLiveView.Camera(position, 0, pitch, 520, 320, 260), at,
				(age - JarFlight.RELEASE_TICK) / 20.0 * 2 * Math.PI, 1, 0);
			JarType jar = JarType.values()[(int) (t / 20 / (flight + 15) % JarType.values().length)];
			JarModel model = JarShapes.build(jar, true);
			Rectangle area = JarRenderer.bounds(model, view);
			g.drawImage(JarRenderer.render(model, view, area, 4, 0.5, 0xB4), area.x, area.y, null);
			g.setColor(Color.WHITE);
			g.drawString(jar.label() + " jar, " + flight + " ticks, arc " + arc, 10, 18);
			g.dispose();
			return image;
		}
	}

	private static BufferedImage pictures(Scene scene)
	{
		double s = scene.scale;
		BufferedImage image = canvas((int) (560 * s), (int) (200 * s));
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		List<JarType> jars = new ArrayList<>(Arrays.asList(JarType.values()));
		for (int i = 0; i < jars.size(); i++)
		{
			JarIcons.draw(g, jars.get(i), (10 + (i % 9) * 60) * s, (8 + i / 9 * 60) * s, 50 * s);
			JarIcons.draw(g, jars.get(i), (10 + i * 22) * s, 176 * s, 16 * s);
		}

		for (ItemType item : ItemType.values())
			ItemIcons.draw(g, item, (10 + item.ordinal() * 60) * s, 122 * s, 50 * s);

		g.dispose();
		return image;
	}
}
