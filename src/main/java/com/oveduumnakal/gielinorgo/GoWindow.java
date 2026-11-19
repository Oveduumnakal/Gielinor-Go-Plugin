/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.LinkBrowser;

/**
 * The painted window as a RuneLite overlay: movable and resizable through RuneLite's own overlay mechanics (hold the
 * drag key, which is read from RuneLite's settings and never assumed to be Alt), above the game's widgets. RuneLite
 * owns the position. The size is stored only as a fraction of the canvas width: after a drag-resize the new width
 * becomes the new fraction, and every frame the overlay's preferred size is set from the fraction, so the window
 * follows the game window and snaps back to its 800:520 shape. The first time it opens it is centred. Everything it
 * shows and does is in {@link WindowShell}; this class is the RuneLite plumbing around it: size, place, the published
 * {@link WindowView} and the settings the shell asks for. The game thread paints it, as for every overlay.
 */
final class GoWindow extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(GoWindow.class);

	/** How long the size must stay still after a drag-resize before it is written to the settings, in milliseconds. */
	private static final long SAVE_DELAY_MS = 400;

	private final GielinorGoConfig config;
	private final ConfigManager configManager;
	private final OverlayManager overlayManager;
	private final LongSupplier clock;
	private final IntSupplier canvasWidth;
	private final IntSupplier canvasHeight;
	private final WindowShell shell;
	private final String version = Changelog.load().currentVersion();
	private volatile WindowView view = WindowView.CLOSED;
	private Dimension lastSize;
	private double fraction = -1;
	private volatile double requestedFraction = -1;
	private volatile boolean recenter;
	private boolean placed;
	private boolean unsaved;
	private long resizedMs;

	/**
	 * Creates the window, closed.
	 *
	 * @param config the plugin's settings
	 * @param configManager stores the window's width share
	 * @param overlayManager saves the window's place after a reset
	 * @param clock the time in milliseconds
	 * @param canvasWidth the game canvas width
	 * @param canvasHeight the game canvas height
	 * @param notice shows a pop-up notice at a point on the canvas
	 */
	@SuppressWarnings("deprecation")
	GoWindow(GielinorGoConfig config, ConfigManager configManager, OverlayManager overlayManager, LongSupplier clock,
		IntSupplier canvasWidth, IntSupplier canvasHeight, NoticeSink notice)
	{
		this.config = config;
		this.configManager = configManager;
		this.overlayManager = overlayManager;
		this.clock = clock;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.shell = new WindowShell(new WindowShell.Host()
		{
			@Override
			public WindowTheme theme()
			{
				return WindowTheme.RUNELITE_DARK;
			}

			@Override
			public MotionMode motion()
			{
				return config.windowAnimations();
			}

			@Override
			public boolean keyHints()
			{
				return config.windowKeyHints();
			}

			@Override
			public boolean keyboard()
			{
				return config.windowKeyboard();
			}

			@Override
			public String sizeLabel()
			{
				return "Size: " + config.windowSize();
			}

			@Override
			public void resetWindow()
			{
				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WINDOW_SIZE,
					WindowSizePreset.MEDIUM);
				applyFraction(WindowFit.DEFAULT_FRACTION);
			}

			@Override
			public void nextSize()
			{
				WindowSizePreset[] all = WindowSizePreset.values();
				WindowSizePreset next = all[(config.windowSize().ordinal() + 1) % all.length];
				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WINDOW_SIZE, next);
			}

			@Override
			public void notice(NoticeMessage message)
			{
				Rectangle b = getBounds();
				notice.show(message, b.x + b.width / 2, b.y + b.height / 2);
			}

			@Override
			public boolean whatsNew()
			{
				return WhatsNew.shouldShow(config.lastSeenVersion(), version, config.whatsNewDismissed());
			}

			@Override
			public void whatsNewSeen()
			{
				if (version == null)
					return;

				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.LAST_SEEN_VERSION, version);
				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WHATS_NEW_DISMISSED, true);
			}

			@Override
			public void openLink(String url)
			{
				LinkBrowser.browse(url);
			}
		});
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setResizable(true);
		setMinimumSize((int) Math.round(WindowFit.DESIGN_H * WindowFit.MIN_SCALE));
	}

	/** Where a notice goes: the plugin shows it and also writes it to chat when that setting is on. */
	interface NoticeSink
	{
		/**
		 * Shows a notice.
		 *
		 * @param message what to say
		 * @param centerX the centre x on the canvas
		 * @param centerY the centre y on the canvas
		 */
		void show(NoticeMessage message, int centerX, int centerY);
	}

	/** @return the shell, which holds the window's state and takes its input on the game thread */
	WindowShell shell()
	{
		return shell;
	}

	/** @return what the UI thread may know about the window; safe from any thread */
	WindowView view()
	{
		return view;
	}

	/** @return where the window is on the canvas, or an empty rectangle before it was first drawn */
	Rectangle screenBounds()
	{
		Rectangle b = getBounds();
		return b == null ? new Rectangle() : new Rectangle(b);
	}

	/**
	 * Sets the window to a share of the canvas width and centres it again. The size is stored and applied on the next
	 * frame, when the canvas size is known.
	 *
	 * @param share the share of the canvas width
	 */
	void applyFraction(double share)
	{
		requestedFraction = share;
		recenter = true;
	}

	/**
	 * Applies a size preset: its share of the canvas width, centred.
	 *
	 * @param preset the preset
	 */
	void applyPreset(WindowSizePreset preset)
	{
		applyFraction(preset.fraction());
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			if (!shell.isVisible())
			{
				view = WindowView.CLOSED;
				return null;
			}

			int cw = canvasWidth.getAsInt();
			int ch = canvasHeight.getAsInt();
			Dimension size = followSize(cw, ch);
			if (!shell.paint(graphics, size.width, size.height, clock.getAsLong()))
			{
				view = WindowView.CLOSED;
				return null;
			}

			view = shell.view(screenBounds());
			return size;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the window", e);
			return null;
		}
	}

	private Dimension followSize(int cw, int ch)
	{
		if (fraction < 0)
			fraction = config.windowFraction();

		Dimension preferred = getPreferredSize();
		if (requestedFraction >= 0)
		{
			store(requestedFraction);
			requestedFraction = -1;
		}
		else if (lastSize != null && preferred != null && !preferred.equals(lastSize))
		{
			fraction = WindowFit.fractionAfterResize(preferred.width, preferred.height, cw, ch);
			resizedMs = clock.getAsLong();
			unsaved = true;
		}

		if (unsaved && clock.getAsLong() - resizedMs >= SAVE_DELAY_MS)
		{
			store(fraction);
			unsaved = false;
		}

		Dimension size = WindowFit.sizeFor(fraction, cw, ch);
		setPreferredSize(size);
		lastSize = size;
		if (recenter || !placed && getPreferredLocation() == null)
		{
			setPreferredLocation(WindowFit.centered(size, cw, ch));
			if (recenter)
				overlayManager.saveOverlay(this);

			recenter = false;
		}

		placed = true;
		return size;
	}

	private void store(double share)
	{
		fraction = share;
		configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WINDOW_FRACTION, share);
	}
}
