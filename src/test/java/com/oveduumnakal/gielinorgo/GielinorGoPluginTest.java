/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.gpu.GpuPlugin;

/** Development entry point that launches a RuneLite client with the plugin loaded (used by {@code ./gradlew run}). */
public class GielinorGoPluginTest
{
	public static void main(String[] args) throws Exception
	{
		quietGpuDebugOutput();
		ExternalPluginManager.loadBuiltin(GielinorGoPlugin.class);
		RuneLite.main(args);
	}

	/**
	 * Keeps the GPU plugin's logger at INFO while {@code --debug} turns everything else up to DEBUG. At DEBUG the
	 * GPU plugin installs an OpenGL debug callback, which attaches the graphics driver's own threads to the JVM.
	 * On exit the driver waits for those threads while they wait for the exiting JVM, so the window never closes.
	 * A level set on the GPU plugin's own logger outlives the root-level change {@code --debug} makes.
	 */
	private static void quietGpuDebugOutput()
	{
		((Logger) LoggerFactory.getLogger(GpuPlugin.class)).setLevel(Level.INFO);
	}
}
