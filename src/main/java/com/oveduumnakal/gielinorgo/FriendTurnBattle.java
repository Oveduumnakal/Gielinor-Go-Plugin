/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * A battle between two friends who both choose their turns. Each player's computer holds one of these. They start
 * from the same two teams and the same seed and are fed the same pairs of choices, so each plays the same battle
 * on its own, and each message also carries a hash of the sender's battle so a computer that went out of step is
 * noticed. A choice is sent when it is made and only played when both are in; the other player's choice is not
 * shown before then. It holds no game client types and reads no clock: every time that matters is passed in.
 *
 * <p>Nothing is trusted from the other side: a message must be for this battle and this turn, carry a choice that
 * can be made now and a hash that matches. Anything else ends the battle with no result. So does a friend who
 * leaves, or who has not answered 10 seconds after the turn's time ran out. Nothing is won or lost in these
 * battles.
 */
final class FriendTurnBattle
{
	/** Seconds each player has to choose a turn, the same for both players whatever their own setting says. */
	static final int TURN_SECONDS = 20;

	/** Milliseconds each player has to choose a turn. */
	static final long TURN_MS = TURN_SECONDS * 1000L;

	/** How long, in milliseconds, the other player's choice may be late once the time to choose has run out. */
	static final long GRACE_MS = 10_000L;

	private static final long NOT_OPEN = Long.MAX_VALUE;

	/** Why a battle ended with no result. */
	enum End
	{
		/** The friend left the battle, the party, or the game. */
		LEFT("%s left the battle"),

		/** The friend's choice did not arrive in time. */
		SILENT("Your friend stopped answering"),

		/** The two computers stopped agreeing about the battle, or a message made no sense. */
		OUT_OF_STEP("The battle went out of step"),

		/** This player left. */
		MINE("You left the battle");

		private final String text;

		End(String text)
		{
			this.text = text;
		}

		/**
		 * Words the ending.
		 *
		 * @param friend the friend's name
		 * @return the line to show
		 */
		String words(String friend)
		{
			return String.format(text, friend);
		}
	}

	/** A turn that was played, with what the player chose in it. */
	static final class Played
	{
		private final TurnResult result;
		private final BattleChoice mine;

		private Played(TurnResult result, BattleChoice mine)
		{
			this.result = result;
			this.mine = mine;
		}

		/** @return what the turn did, from this player's side */
		TurnResult result()
		{
			return result;
		}

		/** @return what this player chose, or null when the turn was the friend's free switch */
		BattleChoice mine()
		{
			return mine;
		}
	}

	private final long battle;
	private final long friendId;
	private final String friendName;
	private final BattleState state;
	private final Consumer<GielinorGoPartyMessage> outbox;
	private final Deque<Played> played = new ArrayDeque<>();
	private int turn = 1;
	private BattleChoice localChoice;
	private BattleChoice remoteChoice;
	private long deadlineMs = NOT_OPEN;
	private End end;

	/**
	 * Starts a battle. Both players call this with the same teams in the same order and the same seed.
	 *
	 * @param battle the battle's number, as the challenge gave it
	 * @param friendId the other player's party member id
	 * @param friendName the other player's name
	 * @param iAmFirst whether this player sent the challenge
	 * @param mine this player's team, at full health
	 * @param theirs the friend's team, at full health
	 * @param seed both halves of the seed combined
	 * @param outbox sends a message to the party
	 */
	FriendTurnBattle(long battle, long friendId, String friendName, boolean iAmFirst, List<Combatant> mine,
		List<Combatant> theirs, long seed, Consumer<GielinorGoPartyMessage> outbox)
	{
		this.battle = battle;
		this.friendId = friendId;
		this.friendName = friendName;
		this.outbox = outbox;
		this.state = new BattleState(mine, theirs, new Random(seed), BattleState.Mode.CHAMPION)
			.friendMatch(iAmFirst);
	}

