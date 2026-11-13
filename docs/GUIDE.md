# Gielinor Go: full guide

Every feature and every number, in detail. For the short version, see the [README](../README.md).

### Wild creatures

- **Monsters near you sometimes turn wild**

  When a monster turns wild, a smaller copy of it (75% size) appears on a free tile within two tiles of it, on
  your side of any wall or fence, with a soft outline and a level tag. The real monster carries on as if nothing
  happened and is never touched. The wild copy is its own creature: it wanders a step or two (never more than 3
  tiles from where it appeared), stays about 3 minutes (the clock stops while you fight or catch it) and fades out
  over 2 seconds when its time is up, when you walk more than 25 tiles away or change floors. If there is no free
  tile you can walk to, no wild creature appears. Click it to **Battle** it (a plain left click does that), or
  right-click it for **Battle** and **Catch**. Once your lead has beaten it and it lies knocked out, a plain left
  click throws your selected jar instead. A click on the copy never walks you to the tile behind it. Any monster you
  can fight can turn wild, from chickens to bosses, and stronger monsters are rarer finds. At most three wild
  creatures exist at once.

- **Catch them with jars**

  Throwing a jar has a chance to catch the creature. The menu shows your odds before you click. Stronger monsters
  are harder to catch, and better jars (Bronze, Iron, Rune, Dragon) improve your chance.

- **See the catch happen**

  Your character makes a throwing motion when you throw a jar, then cheers for a catch, stamps when the
  creature breaks free and cries when it runs away (skipped if you moved or started something else). The jar you threw
  flies in an arc from your hand to the creature. The creature reacts when the jar lands: a creature you catch
  flinches and shrinks into the jar; one that breaks free turns to you, attacks and stands again; one that runs
  away turns and runs off, fading out. The result text floats over the creature's copy. Only you see this, and no real monster is ever hidden or
  changed. You can switch it off in the settings: a caught or fled creature then just disappears.

- **Earn jars by playing**

  You start with 10 Bronze jars. Walking earns more ("Found a Bronze jar"), every level-up in the real game gives three, and battle wins
  can drop better ones as your trainer level grows.

- **Find specialty jars while skilling**

  Training a skill can turn up a jar that is much better on one kind of creature. Mining finds Cave jars for
  creatures you meet underground, Agility finds Swift jars for fast attackers, Prayer finds Blessed jars for
  undead and demons, and so on for almost every skill. When you own one that suits the creature in front of
  you, it shows up as its own **Catch** option with its odds. Every jar has its own small picture, in the jar list on
  the panel, on the champion cards and beside the "Found a ... jar" lines that float over your head.

- **Healing items**

  Four items help a hurt creature. A **Small salve** restores 25% health and a **Large salve** 60%. A **Revive
  draught** lifts a creature that is too tired to battle (below 25% health) to 50%. An **Antidote** cures poison,
  venom and burn, in battle only. Small salves turn
  up while training Herblore or Cooking and after about one battle win in ten; Herblore also finds Antidotes.
  Bigger XP drops find more, up to a 30% chance, and the roll is separate from the specialty jar's, so one drop can
  give a jar, an item, both or neither. Battle wins can also drop a Large salve (3%) or a Revive draught (2%), and
  your first win over each City Champion pays 2 Large salves and a Revive draught. Right-click a creature on the
  Team or Storage tab and choose **Use** to pick an item that would help it; one that would do nothing is not
  offered and never spent. Health also comes back by itself over five minutes. The **Items** line under the jar list
  shows how many of each you own, greyed at zero. Items cannot be traded.

- **Everything shows where it happens**

  News about a creature floats over that creature: it appeared, you caught it, it broke free. Things you find or
  earn, such as jars and level-ups, float over your own head.

### Your team

- **Your XP trains your creatures**

  Every XP drop you earn, in any skill, also trains your team of up to six creatures. The creature in the lead
  slot learns twice as fast.

