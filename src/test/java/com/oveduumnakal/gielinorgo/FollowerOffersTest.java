/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests what the follower bubbles may offer in and out of battle. */
public class FollowerOffersTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private static BattleSession wild()
	{
		BattleState state = new BattleState(Arrays.asList(new Combatant(GOBLIN, 20, 1.0),
			new Combatant(GOBLIN, 10, 1.0)), Collections.singletonList(new Combatant(GOBLIN, 20, 1.0)),
			new Random(1), BattleState.Mode.WILD);
		return new BattleSession(state, Collections.emptyList(), new WildEntry(1, GOBLIN, 20, 1), null,
			new BattleLine("A battle starts", Tone.INFO, true));
	}

	@Test
	public void aBattleThatPlaysItselfOffersItemsAndRun()
	{
		BattleView.ItemOption salve = new BattleView.ItemOption(ItemType.SMALL_SALVE, 2,
			Collections.singletonList(new BattleView.Option(0, "Goblin", 20, 0.5)));

		FollowerOffer offer = FollowerOffers.inBattle(wild(), null, true, Collections.singletonList(salve));

		assertTrue(offer.runShown());
		assertTrue(offer.runEnabled());
		assertFalse(offer.switchShown());
		assertTrue(offer.itemShown());
		assertTrue(offer.itemEnabled());
		assertEquals(1, offer.items().size());
		assertEquals(0.5, offer.runChance(), 1e-9);
	}

	@Test
	public void aSteeredBattleWaitingForItsTurnGreysEverything()
	{
		FollowerOffer offer = FollowerOffers.inBattle(wild(), null, false, Collections.emptyList());

		assertTrue(offer.switchShown());
		assertTrue(offer.itemShown());
		assertTrue(offer.runShown());
		assertFalse(offer.switchEnabled());
		assertFalse(offer.itemEnabled());
		assertFalse(offer.runEnabled());
		assertEquals(1, offer.others().size());
		FollowerOffer.Member other = offer.others().get(0);
		assertEquals(1, other.index());
	}
}
