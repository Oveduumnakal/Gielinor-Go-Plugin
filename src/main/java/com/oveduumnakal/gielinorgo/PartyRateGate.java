/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Decides which party messages are handled and which are dropped before they cost the player any work. A message
 * addressed to another player is none of this player's business: it is dropped without being counted, so other
 * people's traffic cannot use up a sender's share. A reply the player is waiting for in a trade they agreed to
 * always gets through, since losing it leaves half a trade. Everything else is held to {@link MessageRate}.
 */
final class PartyRateGate
{
	private final MessageRate rate = new MessageRate();

	/**
	 * Says whether a message may be handled, and counts it when it is held to the limit.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param myId this player's party member id
	 * @param tradeReply whether the message is the {@code CONFIRM} or {@code REFUSE} of a trade this player agreed to
	 * @param turnPartner whether the message is a turn or leave from the player in their steered battle
	 * @param nowMs wall-clock time in milliseconds
	 * @return false when the message is for someone else or its sender used up their share
	 */
	boolean allow(GielinorGoPartyMessage message, long myId, boolean tradeReply, boolean turnPartner, long nowMs)
	{
		long to = message.to();
		if (to != 0 && to != myId)
			return false;

		if (tradeReply)
			return true;

		return turnPartner || rate.allow(message.getMemberId(), nowMs);
	}

	/** Forgets every sender, for when the player joins or leaves a party. */
	void clear()
	{
		rate.clear();
	}
}