- **Your lead walks with you**

  Your lead creature follows a step behind you as a small copy of itself, so it never gets in the way. A gilded
  lead follows in gold. A creature looks like the monster you caught: the small Monkey Guard stays small, in the
  follower, the Team card and the battle scenes. Creatures that hatch, evolve or come from a trade, and creatures
  caught before this was added, look like the species' usual monster. Only you can see it, it cannot be clicked, and
  you can switch it off in the settings.

- **Every creature is one of a kind**

  Each creature you catch or hatch rolls four hidden bonuses, for health, attack, defence and speed, and one of ten
  temperaments. A roll runs from -5 to +5 and is worth 1.5% of that stat, so a creature is up to 7.5% stronger or
  weaker in each. A temperament is either Steady, which changes nothing, or gives 10% more of one stat and 10% less
  of another: Brave (attack up, defence down), Reckless, Fierce, Guarded, Stubborn, Hardy, Stout, Hasty and Nimble.
  The four rolls add up to a rating: Weak, Average, Strong or Exceptional. The card shows the temperament beside the
  style and the rating on the right, and hovering explains them: the temperament with its effect (`Nimble: +10% Speed, -10% Health`) and each
  stat's roll with the net percentage it really gets from roll and temperament together (`Speed -4 (+3%)`).
  Average creatures show no rating word, and a rated lead shows its rating in place of "Lead". Catching a rated
  creature adds a line with its rating. The faster creature strikes first in a battle; with equal speed the higher
  level goes first, and then it is a coin toss. Wild creatures fight at average until you catch them. Creatures
  you had before this update keep exactly the strength they had: average rolls and the Steady temperament. A
  creature keeps its rolls when it evolves, moves to Storage or is traded.

- **Nicknames**

  Right-click a creature in Team or Storage and choose **Rename...** to give it a name of 1 to 12 letters, numbers,
  spaces or hyphens. The nickname shows on its card (hover the card for the species), in Storage, in battle
  lines and in level-up and evolution lines, and a creature keeps it when it evolves. Leave the box empty to go
  back to the species name. With **Show lead's name** on (the default), your lead's name (its nickname, or its species name
  without one) floats above it at half opacity as it follows you. Nicknames stay on your computer: friends never see them, and a creature you trade away
  arrives without one.

- **Moves**

  Every creature knows up to four moves. A move has a style (Melee, Ranged or Magic), a power (1 is a plain
  attack), an accuracy (a move below 100% can miss and does no damage then) and sometimes an effect. A creature
  starts with the newest four moves its level has unlocked. New moves unlock at set levels: a dragon bites at level
  1, claws at 10, uses Dragonfire at 20 and Inferno at 40, a wizard goes from a strike to a bind to a blast, a spider
  adds a venom bite, and about twenty families have a pool of their own, written in the table
  `scripts/move-pools.json`. A new move is learned at once while the creature knows fewer than four ("Bob learned
  Dragonfire!"). With four known it waits ("Bob wants to learn Dragonfire"): a gold **New move** line shows on the
  creature's card, and clicking it (or **Moves...** in the card's right-click menu) lets you pick a move to forget
  or skip the new one. Hover a card to see its moves with their power and accuracy. An evolved creature keeps the
  moves its new species can still know. A wild creature has the newest four moves its level unlocks, and an
  automatic battle picks a move for your creature by power, accuracy and the style match-up. Friends see the same
  moves, and a friend's creature can only use moves its species could really know.

- **Status effects**

  Some moves can leave a creature **poisoned** (PSN), **envenomed** (VNM), **burned** (BRN), **frozen** (FRZ) or
  **stunned** (STN) after a hit: a venom bite or a scorpion's sting poisons, a constrictor envenoms, Dragonfire and fire
  spells burn, an icy breath freezes, a bind or a shield bash stuns. A poisonous creature's melee hit also poisons one
  time in five. Poison and burn take 1/16 of full health at the end of each round and venom takes 1/20, then 2/20,
  then 3/20 and on, all for four rounds; a burned creature's melee hits do a quarter less. A frozen creature loses
  one or two turns and a stunned one has a 50% chance to lose each of three turns. A creature has one status at a
  time, poisonous creatures cannot be poisoned or envenomed and fiery ones cannot be burned. A status ends with its
  time, when the creature is switched out and when the battle ends; it is never saved. The battle text says it
  ("Bob is poisoned!", "Bob is hurt by poison -3 (12/40)", "Bob is frozen solid", "Bob shook it off"), a small
  coloured tag floats over an affected creature in a battle that is acted out, and the battle box shows the tag next
  to each name. Friends see the same statuses (party protocol 4).

