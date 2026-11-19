/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The set-up the Party and Summary tests share: a shell with the real pages in it, a recording stand-in for the
 * plugin's panel actions, a portrait source that draws a plain square, a clock the test moves, and builders for
 * creatures and panel states.
 */
final class PartyRig
{
	/** A move every test creature knows; the game hands out one shared instance per move too. */
	static final Move SLASH = new Move("slash", "Slash", CombatType.SLASH, 1.0, 0.95);

	/** A second move every test creature knows. */
	static final Move BASH = new Move("bash", "Bash", CombatType.CRUSH, 1.4, 0.8);

	/** What the panel actions were called with, one line per call, such as {@code setLead 2}. */
	final List<String> calls = new ArrayList<>();

	/** Every string the pages drew: its text, width, line height and box. */
	final List<Drawn> drawn = new ArrayList<>();

	/** The motion mode the shell reports. */
	MotionMode motion = MotionMode.OFF;

	/** The theme the shell reports. */
	WindowTheme theme = WindowTheme.RUNELITE_DARK;

	/** The time the shell is driven at. */
	long now = 1_000L;

	/** The window width. */
	int width = 535;

	/** The window height. */
	int height = 348;

	/** The notices the shell showed. */
	final List<NoticeMessage> notices = new ArrayList<>();

	/** The pictures that were asked for, by species. */
	final List<String> asked = new ArrayList<>();

	/** The shell under test. */
	final WindowShell shell;

	/** The pages under test. */
	final WindowPages pages;

	/** The picture cache the pages draw from. */
	final PortraitCache pictures;

	/** One string a page drew. */
	static final class Drawn
	{
		final String text;
		final int width;
		final int height;
		final Rectangle box;

		Drawn(String text, int width, int height, Rectangle box)
		{
			this.text = text;
			this.width = width;
			this.height = height;
			this.box = box;
		}

		@Override
		public String toString()
		{
			return "'" + text + "' " + width + "x" + height + " in " + box;
		}
	}

	PartyRig()
	{
		shell = new WindowShell(new WindowShell.Host()
		{
			@Override
			public WindowTheme theme()
			{
				return theme;
			}

			@Override
			public MotionMode motion()
			{
				return motion;
			}

			@Override
			public boolean keyboard()
			{
				return true;
			}

			@Override
			public void resetWindow()
			{
				return;
			}

			@Override
			public void resizeWindow(int width)
			{
				return;
			}

			@Override
			public void notice(NoticeMessage message)
			{
				notices.add(message);
			}
		});
		pictures = new PortraitCache(new PortraitSource()
		{
			@Override
			public void request(String speciesName, Consumer<BufferedImage> onReady)
			{
				asked.add(speciesName);
				onReady.accept(square());
			}
		}, () -> now);
		pages = new WindowPages(shell, recorder(), pictures, () -> now,
			(text, w, h, box) -> drawn.add(new Drawn(text, w, h, box)));
		shell.pages(pages);
	}

	private static BufferedImage square()
	{
		BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0x40A0FF));
		g.fillRect(6, 6, 28, 28);
		g.dispose();
		return image;
	}

	private PanelActions recorder()
	{
		return (PanelActions) Proxy.newProxyInstance(PanelActions.class.getClassLoader(),
			new Class<?>[] {PanelActions.class}, (proxy, method, args) ->
			{
				StringBuilder line = new StringBuilder(method.getName());
				for (int i = 0; args != null && i < args.length; i++)
					line.append(' ').append(describe(args[i]));

				calls.add(line.toString());
				return null;
			});
	}

	private static String describe(Object arg)
	{
		if (arg instanceof TradePick)
			return ((TradePick) arg).from() + "#" + ((TradePick) arg).index();

		if (arg instanceof List)
		{
			List<String> parts = new ArrayList<>();
			for (Object each : (List<?>) arg)
				parts.add(describe(each));

			return parts.toString();
		}

		return String.valueOf(arg);
	}

	/**
	 * Builds a creature as the panel shows it.
	 *
	 * @param name the species name
	 * @param level the level
	 * @param hp the share of health
	 * @param nickname the nickname, or null
	 * @return the view, with a gender and the details a Summary shows
	 */
	static CreatureView creature(String name, int level, double hp, String nickname)
	{
		return new CreatureView(name, level, 0.4, hp, Rarity.RARE, CombatType.SLASH, false, nickname,
			new StatRolls(3, -2, 0, 5, Temperament.BRAVE), 0, Arrays.asList(SLASH, BASH), Collections.emptyList(), 3)
			.withGender(Gender.FEMALE)
			.withDetails("A small green creature with a bad temper.", "Grows into Hobgoblin (20)");
	}

	/**
	 * Builds a panel state with a team, no storage and no eggs.
	 *
	 * @param team the team
	 * @return the state
	 */
	static PanelState state(List<CreatureView> team)
	{
		return new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0, team,
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.emptyList(), TodayView.NONE, TaskView.NONE);
	}

	/**
	 * Builds a team of creatures with different names.
	 *
	 * @param size how many
	 * @return the team
	 */
	static List<CreatureView> team(int size)
	{
		String[] names = {"Goblin", "Cow", "Imp", "Bloodthirsty banshee", "Man", "Rat"};
		List<CreatureView> team = new ArrayList<>();
		for (int i = 0; i < size; i++)
			team.add(creature(names[i], 5 + i * 7, 1.0 - i * 0.18, i == 1 ? "Moo" : null));

		return team;
	}

	/**
	 * Hands the shell a snapshot.
	 *
	 * @param state the state
	 * @param storage whether Storage can be chosen
	 */
	void publish(PanelState state, boolean storage)
	{
		shell.publish(WindowSnapshot.of(now, state, storage));
	}

	/** @return a canvas of the window's size */
	BufferedImage paint()
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

		return image;
	}

	/** Opens the shell at the current time and paints once so the pages have declared their hits. */
	void open()
	{
		shell.open(now);
		paint();
	}

	/**
	 * @param id a hit's id
	 * @return the hit
	 */
	Hit hit(String id)
	{
		for (Hit hit : shell.hits())
		{
			if (hit.id().equals(id))
				return hit;
		}

		throw new AssertionError("no hit " + id + " in " + ids());
	}

	/** @return the ids of every hit, in paint order */
	List<String> ids()
	{
		List<String> ids = new ArrayList<>();
		for (Hit hit : shell.hits())
			ids.add(hit.id());

		return ids;
	}

	/**
	 * Clicks the middle of a hit and paints, as the game thread would on the next frame.
	 *
	 * @param id the hit
	 * @param button the mouse button
	 */
	void click(String id, int button)
	{
		Rectangle b = hit(id).bounds();
		int x = b.x + b.width / 2;
		int y = b.y + b.height / 2;
		shell.onPressed(x, y, button, 0, now);
		shell.onReleased(x, y, button, now);
		paint();
	}

	/**
	 * Left-clicks the middle of a hit.
	 *
	 * @param id the hit
	 */
	void click(String id)
	{
		click(id, MouseEvent.BUTTON1);
	}

	/**
	 * Presses a key and paints.
	 *
	 * @param code the key code
	 */
	void key(int code)
	{
		shell.onKey(code, 0, now);
		paint();
	}

	/** @return how many times each species' picture was asked for */
	Map<String, Integer> askedCounts()
	{
		Map<String, Integer> counts = new HashMap<>();
		for (String species : asked)
			counts.merge(species, 1, Integer::sum);

		return counts;
	}
}
