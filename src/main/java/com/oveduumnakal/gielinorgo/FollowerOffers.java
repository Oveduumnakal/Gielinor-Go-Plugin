/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out what the follower bubbles may offer, from the same rules the window and the battle box use, so they never
 * become a way round one. Holds no RuneLite types.
 */
final class FollowerOffers
{
	private FollowerOffers()
	{
	}

	/**
	 * What may be done out of battle: another team member can be brought to the front, except during an Elite run, and
	 * an item used on any team member it would help.
	 *
	 * @param game the game
	 * @param now the time, for the health each creature has recovered
	 * @return the offer, with nothing shown when there is no team
	 */
	static FollowerOffer outOfBattle(GoGame game, long now)
	{
		FollowerOffer.Builder offer = new FollowerOffer.Builder();
		SaveData data = game.data();
		if (data.team.isEmpty())
			return offer.build();

		List<FollowerOffer.Member> team = new ArrayList<>();
		for (int i = 0; i < data.team.size(); i++)
			team.add(member(game, data.team.get(i), i, now));

		boolean elite = game.eliteOpen();
		offer.switching(elite ? new ArrayList<>() : team.subList(1, team.size()));

		List<FollowerOffer.Item> items = new ArrayList<>();
		for (ItemType type : ItemType.values())
		{
			int count = data.itemCount(type);
			if (count <= 0)
				continue;

			List<FollowerOffer.Member> targets = new ArrayList<>();
			for (int i = 0; i < data.team.size(); i++)
			{
				Creature creature = data.team.get(i);
				if (!type.cures()
					&& ItemRules.appliesOutsideBattle(type, creature.hpFractionAt(now), creature.fainted))
					targets.add(team.get(i));
			}

			items.add(new FollowerOffer.Item(type, count, targets));
		}

		return offer.items(items, 0).build();
	}

	/**
	 * What may be done in a battle. A battle that plays itself offers only Run, against a wild creature. A steered one
	 * offers what its battle box does, greyed while no choice is open, and only Switch after a faint.
	 *
	 * @param session the battle
	 * @param view the open choice, or null when none is open
	 * @param automatic whether the battle plays itself
	 * @return the offer
	 */
	static FollowerOffer inBattle(BattleSession session, BattleView view, boolean automatic)
	{
		boolean open = automatic || view != null && !view.waitingForFriend();
		FollowerOffer.Builder offer = new FollowerOffer.Builder().battle(open);
		BattleState state = session.state();
		if (automatic)
			return session.wild() ? offer.running(state.fleeChance()).build() : offer.build();

		List<FollowerOffer.Member> others = new ArrayList<>();
		for (int place : state.switchTargets())
			others.add(fighter(state, place));

		offer.switching(others);
		if (state.needsSwitch())
			return offer.build();

		List<FollowerOffer.Item> items = new ArrayList<>();
		if (view != null)
		{
			for (BattleView.ItemOption option : view.items())
			{
				List<FollowerOffer.Member> targets = new ArrayList<>();
				for (BattleView.Option target : option.targets())
					targets.add(fighter(state, target.teamIndex()));

				items.add(new FollowerOffer.Item(option.type(), option.count(), targets));
			}
		}

		offer.items(items, state.myIndex());
		if (session.wild())
			offer.running(state.fleeChance());

		return offer.build();
	}

	private static FollowerOffer.Member member(GoGame game, Creature creature, int index, long now)
	{
		String species = game.speciesNameOf(creature);
		String name = creature.nickname == null || creature.nickname.isEmpty() ? species : creature.nickname;
		return new FollowerOffer.Member(index, name, species, creature.npcId, creature.level(),
			creature.fainted ? 0 : creature.hpFractionAt(now));
	}

	private static FollowerOffer.Member fighter(BattleState state, int place)
	{
		List<Combatant> team = state.team();
		Combatant one = team.get(place);
		return new FollowerOffer.Member(place, one.name(), one.speciesKey(), one.npcId(), one.level(),
			one.hpFraction());
	}
}