- **Battles that play themselves**

  Choose **Battle** on a wild creature and your lead fights it right away. When it faints, the next creature of
  your team that can fight steps in by itself ("Go, Bob!") and the battle goes on; it is lost only when your whole
  team is down. Creatures below a quarter of their health sit out, so when the lead is too tired the first rested
  creature opens. A win pays once and the creature that lands the last blow gets the creature XP. Each hit floats over the creature's
  head, and the hits your own creature takes float over yours, so you never have to stop skilling or fighting. Melee beats Ranged, Ranged beats Magic, and Magic beats
  Melee. A creature's card tooltip reminds you of its style's match-up. There is no opening line: the fight goes
  straight from the face-off to the first blow. A creature that loses to you is easier to catch. Every attack is one of the creature's moves, and the
  line says which ("Bob uses Dragonfire: -14" or "Bob's Bite missed"); see **Moves** below.

  With **Battle animations** on (the default), your lead walks up to the wild creature and they face each other
  two tiles apart (one tile when the far spot cannot be reached). The lead walks round walls and objects, and
  every step of a melee attacker is over open ground. When the lead has no way to get to the wild creature (it
  stands behind a fence), the battle is shown as text and says so once. Each hit plays as an attack by one and a flinch by the
  other, one every 3 game ticks, and the flinch and the floating line come at the same moment of the turn for every
  attack. A Melee attacker steps up, strikes and steps back. A Ranged or Magic attacker stays where it is and an
  arrow or a bolt flies from one body to the other, at the height each is drawn at; a dragon steps in to bite but
  stays put to use Dragonfire, and a wyvern uses Icy breath. A move that misses is acted out like a hit, but the
  other creature does not flinch.
  The arrow, bolt and breath are drawn on your own screen only, they are not real game projectiles, so other plugins
  do not see them. The loser lies down for a few seconds before getting up. Your lead then walks back to you. The fight is
  played on the wild copy itself, which turns to face your lead. Only you see any of this, and the real monster is
  untouched. A breath plays the creature's breath animation. Creatures with no animation for a
  move lunge or tip over instead. Every variant of a monster that shares its name (a goblin of any kind, a small or a big
  Monkey Guard) has its own animations, and a monster the data does not know yet stands and walks the way the game
  draws it. Switch it off in the settings.

  **Catch a knocked-out creature.** When your lead wins, the wild creature lies knocked out for about 10 seconds.
  A plain left click on it, or **Catch** in the right-click menu, throws your selected jar (you can also do this
  while it fights). A knocked-out creature is twice as easy to catch (x2.0); once it is back up it is x1.5 as
  before. Caught: the jar flies to the lying creature, which shrinks into it (it does not get up to flinch). Broke
  free: it gets up, turns to you, attacks and the scene ends. Ran away: it gets up and runs off fading out, and it
  is no longer wild. A jar thrown while it still fights is played on the creature the same way.

