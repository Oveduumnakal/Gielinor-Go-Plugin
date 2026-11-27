package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Animations on a loop. Each is a script: things that happen at set times, driving the plugin's own code. Going back in
 * time starts the script again, so it can be scrubbed.
 */
final class LabAnimations
{
	private static final long BASE = 600_000;
	private static final int TICK = 20;
	private static final long OPENING_MS = 3_000;

	private LabAnimations()
	{
	}

	static void add(Map<String, Supplier<LabScenes.Scene>> scenes)
	{
		scenes.put("Animate: window opens and closes", OpenClose::new);
		scenes.put("Animate: health and XP bars change", Bars::new);
		scenes.put("Animate: pointer over rows, then a press", Pointer::new);
		scenes.put("Animate: menu opens", Menus::new);
		scenes.put("Animate: page changes and rows slide", Slides::new);
		scenes.put("Animate: refused tab shakes", Refused::new);
		scenes.put("Animate: search box caret", Caret::new);
		scenes.put("Animate: gilded shine", Shine::new);
		scenes.put("Animate: notice pops, holds, fades", NoticeLife::new);
		scenes.put("Animate: wild creature bubbles", Wild::new);
		scenes.put("Animate: follower bubbles open and close", Follow::new);
		scenes.put("Animate: battle turn (dash, strike, return)", Turn::new);
		scenes.put("Animate: bubbles wait, then follow a walking creature", Walk::new);
		scenes.put("Animate: knocked-out creature pulses", Down::new);
		scenes.put("Animate: portraits arrive late", Late::new);
	}

	/** A scene that replays itself. */
	private abstract static class Script extends LabScenes.Scene
	{
		private final int period;
		private long last = -1;
		private final List<Long> times = new ArrayList<>();
		private final List<Runnable> steps = new ArrayList<>();
		private int done;
		/** The scripted clock, in milliseconds. */
		long now;

		Script(int period)
		{
			this.period = period;
		}

		/** Builds everything afresh and lists the steps. */
		abstract void start();

		/** @return the picture at this moment of the loop */
		abstract BufferedImage draw(long at);

		/** Moves anything that needs ticking forward to this moment. */
		void advance(long at)
		{
		}

		final void at(long time, Runnable step)
		{
			times.add(time);
			steps.add(step);
		}

		@Override
		final BufferedImage frame(long t)
		{
			long loop = t % period;
			if (last < 0 || loop < last)
			{
				times.clear();
				steps.clear();
				done = 0;
				last = 0;
				now = BASE;
				start();
			}

			long from = last;
			for (long step = from; step <= loop; step += TICK)
			{
				long upTo = step;
				while (done < steps.size() && times.get(done) <= upTo)
				{
					now = BASE + times.get(done);
					steps.get(done++).run();
				}

				now = BASE + upTo;
				advance(upTo);
			}

			last = loop;
			now = BASE + loop;
			return draw(loop);
		}
	}

	/** Opens the window long enough before the loop that its opening animation is over when the loop starts. */
	private static void openedBefore(PartyRig rig)
	{
		rig.now = BASE - OPENING_MS;
		rig.shell.open(rig.now);
		rig.paint();
		rig.now = BASE;
		rig.paint();
	}

	/** A script over the painted window. */
	private abstract static class WindowScript extends Script
	{
		PartyRig rig;

		WindowScript(int period)
		{
			super(period);
		}

		List<CreatureView> team()
		{
			return PartyRig.team(6);
		}

		/** Adds the steps; the window has been built but not opened. */
		abstract void script();

		/** Changes the rig before it is given anything to show. */
		void prepare()
		{
		}

		@Override
		final void start()
		{
			rig = new PartyRig();
			prepare();
			rig.theme = theme;
			rig.motion = MotionMode.FULL;
			rig.width = (int) Math.round(800 * scale);
			rig.height = (int) Math.round(520 * scale);
			rig.now = BASE;
			rig.publish(LabData.state(team()), true);
			script();
		}