	/** @return the battle, from this player's side */
	BattleState state()
	{
		return state;
	}

	/** @return the battle's number */
	long battle()
	{
		return battle;
	}

	/** @return the other player's party member id */
	long friendId()
	{
		return friendId;
	}

	/** @return the other player's name */
	String friendName()
	{
		return friendName;
	}

	/** @return the number of the turn that waits for choices, from 1 */
	int turn()
	{
		return turn;
	}

	/** @return why the battle ended with no result, or null while it goes on or when it was played to the end */
	End ended()
	{
		return end;
	}

	/** @return whether the battle is finished, one way or the other */
	boolean over()
	{
		return end != null || state.ended() != null;
	}

	/**
	 * Tells whether this player owes a choice now. Not when the friend alone must pick the next creature, and not
	 * once the choice for the turn was made.
	 *
	 * @return true when {@link #local(BattleChoice, long)} would take a choice
	 */
	boolean mustChoose()
	{
		return !over() && localChoice == null && !state.foeNeedsSwitch();
	}

	/**
	 * Tells whether the turn waits for the friend's choice and nothing else.
	 *
	 * @return true when this player has nothing to choose, or has chosen, and the friend has not
	 */
	boolean waitingForFriend()
	{
		return !over() && !mustChoose() && friendOwes() && remoteChoice == null;
	}

	private boolean friendOwes()
	{
		return !state.needsSwitch();
	}

	/**
	 * Says why a choice cannot be made now.
	 *
	 * @param choice the choice
	 * @return null when it can be made, otherwise the reason, in words
	 */
	String refusal(BattleChoice choice)
	{
		if (!mustChoose())
			return "Nothing to choose now";

		return state.refusal(true, choice);
	}

	/**
	 * Starts the clock of the turn that waits, when the player can see it. Called once the previous turn has been
	 * shown. A turn already started keeps its clock.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void opened(long nowMs)
	{
		if (deadlineMs == NOT_OPEN)
			deadlineMs = nowMs + TURN_MS;
	}

	/**
	 * Takes this player's choice for the turn and sends it. The turn is played when the friend's choice is in too.
	 *
	 * @param choice the choice
	 * @param nowMs the time in milliseconds
	 * @return false when the choice cannot be made now, which changes nothing
	 */
	boolean local(BattleChoice choice, long nowMs)
	{
		if (choice == null || refusal(choice) != null)
			return false;

		localChoice = choice;
		outbox.accept(GielinorGoPartyMessage.turn(friendId, battle, turn, encode(choice), state.stateHash()));
		if (remoteChoice != null || state.needsSwitch())
			play();

		return true;
	}

	/**
	 * Takes a message from the friend about this battle: a choice for a turn, or leaving. A message that is not for
	 * this battle is ignored. One that is for it but makes no sense ends the battle.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param nowMs the time in milliseconds
	 */
	void remote(GielinorGoPartyMessage message, long nowMs)
	{
		if (over() || message.battle() != battle || message.getMemberId() != friendId)
			return;

		if (GielinorGoPartyMessage.LEAVE.equals(message.kind()))
		{
			end(End.LEFT);
			return;
		}

		if (!GielinorGoPartyMessage.TURN.equals(message.kind()))
			return;

		BattleChoice choice = decode(message.choice());
		if (message.turn() != turn || remoteChoice != null || !friendOwes() || choice == null
			|| message.hash() != state.stateHash() || state.refusal(false, choice) != null)
		{
			end(End.OUT_OF_STEP);
			return;
		}

		remoteChoice = choice;
		if (localChoice != null || state.foeNeedsSwitch())
			play();
	}