- **Choose your own moves**

  Turn on **Choose battle actions** in the settings (it is off by default, and then every battle plays itself as
  described above, with your whole team: the next creature steps in on its own). A battle with a wild creature or a City Champion then waits for you each
  turn, and the battle box appears at the top of the side panel, above the tabs, with both creatures' health, a
  countdown bar and five buttons:

  - **Attack**: your creature attacks, then the other one does (or the other way round: the faster creature is
    first, and a level tie is a coin toss). A creature that knows more than one move opens the list of its moves
    under the button; pick one. The Attack hotkey and the 10-second timeout let the creature pick for itself.
  - **Switch**: pick any other creature of your team that can still fight. It costs your turn, so the other creature
    attacks the one that steps in. When your creature faints you must pick the next one, and that switch is free.
    The battle is lost when your whole team is down. Only creatures rested to a quarter of their health or more
    take part.
  - **Catch** (wild creatures only): throws the jar chosen in the header. The chance grows with the health the
    creature has lost, from the normal chance at full health up to 1.5 times it near zero (the right-click menu shows
    the odds). A jar that does not hold costs your turn and the creature attacks; a catch or a creature that runs off
    ends the battle with the usual catch scene.
  - **Item**: uses a healing item you own as your turn, so the other creature attacks. A Small or Large salve heals
    a creature of your team that is still up and hurt, a Revive draught brings back a creature that fainted in this
    battle at half health, and an Antidote cures poison, venom or burn on the creature in the fight. The button lists
    only items that would do something and asks which creature when an item could help several. The item is spent
    once the turn is played, and nothing is spent when it would do nothing. From a creature's card in the panel,
    items cannot be used while a battle is on.
  - **Run**: ends the battle. Nothing is won and that creature cannot be battled again. Logging out, hopping, walking
    more than 15 tiles away or the monster going away counts as running.

  Choose nothing for 10 seconds (set **Seconds per turn** from 5 to 30) and your creature attacks by itself, or steps
  in the next creature after a faint. A small prompt on the game screen shows whose turn it is, the time left and both
  health values, and you can set **Attack**, **Switch**, **Catch**, **Item** and **Run** hotkeys (none are set by default, so no
  key is taken from chat or dialogue, and they work only while a choice is open). The Switch hotkey steps in the first
  creature that can fight, and the Item hotkey uses the first item that would help (Small salve, Large salve, Revive
  draught, Antidote); use the panel buttons to pick a creature or an item.

  Every blow and every switch ("Go, Bob!", "Bob comes back", "You ran away") floats as it happens, with no summary.
  With **Battle animations** on, the scene waits for you between turns: the lead and the wild creature (or the two
  teams in a champion battle) stand ready, a creature that steps in takes the lead's place and one that faints lies
  down and is replaced. With it off, the same battle is played with floating text only. Winning pays the same as an
  automatic win (XP for the creature that won, jar and egg drops, the champion's sigil, tasks), and a beaten wild
  creature lies knocked out and can be caught at the x2.0 odds. Friend battles always play themselves.

- **Creatures evolve**

  Many creatures grow into a stronger relative when they reach a certain level: an Imp becomes a Lesser demon,
  then a Greater demon, then a Black demon. There are over 150 evolutions to find.

- **Eggs hatch as you walk**

  Battle wins sometimes drop an egg. Carry up to three, and each hatches into a new creature once you have
  walked far enough. Rarer eggs take longer and hatch rarer creatures.

- **Gilded creatures**

  One wild creature in 500 is gilded: gold outline in the world, gold name in your panel. They are only for
  showing off.

### Spawn variety

- **Featured today**

  Each day one species of every rarity is featured, the same for every player on the same plugin version, and a
  featured species turns wild three times as often. The Today section at the top of the Log tab names them and
  shows when they change (midnight UTC). A star marks them in the Log, and the Log's filter has a **Featured**
  choice.

- **Night**

  Between 20:00 and 06:00 on your computer's clock, undead, demons and other unholy creatures turn wild twice as
  often.

- **Weekend events**

  Each Saturday and Sunday (UTC) one group turns wild twice as often and is gilded twice as often: Dragons, Undead,
  Tide, Giants, Slayer monsters or Goblins, a new one each week. When boosts overlap they multiply, up to six times.

- **Roaming creatures**

  One wild creature in 20 is a stronger roaming one: 10 levels higher, with "Roaming" in front of its name. Beating
  one drops an egg 30% of the time instead of 10%, and the egg is one rarity higher.

  Switch featured species, night, weekend events and roaming creatures off with **Spawn events** in the settings.

### Daily and weekly tasks

- **Three daily tasks and one weekly task**

  Every day at 00:00 UTC (the same moment the game's daily reset happens) three new tasks appear, and a new weekly
  task every Monday. They are the same for every player. Daily tasks are things like catching a few creatures,
  catching ones of one combat style, catching one underground, winning battles, walking, gaining XP in any skill or
  hatching an egg. The weekly task is bigger: beat a City Champion, catch 25 creatures, win 15 battles or walk
  5,000 tiles. They show on the Team tab under your eggs, with a bar for each and the time left to the reset.

- **Rewards are paid for you**

  When a task is done the reward is paid at once and floats over your head: 2 jars of the best tier you have
  unlocked, or 100 trainer XP, for a daily task; 5 jars and a Rare egg for the weekly one. When you have no room
  for an egg you get 5 jars instead.

- **Days played**

  The first catch of each UTC day pays 2 Bronze jars and adds a day to your days played. The count only goes up:
  missing a day costs nothing. Every 7th day played pays an Uncommon egg. The Team tab shows the day you are on
  and how many days are left to the next egg. The plugin goes by your computer's clock, so changing the clock would
  cheat; nothing stops that in a single-player side game.

### Creature Log milestones

- **Rewards for a growing Log**

  Catching new species pays rewards once each, at 25, 50, 100, 250 and 500 species: 5 Iron jars; 5 Rune jars and a
  Rare egg; 5 Dragon jars and an Epic egg; the **Gilded charm**; then 10 Dragon jars and a Legendary egg. The charm
  is not an item: from then on gilded creatures turn up twice as often (1 in 250 instead of 1 in 500), in the wild
  and from eggs. Completing a family, such as every goblin or every dragon, pays 3 jars of the family's specialty
  type and an egg of the rarest creature in it. Milestones you had already reached are paid when you log in. When
  your egg bag is full an egg reward waits until you have a free slot.

- **Progress on the Log tab**

  Under the Log header the next goal shows with a bar (`Next: 100 species`, `87 / 100`). The **Milestones...**
  button lists every milestone and family with its bar, its reward and a tick once it is paid.

- **Quest-only monsters**

  A monster that only exists during a quest (a quest boss, a cutscene fight) never turns wild, never hatches from an
  egg, is never featured or evolved into, and is not in the Creature Log total or in a family, so every species you
  are asked to catch can be met by a player who has finished every quest. A creature of such a species that you
  already own stays yours and is listed in the Log as quest only, outside the total. Milestones already paid stay paid.

### City Champions

- **Eight cities, eight champions**

  From Lumbridge to Yanille, each city has a champion with a team of three. Stand in the city, open the Battle
  tab and challenge them with your whole team. Your first win earns the city's sigil and a stack of better jars.

  With **Battle animations** on, the battle is acted out beside you: the champion's first creature fades in about 3
  tiles away, your lead walks up to it, and they fight with the same attacks, arrows and breath as a wild battle.
  Three hits of each duel are shown (the first, one from the middle and the last) with their lines floating over the
  creature that was hit. The loser lies down for a couple of seconds and fades out, and the next creature of its team
  steps in; your lead is drawn as whichever of your creatures is fighting, and walks back to you afterwards. A
  knock-out line floats over you as it happens, and the verdict and any rewards float up once the scene is over. A
  battle of three creatures against three takes about 45 seconds at most. Creatures are drawn small, like your
  follower. The result is decided before the scene starts, so nothing in it can change the outcome. With the setting
  off, or when there is no room or no follower on screen, the battle is text only.

### Elite challenge

- **For Grand Champions to be**

  Once you hold all eight City Champion sigils, the Battle tab shows an **Elite challenge** card under the
  champions. Go to the Champions' Guild south-west of Varrock (the ground in front of its door counts, so you do not
  need the quest points the real guild asks for) and press **Challenge**. You fight four Elite trainers with five
  creatures each at levels 70 to 85, then the Grand Champion with six at levels 85 to 99, all Epic or Legendary
  creatures, back to back with your whole team.

- **No resting**

  Your creatures' health carries from one battle to the next and nothing rests until the run ends. Between battles the
  card shows `Elite 3 of 5` with **Continue** and **Forfeit**. While a run is open you cannot change your team or
  battle anything else. Lose a battle, forfeit or log out and the run is over; the next attempt starts at the first
  trainer. You cannot start with a full egg bag (`Free an egg slot first`). The battles are acted out like a City
  Champion battle. With **Choose battle actions** off they are decided automatically.

- **Choosing your turns**

  With **Choose battle actions** on, every Elite battle waits for you each turn, as a City Champion battle does:
  Attack, Switch or use an item such as a salve. There is no catching. **Run**, walking far away or logging out
  during a battle loses it, and the run is over. While a battle waits, **Continue** and healing items on the cards
  are blocked (`Finish the battle first`); **Forfeit** gives the run up.

- **Rewards**

  The first clear earns the title **Grand Champion** (shown in the panel header), 10 Dragon jars, 3 of every
  specialty jar and a Legendary egg. Clearing it again pays 2 Dragon jars. Beating Elite trainers pays creature XP,
  half as much once you have cleared the challenge.

### Friends

- **Battle a friend's team**

  Join a RuneLite party with a friend, using RuneLite's Party plugin, and everyone in it who plays Gielinor Go
  shows up on your Battle tab. Challenge one and your two teams fight it out at once, with the result floating
  over both your heads. Friend battles are for fun: both teams fight at full health and nothing is won or lost.
  With **Battle animations** on, you each see the battle acted out beside you, the same way as a champion battle,
  drawn from the teams you both already showed each other: nothing extra is sent. A battle a friend starts with you
  is only acted out when you are not busy fighting or talking to something, so it never takes over a boss fight; you
  then get the text only. A friend's gilded creature is drawn plain.
  Both players need the same version of the plugin (moves changed the battle rules, so a build from before moves
  cannot battle one with them): a battle with a player on another version is turned away with a line saying so.

- **Trade creatures and eggs**

  Offer a friend one of your creatures or eggs, for one of theirs or as a gift. They see the offer on your card
  and choose Accept or Decline, and nothing changes hands until they accept. A traded creature keeps its level,
  and an egg keeps the walking already done. Rolls and temperament travel with it, and the offer shows the rating. Jars cannot be traded. Promised creatures and eggs cannot be released or hatched until the trade ends. There is no server to referee, so a trade
  can complete on one side only: if either player leaves the party, logs out or changes world in the middle of a
  trade, one creature can end up with both of you while the other one is lost.

- **Only your party hears about it**

  The only things the plugin ever sends are your team (species, levels, rolls and temperaments), your trainer level, the rarities of
  the eggs you carry and whatever you put in a trade (never nicknames), and only to the people in your own party. You can turn
  battle challenges off in the settings.

### Sounds

- **Game sounds**

  A short sound from the game itself plays for a catch, a creature breaking free, a creature gaining a level, an
  evolution, an egg hatching, a gilded creature appearing, a sigil and a Creature Log milestone. No sound file is
  added: the plugin asks the game to play one of its own sound effects, so the game's sound effect volume applies
  and muting the game mutes them. Only one sound plays at a time (a second one within 0.6 seconds is dropped),
  except the gilded creature's sound, which always plays. Battle hits, jar finds and task progress are silent, and
  so is everything while the plugin is hidden in PvP areas. Switch them off with **Sounds** in the settings.

### Side panel

- **Team, Storage, Creature Log and Battle**

  The side panel shows your team with portraits, health and XP bars, the eggs you are carrying, your daily and weekly tasks, the
  creatures you have stored, and a Creature Log of every species you can meet in the game (1,094) so you can see what
  you have caught, what you have only seen, and how rare the missing ones are. The Battle tab lists the friends and City Champions
  you can challenge. The top of the Log tab shows what is featured today and which event is on.

  You can move a creature between your team and Storage only inside a city (any City Champion's area) or within
  10 tiles of a banker, a bank booth or a bank chest. Anywhere else the panel says "Go to a city or a bank to change
  your team". Making another team member the lead is allowed anywhere, and so is a catch that goes to Storage because
  your team is full, a creature you receive in a trade, a hatched egg and releasing a stored creature.

## Links

- [Report a bug](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/new?template=feature_request.yml)
- [Buy me a coffee](https://buymeacoffee.com/oveduumnakal)