		/** Starts the loop with the window already open, so it does not fade in again each time round. */
		final void opened()
		{
			openedBefore(rig);
		}

		/** Runs a step on the window at its time, then paints so the window sees it. */
		final void then(long time, Runnable step)
		{
			at(time, () ->
			{
				rig.now = now;
				step.run();
				rig.paint();
			});
		}

		final Rectangle hit(String ending)
		{
			for (String id : rig.ids())
			{
				if (id.toLowerCase().endsWith(ending.toLowerCase()))
					return rig.hit(id).bounds();
			}

			return new Rectangle(-10, -10, 1, 1);
		}

		final void move(String ending)
		{
			Rectangle b = hit(ending);
			rig.shell.onMoved(b.x + b.width / 2, b.y + b.height / 2, rig.now);
		}

		final void press(String ending)
		{
			Rectangle b = hit(ending);
			rig.shell.onPressed(b.x + b.width / 2, b.y + b.height / 2, MouseEvent.BUTTON1, 0, rig.now);
		}

		final void release(String ending)
		{
			Rectangle b = hit(ending);
			rig.shell.onReleased(b.x + b.width / 2, b.y + b.height / 2, MouseEvent.BUTTON1, rig.now);
		}

		final void click(String ending)
		{
			press(ending);
			release(ending);
		}

		@Override
		final BufferedImage draw(long at)
		{
			rig.now = now;
			LabDepth.frame();
			LabElements.frame();
			return rig.paint();
		}
	}

	private static final class OpenClose extends WindowScript
	{
		private OpenClose()
		{
			super(4600);
		}

		@Override
		void script()
		{
			then(300, () -> rig.shell.open(rig.now));
			then(1800, () -> rig.shell.close(rig.now, ShellMotion.Phase.CLOSE));
			then(2700, () -> rig.shell.open(rig.now));
			then(3900, () -> rig.shell.close(rig.now, ShellMotion.Phase.COMBAT_CLOSE));
		}
	}

	private static final class Bars extends WindowScript
	{
		private Bars()
		{
			super(5000);
		}

		private List<CreatureView> changed()
		{
			List<CreatureView> team = new ArrayList<>(PartyRig.team(6));
			team.set(0, PartyRig.creature("Goblin", 5, 0.95, 0.15, null));
			team.set(1, PartyRig.creature("Cow", 12, 0.8, 1.0, "Moo"));
			team.set(2, PartyRig.creature("Imp", 19, 0.1, 0.1, null));
			return team;
		}

		@Override
		void script()
		{
			opened();
			then(900, () -> rig.publish(LabData.state(changed()), true));
			then(2800, () -> rig.publish(LabData.state(team()), true));
		}
	}

	private static final class Pointer extends WindowScript
	{
		private Pointer()
		{
			super(4600);
		}

		@Override
		void script()
		{
			opened();
			then(500, () -> move("row:1"));
			then(1200, () -> move("row:3"));
			then(1900, () -> move("row:0"));
			then(2600, () -> move("more"));
			then(2900, () -> press("more"));
			then(3500, () ->
			{
				rig.shell.onMoved(1, 1, rig.now);
				rig.shell.onReleased(1, 1, MouseEvent.BUTTON1, rig.now);
			});
		}
	}

	private static final class Menus extends WindowScript
	{
		private Menus()
		{
			super(3200);
		}

		@Override
		void script()
		{
			opened();
			then(500, () -> click("more"));
			then(2400, () -> rig.key(KeyEvent.VK_ESCAPE));
		}
	}

	private static final class Slides extends WindowScript
	{
		private Slides()
		{
			super(6000);
		}

		@Override
		void script()
		{
			opened();
			then(600, () -> click("bag"));
			then(1600, () -> click("party"));
			then(2800, () ->
			{
				List<CreatureView> swapped = new ArrayList<>(team());
				Collections.swap(swapped, 0, 3);
				rig.publish(LabData.state(swapped), true);
			});
			then(4000, () -> rig.publish(LabData.state(team().subList(0, 5)), true));
		}

