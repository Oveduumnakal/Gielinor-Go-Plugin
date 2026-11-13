/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Healing items as the turn's action: salves, Revive draughts and Antidotes, in the engine, the game and the box. */
public class BattleItemTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	/** A random source that counts every draw. */
	private static final class Counting extends Random
	{
		private int draws;

		@Override
		public double nextDouble()
		{
			draws++;
			return super.nextDouble();
		}
	}

	private static BattleState wild(Random random, Combatant... mine)
	{
		return new BattleState(new ArrayList<>(Arrays.asList(mine)),
			Collections.singletonList(new Combatant(GOBLIN, "Foe", 10, 1.0)), random, BattleState.Mode.WILD);
	}

	private static Combatant goblin(String name, double health)
	{
		return new Combatant(GOBLIN, name, 40, health);
	}

	private static List<String> texts(TurnResult turn)
	{
		List<String> out = new ArrayList<>();
		for (BattleLine line : turn.lines())
			out.add(line.text());

		return out;
	}

	@Test
	public void aSalveHealsAndCostsTheTurnSoTheFoeAttacks()
	{
		Combatant bob = goblin("Bob", 0.5);
		Counting random = new Counting();
		BattleState state = wild(random, bob, goblin("Ann", 1.0));

		TurnResult turn = state.step(BattleChoice.item(ItemType.SMALL_SALVE, 0));

		List<String> said = texts(turn);
		assertTrue(said.toString(), said.get(0).startsWith("Bob is healed +"));
		assertTrue("the foe attacked: " + said, turn.hits().size() >= 1);
		assertEquals("the foe's blow is the only draw", 1, random.draws);
		assertTrue(turn.turnUsed());
		assertEquals(TurnResult.Status.ONGOING, turn.status());
	}

	@Test
	public void aSalveRestoresAQuarterAndNeverPastFull()
	{
		Combatant bob = goblin("Bob", 0.9);
		BattleState state = wild(new Random(1), bob);
		double before = bob.hp();

		state.step(BattleChoice.item(ItemType.LARGE_SALVE, 0));

		assertTrue("health rose or the foe's blow took it back: " + bob.hp() + " from " + before,
			bob.hp() <= bob.maxHp());
		Combatant other = goblin("Ann", 0.4);
		int healed = other.heal(0.25);
		assertEquals(Math.round(other.maxHp() * 0.25), healed);
		assertEquals(0, goblin("Full", 1.0).heal(0.6));
	}

	@Test
	public void aSalveOnACreatureAtFullHealthIsRefusedAndTakesNoTurn()
	{
		BattleState state = wild(new Random(1), goblin("Bob", 1.0), goblin("Ann", 1.0));

		try
		{
			state.step(BattleChoice.item(ItemType.SMALL_SALVE, 0));
			fail("a salve at full health was allowed");
		}
		catch (IllegalArgumentException expected)
		{
			assertEquals(0, state.totalAttacks());
		}

		assertTrue(state.itemTargets(ItemType.SMALL_SALVE).isEmpty());
	}

	@Test
	public void aReviveDraughtBringsBackAFaintedCreatureAtHalfHealth()
	{
		Combatant bob = goblin("Bob", 1.0);
		Combatant ann = goblin("Ann", 1.0);
		ann.knockOut();
		BattleState state = wild(new Random(1), bob, ann);
		assertEquals(Arrays.asList(1), state.itemTargets(ItemType.REVIVE));
		assertTrue("a salve does not work on a fainted creature", state.itemTargets(ItemType.SMALL_SALVE).isEmpty());

		TurnResult turn = state.step(BattleChoice.item(ItemType.REVIVE, 1));

		assertEquals(ann.maxHp() * ItemRules.REVIVE_TO, ann.hp(), 0.0);
		List<String> said = texts(turn);
		assertTrue(said.get(0).startsWith("Ann is back on their feet"));
		assertTrue("she can be switched in now", state.switchTargets().contains(1));
	}

	@Test
	public void aReviveDraughtIsRefusedForACreatureThatIsUp()
	{
		BattleState state = wild(new Random(1), goblin("Bob", 0.5), goblin("Ann", 1.0));

		try
		{
			state.step(BattleChoice.item(ItemType.REVIVE, 0));
			fail("a revive on a creature that is up was allowed");
		}
		catch (IllegalArgumentException expected)
		{
			assertEquals(0, state.totalAttacks());
		}
	}

	@Test
	public void anAntidoteClearsBurnPoisonAndVenomButNotFreeze()
	{
		for (Status status : Status.values())
		{
			Combatant bob = goblin("Bob", 1.0);
			bob.afflict(status, new Random(1));
			BattleState state = wild(new Random(1), bob);

			boolean usable = !state.itemTargets(ItemType.ANTIDOTE).isEmpty();

			assertEquals(status.name(), status.curable(), usable);
			if (!usable)
				continue;

			TurnResult turn = state.step(BattleChoice.item(ItemType.ANTIDOTE, 0));
			assertNull(status.name(), bob.status());
			List<String> said = texts(turn);
			BattleLine first = turn.lines().get(0);
			assertTrue(said.toString(), said.get(0).startsWith("Bob is cured of "));
			assertTrue("the tag is cleared in the scene", first.tagged());
			assertNull(first.mineStatus());
		}
	}

	@Test
	public void anAntidoteWithNothingToCureIsRefused()
	{
		BattleState state = wild(new Random(1), goblin("Bob", 1.0));

		try
		{
			state.step(BattleChoice.item(ItemType.ANTIDOTE, 0));
			fail("an antidote with nothing to cure was allowed");
		}
		catch (IllegalArgumentException expected)
		{
			assertEquals(0, state.totalAttacks());
		}
	}

	@Test
	public void noItemCanBeUsedWhileTheNextCreatureMustBePicked()
	{
		BattleState state = new BattleState(new ArrayList<>(Arrays.asList(goblin("Dead", 1.0), goblin("Ann", 0.5))),
			Collections.singletonList(new Combatant(GOBLIN, "Foe", 80, 1.0)), new Random(2), BattleState.Mode.WILD);
		while (!state.needsSwitch() && state.ended() == null)
			state.step(BattleChoice.attack());

		assertTrue(state.needsSwitch());
		assertTrue(state.itemTargets(ItemType.SMALL_SALVE).isEmpty());
		try
		{
			state.step(BattleChoice.item(ItemType.SMALL_SALVE, 1));
			fail("an item after a faint was allowed");
		}
		catch (IllegalArgumentException expected)
		{
			assertTrue(state.needsSwitch());
		}
	}

	@Test
	public void theItemHotkeyIsItsOwnChoiceAndShowsInThePrompt()
	{
		List<BattleChoice.Kind> pressed = new ArrayList<>();
		KeyEvent key = new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_F5,
			KeyEvent.CHAR_UNDEFINED)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return KeyEvent.VK_F5;
			}
		};
		BattleHotkeys keys = new BattleHotkeys(() -> Keybind.NOT_SET, () -> Keybind.NOT_SET,
			() -> Keybind.NOT_SET, () -> Keybind.NOT_SET, () -> new Keybind(KeyEvent.VK_F5, 0), () -> true,
			pressed::add);

		assertEquals(BattleChoice.Kind.ITEM, keys.kindOf(key));
		keys.keyPressed(key);

		assertEquals(Arrays.asList(BattleChoice.Kind.ITEM), pressed);
		assertEquals("Attack F1 · Item F5 · Run F4",
			BattlePrompt.hotkeys("F1", null, null, "F5", "F4", false, false));
		assertEquals("Switch F2", BattlePrompt.hotkeys("F1", "F2", "F3", "F5", "F4", true, true));
	}

	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	private GoGame newGame()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN));
		ChampionTable champions = new ChampionTable(Collections.emptyList(), table);
		return new GoGame(table, new EvolutionTable(Collections.emptyList(), table), champions, new Random(5),
			() -> 1_000_000L, notice ->
			{
			});
	}

	@Test
	public void theGameSpendsTheItemOnlyWhenTheBattleAcceptsIt()
	{
		GoGame game = newGame();
		Creature lead = new Creature("goblin", 20, 1_000_000L);
		lead.hpFraction = 0.6;
		lead.hpStampMs = 1_000_000L;
		game.data().team.add(lead);
		game.data().addItems(ItemType.SMALL_SALVE, 2);
		BattleSession session = game.startBattle(new WildEntry(1, GOBLIN, 3, 2));
		assertNotNull(session);

		assertNull("no Large salve is owned", game.act(session, BattleChoice.item(ItemType.LARGE_SALVE, 0)));
		assertNull("an antidote has nothing to cure", game.act(session, BattleChoice.item(ItemType.ANTIDOTE, 0)));
		assertEquals(2, game.data().itemCount(ItemType.SMALL_SALVE));

		TurnResult turn = game.act(session, BattleChoice.item(ItemType.SMALL_SALVE, 0));

		assertNotNull(turn);
		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		BattleView view = game.battleView(session, 10, 1.0);
		assertEquals(Arrays.asList(ItemType.SMALL_SALVE), types(view));
	}

	private static List<ItemType> types(BattleView view)
	{
		List<ItemType> out = new ArrayList<>();
		for (BattleView.ItemOption option : view.items())
			out.add(option.type());

		return out;
	}

	@Test
	public void theQuickItemIsTheFirstOwnedItemThatWouldHelp()
	{
		GoGame game = newGame();
		Creature lead = new Creature("goblin", 20, 1_000_000L);
		lead.hpFraction = 0.6;
		lead.hpStampMs = 1_000_000L;
		game.data().team.add(lead);
		BattleSession session = game.startBattle(new WildEntry(1, GOBLIN, 3, 2));
		assertNull("no item is owned", game.quickItem(session));

		game.data().addItems(ItemType.REVIVE, 1);
		assertNull("a Revive draught has no one to revive", game.quickItem(session));

		game.data().addItems(ItemType.LARGE_SALVE, 1);
		assertEquals(BattleChoice.item(ItemType.LARGE_SALVE, 0), game.quickItem(session));
	}

	@Test
	public void theItemButtonListsItemsAndUsesOneAtOnceWhenItHelpsOneCreature()
	{
		List<BattleChoice> chosen = new ArrayList<>();
		BattleBox box = new BattleBox(chosen::add);
		Combatant mine = new Combatant(GOBLIN, "Bob", 40, 0.5);
		Combatant foe = new Combatant(GOBLIN, "Foe", 10, 1.0);
		List<BattleView.Option> both = Arrays.asList(new BattleView.Option(0, "Bob", 40, 0.5),
			new BattleView.Option(1, "Ann", 30, 0.2));
		List<BattleView.ItemOption> items = Arrays.asList(
			new BattleView.ItemOption(ItemType.SMALL_SALVE, 3, both),
			new BattleView.ItemOption(ItemType.ANTIDOTE, 1, Collections.singletonList(both.get(0))));
		box.show(new BattleView(mine, foe, false, false, null, 0, 10, 1.0, Collections.emptyList(), items));

		assertTrue(box.itemButton().isEnabled());
		JPopupMenu menu = box.itemMenu();
		assertEquals(2, menu.getComponentCount());
		assertTrue("a salve for two creatures opens a list", menu.getComponent(0) instanceof JMenu);
		JMenu salve = (JMenu) menu.getComponent(0);
		assertEquals(2, salve.getItemCount());
		salve.getItem(1).doClick();
		assertEquals(Arrays.asList(BattleChoice.item(ItemType.SMALL_SALVE, 1)), chosen);

		((javax.swing.JMenuItem) menu.getComponent(1)).doClick();
		assertEquals(BattleChoice.item(ItemType.ANTIDOTE, 0), chosen.get(1));
	}

	@Test
	public void theItemButtonIsGreyWhenNoItemWouldHelp()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});
		Combatant mine = new Combatant(GOBLIN, "Bob", 40, 1.0);
		Combatant foe = new Combatant(GOBLIN, "Foe", 10, 1.0);

		box.show(new BattleView(mine, foe, false, false, null, 0, 10, 1.0, Collections.emptyList()));

		assertFalse(box.itemButton().isEnabled());
	}
}
