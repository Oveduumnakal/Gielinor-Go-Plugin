/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.runelite.client.eventbus.Subscribe;

import static org.junit.Assert.assertEquals;

/**
 * RuneLite's event bus refuses to start a plugin whose subscriber is not named {@code on} plus the event's class
 * name. Unit tests never start the event bus, so this test checks the names instead.
 */
public class SubscriberNamesTest
{
	@Test
	public void everySubscriberIsNamedAfterItsEvent()
	{
		List<String> wrong = new ArrayList<>();
		for (Method method : GielinorGoPlugin.class.getMethods())
		{
			if (method.getAnnotation(Subscribe.class) == null || method.getParameterCount() != 1)
				continue;

			String expected = "on" + method.getParameterTypes()[0].getSimpleName();
			if (!expected.equals(method.getName()))
				wrong.add(method.getName() + " should be " + expected);
		}

		assertEquals(new ArrayList<String>(), wrong);
	}
}