		@Override
		List<CreatureView> team()
		{
			if (shared == null)
				shared = PartyRig.team(6);

			return shared;
		}

		private List<CreatureView> shared;
	}

	private static final class Refused extends Script
	{
		private PartyRig rig;

		private Refused()
		{
			super(2400);
		}

		@Override
		void start()
		{
			rig = new PartyRig();
			rig.theme = theme;
			rig.motion = MotionMode.FULL;
			rig.width = (int) Math.round(800 * scale);
			rig.height = (int) Math.round(520 * scale);
			rig.now = BASE;
			rig.publish(LabData.state(PartyRig.team(6)), false);
			openedBefore(rig);
			at(600, () ->
			{
				rig.now = now;
				for (String id : rig.ids())
				{
					if (id.toLowerCase().endsWith("storage"))
					{
						rig.click(id);
						return;
					}
				}
			});
		}

		@Override
		BufferedImage draw(long at)
		{
			rig.now = now;
			LabDepth.frame();
			LabElements.frame();
			return rig.paint();
		}
	}

	private static final class Caret extends WindowScript
	{
		private Caret()
		{
			super(4000);
		}

		@Override
		void script()
		{
			opened();
			then(100, () -> click("collection"));
			then(500, () -> click("search"));
		}
	}

	private static final class Shine extends WindowScript
	{
		private Shine()
		{
			super(9000);
		}

		@Override
		List<CreatureView> team()
		{
			List<CreatureView> team = new ArrayList<>(PartyRig.team(6));
			for (int i = 0; i < 3; i++)
				team.set(i, new CreatureView(new String[] {"Goblin", "Cow", "Imp"}[i], 9 + i, 0.5, 1.0, Rarity.RARE,
					CombatType.SLASH, true, null));

			return team;
		}

		@Override
		void script()
		{
			opened();
		}
	}

	/** The window opens with no creature pictures yet; they arrive together a moment later and fade in. */
	private static final class Late extends WindowScript
	{
		private final List<Runnable> waiting = new ArrayList<>();

		private Late()
		{
			super(3200);
		}

		@Override
		void prepare()
		{
			waiting.clear();
			rig.deliver = waiting::add;
		}

		@Override
		void script()
		{
			opened();
			then(800, () ->
			{
				waiting.forEach(Runnable::run);
				waiting.clear();
				rig.deliver = Runnable::run;
			});
		}
	}

	private static final class NoticeLife extends LabScenes.Scene
	{
		@Override
		BufferedImage frame(long t)
		{
			BufferedImage image = new BufferedImage((int) (520 * scale), (int) (90 * scale), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			WindowStyle.prepare(g);
			NoticeMessage message = NoticeMessage.STORAGE_LOCKED;
			NoticeClock.Frame frame = new NoticeSlot(message, 300, 0, 0).frame(MotionMode.FULL, t % 5000);
			if (frame.visible && t % 5000 >= 300)
				style().notice(g, message.text(), image.getWidth() / 2.0, image.getHeight() / 2.0, frame.scale,
					frame.alpha);

			g.dispose();
			return image;
		}
	}

	/** The bubbles over a wild creature: they appear, a jar is hovered, then the card, then the pointer leaves. */
	private static class Wild extends Script
	{
		private static final int ID = 7;
		private static final Rectangle VIEW = new Rectangle(0, 0, 640, 420);

		private final BubblePainter painter = new BubblePainter();
		private WildBubbles bubbles;
		private BubbleOffer offer;
		private Dimension card;
		private BubbleFrame shown;
		int pointerX;
		int pointerY;
		boolean near;
		BubbleKind aim;

		private Wild()
		{
			this(7000);
		}

		Wild(int period)
		{
			super(period);
		}

