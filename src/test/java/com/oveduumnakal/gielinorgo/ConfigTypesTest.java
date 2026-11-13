/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * RuneLite reads settings through a proxy in another module, which cannot touch a type that is not public: the
 * first read throws {@code IllegalAccessError} on the client thread and the client stops. Unit tests never build
 * that proxy, so this test checks that every setting's type is public.
 */
public class ConfigTypesTest
{
	@Test
	public void everySettingTypeIsPublic()
	{
		List<String> hidden = new ArrayList<>();
		for (Method method : GielinorGoConfig.class.getMethods())
		{
			Class<?> type = method.getReturnType();
			if (!type.isPrimitive() && !Modifier.isPublic(type.getModifiers()))
				hidden.add(method.getName() + " returns " + type.getSimpleName());
		}

		assertEquals(new ArrayList<String>(), hidden);
	}
}
