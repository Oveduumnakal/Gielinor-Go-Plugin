/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/** The Box arrange rule: swap on an occupied cell, move to the end on an empty one, nothing otherwise. */
public class StorageArrangeTest
{
	private static final List<String> LIST = Arrays.asList("a", "b", "c", "d");

	@Test
	public void droppingOnAnotherCreatureSwapsTheTwo()
	{
		assertEquals(Arrays.asList("d", "b", "c", "a"), StorageArrange.apply(LIST, 0, 3));
		assertEquals(Arrays.asList("a", "c", "b", "d"), StorageArrange.apply(LIST, 2, 1));
	}

	@Test
	public void droppingOnAnEmptyCellMovesTheCreatureToTheEnd()
	{
		assertEquals(Arrays.asList("b", "c", "d", "a"), StorageArrange.apply(LIST, 0, 4));
		assertEquals(Arrays.asList("a", "c", "d", "b"), StorageArrange.apply(LIST, 1, 30));
	}

	@Test
	public void theLastCreatureDroppedOnAnEmptyCellStaysLast()
	{
		assertEquals(LIST, StorageArrange.apply(LIST, 3, 9));
	}

	@Test
	public void droppingOnItselfOrOutOfRangeChangesNothing()
	{
		assertEquals(LIST, StorageArrange.apply(LIST, 2, 2));
		assertEquals(LIST, StorageArrange.apply(LIST, -1, 2));
		assertEquals(LIST, StorageArrange.apply(LIST, 4, 1));
		assertEquals(LIST, StorageArrange.apply(LIST, 1, -1));
	}

	@Test
	public void aListOfOneStaysAsItIs()
	{
		List<String> one = Collections.singletonList("a");

		assertEquals(one, StorageArrange.apply(one, 0, 0));
		assertEquals(one, StorageArrange.apply(one, 0, 5));
	}

	@Test
	public void theGivenListIsNeverChanged()
	{
		List<String> mine = Arrays.asList("a", "b");

		List<String> next = StorageArrange.apply(mine, 0, 1);

		assertNotSame(mine, next);
		assertEquals(Arrays.asList("a", "b"), mine);
	}
}