		/** @return where the creature is now */
		BubbleAnchors anchors()
		{
			return new BubbleAnchors(300, 330, 300, 250);
		}

		/** @return whether it lost its battle */
		boolean knockedOut()
		{
			return false;
		}

		/** Adds the steps. */
		void script()
		{
			at(300, this::approach);
			at(1300, () -> aim = BubbleKind.JAR);
			at(2800, () -> aim = BubbleKind.INFO);
			at(4600, this::leave);
		}

		/** Puts the pointer on the creature. */
		final void approach()
		{
			near = true;
			pointerX = (int) anchors().originX();
			pointerY = (int) anchors().originY();
		}

		/** Takes the pointer well away from it. */
		final void leave()
		{
			aim = null;
			near = false;
			pointerX = 620;
			pointerY = 410;
		}

		@Override
		final void start()
		{
			bubbles = new WildBubbles();
			shown = BubbleFrame.NONE;
			Species species = new Species("Imp", 5, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER, Trait.MAGIC)
				.withFactors(1.25, 0.75, 1.0, 1.12);
			WildEntry entry = new WildEntry(ID, species, 14, species.level(), false, false);
			entry.record(WildRecord.of(2, false));
			WildCardContent content = WildCardContent.of(entry, new JarSuggest.JarChoice(JarType.RUNE, 2, 0.456, false),
				false);
			offer = new BubbleOffer(ID, JarType.BRONZE, Arrays.asList(new JarSuggest.JarChoice(JarType.BRONZE, 5, 0.5, false),
				new JarSuggest.JarChoice(JarType.IRON, 2, 0.6, false), new JarSuggest.JarChoice(JarType.RUNE, 1, 0.7, false)),
				true, knockedOut(), content);
			BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = probe.createGraphics();
			card = painter.cardSize(g, content, style());
			g.dispose();
			near = false;
			aim = null;
			pointerX = 600;
			pointerY = 400;
			script();
		}

		@Override
		void advance(long at)
		{
			if (aim != null)
			{
				for (BubbleFrame.Disc disc : shown.discs())
				{
					if (disc.kind() == aim)
					{
						pointerX = (int) disc.x();
						pointerY = (int) disc.y();
					}
				}
			}

			BubbleAnchors anchors = anchors();
			bubbles.tick(offer, anchors, VIEW, near, Collections.emptyList(), pointerX, pointerY, now, MotionMode.FULL);
			shown = bubbles.frame(id -> id == ID ? anchors : null, VIEW, card, now, MotionMode.FULL);
		}

		@Override
		final BufferedImage draw(long at)
		{
			BufferedImage image = new BufferedImage(VIEW.width, VIEW.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			BufferedImage imp = LabPortraits.of("Imp", null);
			if (imp != null)
				g.drawImage(imp, (int) anchors().originX() - 60, (int) anchors().originY() - 80, 120, 120, null);

			painter.paint(g, shown, style(), now);
			pointer(g, pointerX, pointerY);
			g.dispose();
			return image;
		}
	}

	/**
	 * The pointer rests on a bubble while the creature walks off, so the bubbles stay; then it moves back to the
	 * creature, and after the hold the bubbles glide after it.
	 */
	private static final class Walk extends Wild
	{
		private static final int WALKS_AT = 1600;
		private static final int WALK_MS = 800;
		private static final int STEP = 140;

		private double walked;

		private Walk()
		{
			super(7000);
		}

		@Override
		BubbleAnchors anchors()
		{
			return new BubbleAnchors(220 + walked, 330, 220 + walked, 250);
		}

		@Override
		void script()
		{
			walked = 0;
			at(300, this::approach);
			at(1100, () -> aim = BubbleKind.BATTLE);
			at(2600, () ->
			{
				aim = null;
				approach();
			});
			at(5800, this::leave);
		}

		@Override
		void advance(long at)
		{
			walked = STEP * Math.max(0, Math.min(1.0, (at - WALKS_AT) / (double) WALK_MS));
			super.advance(at);
		}
	}

