/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Tests {@link SceneReload}. */
public class SceneReloadTest
{
	/** With no load seen, nothing special ever happens. */
	@Test
	public void quietWithoutALoad()
	{
		SceneReload reload = new SceneReload();
		assertEquals(SceneReload.Step.NONE, reload.next(true));
		assertEquals(SceneReload.Step.NONE, reload.next(true));
	}

	/** After a load: nothing while loading, then drop, then add, then quiet again. */
	@Test
	public void dropsThenAddsAfterALoad()
	{
		SceneReload reload = new SceneReload();
		reload.loading();
		assertEquals(SceneReload.Step.NONE, reload.next(false));
		assertEquals(SceneReload.Step.NONE, reload.next(false));
		assertEquals(SceneReload.Step.DROP, reload.next(true));
		assertEquals(SceneReload.Step.ADD, reload.next(true));
		assertEquals(SceneReload.Step.NONE, reload.next(true));
	}

	/** A second load between the drop and the add starts the sequence over. */
	@Test
	public void secondLoadRestarts()
	{
		SceneReload reload = new SceneReload();
		reload.loading();
		assertEquals(SceneReload.Step.DROP, reload.next(true));
		reload.loading();
		assertEquals(SceneReload.Step.NONE, reload.next(false));
		assertEquals(SceneReload.Step.DROP, reload.next(true));
		assertEquals(SceneReload.Step.ADD, reload.next(true));
	}

	/** Reset forgets a pending load. */
	@Test
	public void resetForgets()
	{
		SceneReload reload = new SceneReload();
		reload.loading();
		reload.reset();
		assertEquals(SceneReload.Step.NONE, reload.next(true));
	}

	/** A load seen with no objects is used up, so the first object added after it is not dropped and added. */
	@Test
	public void aLoadSeenWithNoObjectsIsUsedUp()
	{
		SceneReload reload = new SceneReload();
		reload.loading();
		assertEquals(SceneReload.Step.NONE, reload.next(true, false));
		assertEquals(SceneReload.Step.NONE, reload.next(true, true));
	}

	/** With objects, the sequence is the same as without the flag. */
	@Test
	public void objectsStillDropThenAdd()
	{
		SceneReload reload = new SceneReload();
		reload.loading();
		assertEquals(SceneReload.Step.DROP, reload.next(true, true));
		assertEquals(SceneReload.Step.ADD, reload.next(true, true));
	}
}
