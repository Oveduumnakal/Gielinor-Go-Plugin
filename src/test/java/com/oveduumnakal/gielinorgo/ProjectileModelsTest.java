/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;
import org.mockito.InOrder;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that a projectile look is built from the plugin's own copy of the model data, never from the arrays the
 * client keeps in its cache.
 */
public class ProjectileModelsTest
{
	@Test
	public void aRecolourLandsOnACopyAndNeverOnTheClientsSharedData()
	{
		Client client = mock(Client.class);
		ModelData shared = mock(ModelData.class);
		ModelData copy = mock(ModelData.class);
		Model lit = mock(Model.class);
		when(client.loadModelData(ProjectileLook.ARROW.modelId())).thenReturn(shared);
		when(shared.shallowCopy()).thenReturn(copy);
		when(copy.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(lit);

		Model model = new ProjectileModels(client).modelOf(ProjectileLook.ARROW);

		assertSame(lit, model);
		verify(shared, never()).recolor(anyShort(), anyShort());
		InOrder order = inOrder(copy);
		order.verify(copy).cloneColors();
		order.verify(copy, atLeastOnce()).recolor(anyShort(), anyShort());
	}

	@Test
	public void aStillLookIsBuiltAtTheSizeAskedForAndEachSizeIsBuiltOnce()
	{
		Client client = mock(Client.class);
		ModelData shared = mock(ModelData.class);
		ModelData copy = mock(ModelData.class);
		Model lit = mock(Model.class);
		when(client.loadModelData(ProjectileLook.ARROW.modelId())).thenReturn(shared);
		when(shared.shallowCopy()).thenReturn(copy);
		when(copy.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(lit);
		ProjectileModels models = new ProjectileModels(client);

		models.modelOf(ProjectileLook.ARROW, 48);
		models.modelOf(ProjectileLook.ARROW, 48);

		verify(copy, times(1)).scale(48, 48, 48);
		verify(shared, times(1)).shallowCopy();
	}
}