	/** A knocked-out creature: the ring on its Catch bubble pulses. */
	private static final class Down extends Wild
	{
		private Down()
		{
			super(6000);
		}

		@Override
		boolean knockedOut()
		{
			return true;
		}

		@Override
		void script()
		{
			at(300, this::approach);
			at(5000, this::leave);
		}
	}

	private static void pointer(Graphics2D g, int x, int y)
	{
		g.setColor(Color.WHITE);
		g.fillPolygon(new int[] {x, x, x + 11}, new int[] {y, y + 16, y + 11}, 3);
		g.setColor(Color.BLACK);
		g.drawPolygon(new int[] {x, x, x + 11}, new int[] {y, y + 16, y + 11}, 3);
	}

	private static final class Follow extends Script
	{
		private FollowerBubbles bubbles;
		private boolean near;

		private Follow()
		{
			super(4200);
		}

		@Override
		void start()
		{
			bubbles = new FollowerBubbles();
			near = false;
			at(300, () -> near = true);
			at(2800, () -> near = false);
		}

		@Override
		void advance(long at)
		{
			double[] head = {200, 190};
			bubbles.tick(now, LabScenes.followerOffer(), head, near, near ? head : new double[] {390, 255},
				new Rectangle(0, 0, 400, 260));
		}

		@Override
		BufferedImage draw(long at)
		{
			BufferedImage image = new BufferedImage(400, 260, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			new FollowerBubblePainter(new PartyRig().pictures).paint(g, bubbles.dots(), null, style());
			if (near)
				pointer(g, 200, 190);
			else
				pointer(g, 370, 230);

			g.dispose();
			return image;
		}
	}

	/** One attacker dashes in, strikes and walks back, with the moments the game marks. Times are in client ticks. */
	private static final class Turn extends LabScenes.Scene
	{
		@Override
		BufferedImage frame(long t)
		{
			int age = (int) (t / TICK % (TurnMotion.TURN_TICKS + 40));
			BufferedImage image = new BufferedImage(560, 260, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			double dash = TurnMotion.offset(age, 220);
			double settled = age > TurnMotion.TURN_TICKS ? TurnMotion.settle(40, age - TurnMotion.TURN_TICKS) : 0;
			BufferedImage mine = LabPortraits.of("Goblin", null);
			BufferedImage foe = LabPortraits.of("Lesser demon", null);
			if (mine != null)
				g.drawImage(mine, (int) (60 + dash + settled), 110, 110, 110, null);

			if (foe != null)
				g.drawImage(foe, 400, 100, 130, 130, null);

			g.setColor(Color.WHITE);
			g.drawString("tick " + age + (TurnMotion.walking(age) ? "  walking" : ""), 10, 18);
			g.drawString("strike tick " + TurnMotion.strikeTick() + ", impact tick " + TurnMotion.IMPACT_TICK
				+ ", shot " + TurnMotion.SHOT_TICK + ", launch " + TurnMotion.LAUNCH_TICK, 10, 34);
			if (age >= TurnMotion.strikeTick() && age < TurnMotion.strikeTick() + 6)
				g.drawString("STRIKE", 250, 90);

			if (age >= TurnMotion.IMPACT_TICK && age < TurnMotion.IMPACT_TICK + 10)
				g.drawString("IMPACT -12", 440, 90);

			if (age >= TurnMotion.LAUNCH_TICK && age < TurnMotion.IMPACT_TICK)
				g.fillOval(180 + (age - TurnMotion.LAUNCH_TICK) * 220 / Math.max(1,
					TurnMotion.IMPACT_TICK - TurnMotion.LAUNCH_TICK), 60, 8, 8);

			if (age >= TurnMotion.SHOT_TICK && age < TurnMotion.SHOT_TICK + 6)
				g.drawString("shot", 60, 90);

			g.dispose();
			return image;
		}
	}
}
