/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Which party messages are counted, dropped or let through whatever the sender has already sent. */
public class PartyRateGateTest
{
	private static final long ME = 1L;
	private static final long ANN = 11L;
	private static final long BOB = 22L;
	private static final long NOW = 1_000_000L;

	private final PartyRateGate gate = new PartyRateGate();

	@Test
	public void messagesForSomeoneElseDoNotUseUpTheSendersShare()
	{
		for (int i = 0; i < MessageRate.LIMIT * 3; i++)
			assertFalse(gate.allow(from(ANN, BOB, GielinorGoPartyMessage.OFFER), ME, false, false, NOW + i));

		assertTrue(gate.allow(from(ANN, ME, GielinorGoPartyMessage.OFFER), ME, false, false, NOW + 100));
	}

	@Test
	public void aConfirmForAnAgreedTradeGetsThroughAfterTheLimitIsUsedUp()
	{
		for (int i = 0; i < MessageRate.LIMIT; i++)
			assertTrue(gate.allow(from(ANN, ME, GielinorGoPartyMessage.OFFER), ME, false, false, NOW + i));

		assertFalse(gate.allow(from(ANN, ME, GielinorGoPartyMessage.OFFER), ME, false, false, NOW + 50));
		assertTrue(gate.allow(from(ANN, ME, GielinorGoPartyMessage.CONFIRM), ME, true, false, NOW + 60));
	}

	@Test
	public void theBattlePartnerHasAGenerousCapOfTheirOwn()
	{
		for (int i = 0; i < PartyRateGate.TURN_LIMIT; i++)
			assertTrue(gate.allow(from(ANN, ME, GielinorGoPartyMessage.TURN), ME, false, true, NOW + i));

		assertFalse("a flood is cut off", gate.allow(from(ANN, ME, GielinorGoPartyMessage.TURN), ME, false, true,
			NOW + 50));
		assertTrue("a second later turns flow again", gate.allow(from(ANN, ME, GielinorGoPartyMessage.TURN), ME, false,
			true, NOW + PartyRateGate.TURN_WINDOW_MS + 100));
	}

	@Test
	public void aTurnNotFromTheBattlePartnerIsHeldToTheNormalLimit()
	{
		for (int i = 0; i < MessageRate.LIMIT; i++)
			assertTrue(gate.allow(from(ANN, ME, GielinorGoPartyMessage.TURN), ME, false, false, NOW + i));

		assertFalse(gate.allow(from(ANN, ME, GielinorGoPartyMessage.TURN), ME, false, false, NOW + 50));
	}

	@Test
	public void aBroadcastIsHeldToTheLimit()
	{
		for (int i = 0; i < MessageRate.LIMIT; i++)
			assertTrue(gate.allow(from(ANN, 0, GielinorGoPartyMessage.HELLO), ME, false, false, NOW + i));

		assertFalse(gate.allow(from(ANN, 0, GielinorGoPartyMessage.HELLO), ME, false, false, NOW + 50));
	}

	private static GielinorGoPartyMessage from(long sender, long to, String kind)
	{
		GielinorGoPartyMessage message = new GielinorGoPartyMessage(kind, to, 9, 0, 0, new String[0], new int[0]);
		message.setMemberId(sender);
		return message;
	}
}