	/**
	 * Moves the clocks on. When the time to choose ran out the creature chooses for itself, which sends that
	 * choice, and when the friend's choice is still missing 10 seconds after that the battle ends.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void timeout(long nowMs)
	{
		if (over() || deadlineMs == NOT_OPEN)
			return;

		if (mustChoose() && nowMs >= deadlineMs)
			local(automatic(), nowMs);

		if (!over() && waitingForFriend() && nowMs >= deadlineMs + GRACE_MS)
			end(End.SILENT);
	}

	/**
	 * Chooses for the player who ran out of time: the next creature after a faint, otherwise an attack.
	 *
	 * @return the choice
	 */
	BattleChoice automatic()
	{
		return state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive()) : BattleChoice.attack();
	}

	/** Leaves the battle: tells the friend, and ends it with no result. Nothing happens once it is over. */
	void leave()
	{
		if (over())
			return;

		outbox.accept(GielinorGoPartyMessage.leave(friendId, battle));
		end(End.MINE);
	}

	/**
	 * Ends the battle with no result, as when the friend left the party.
	 *
	 * @param why the reason
	 */
	void end(End why)
	{
		if (end == null && state.ended() == null)
			end = why;
	}

	/** @return the next turn that was played and not yet taken, or null */
	Played poll()
	{
		return played.pollFirst();
	}

	private void play()
	{
		BattleChoice mine = state.foeNeedsSwitch() ? null : localChoice;
		BattleChoice theirs = state.needsSwitch() ? null : remoteChoice;
		BattleChoice shown = localChoice;
		localChoice = null;
		remoteChoice = null;
		deadlineMs = NOT_OPEN;
		try
		{
			played.addLast(new Played(state.step(mine, theirs), shown));
			turn++;
		}
		catch (IllegalArgumentException | IllegalStateException e)
		{
			end(End.OUT_OF_STEP);
		}
	}

	/**
	 * Writes a choice as short text for a message: {@code A} or {@code A2} for an attack, the second move, {@code S1}
	 * for a switch to the second creature, {@code ISMALL_SALVE:0} for an item and the creature it is used on.
	 *
	 * @param choice the choice
	 * @return the text
	 */
	static String encode(BattleChoice choice)
	{
		switch (choice.kind())
		{
			case ATTACK:
				return choice.moveIndex() < 0 ? "A" : "A" + choice.moveIndex();

			case SWITCH:
				return "S" + choice.teamIndex();

			case ITEM:
				return "I" + choice.itemType().name() + ":" + choice.teamIndex();

			default:
				return "?";
		}
	}

	/**
	 * Reads a choice written by {@link #encode(BattleChoice)}. Nothing is trusted: text of any other shape, a number
	 * that is too large or an unknown item reads as nothing.
	 *
	 * @param text the text as sent, possibly null
	 * @return the choice, or null when the text is not one
	 */
	static BattleChoice decode(String text)
	{
		if (text == null || text.isEmpty() || text.length() > 40)
			return null;

		String rest = text.substring(1);
		switch (text.charAt(0))
		{
			case 'A':
				if (rest.isEmpty())
					return BattleChoice.attack();

				int move = number(rest);
				return move < 0 ? null : BattleChoice.attack(move);

			case 'S':
				int place = number(rest);
				return place < 0 ? null : BattleChoice.switchTo(place);

			case 'I':
				int colon = rest.indexOf(':');
				int target = colon < 0 ? -1 : number(rest.substring(colon + 1));
				ItemType type = colon < 0 ? null : itemNamed(rest.substring(0, colon));
				return target < 0 || type == null ? null : BattleChoice.item(type, target);

			default:
				return null;
		}
	}

	private static int number(String digits)
	{
		if (digits.isEmpty() || digits.length() > 3)
			return -1;

		for (int i = 0; i < digits.length(); i++)
		{
			if (digits.charAt(i) < '0' || digits.charAt(i) > '9')
				return -1;
		}

		return Integer.parseInt(digits);
	}

	private static ItemType itemNamed(String name)
	{
		for (ItemType type : ItemType.values())
		{
			if (type.name().equals(name))
				return type;
		}

		return null;
	}
}
