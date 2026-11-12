<h1 align="center">Gielinor Go</h1>

Gielinor Go is a RuneLite plugin that adds a creature-raising side game to Old School RuneScape. Catch the monsters
you walk past, let your normal XP gains train them, and send them into quick battles, all without stopping what you
were doing.

Everything happens on your own computer. Nothing is sent to the game, and the plugin never clicks or acts for you.
Nobody else can see any of it, unless you join a RuneLite party to battle or trade with a friend.

## Features

### Wild creatures

- **Monsters near you sometimes turn wild**

  A wild monster gets a soft outline and a level tag. Right-click it to see two new options: **Catch** and
  **Battle**. Any monster you can fight can turn wild, from chickens to bosses, and stronger monsters are rarer
  finds.

- **Catch them with jars**

  Throwing a jar has a chance to catch the creature. The menu shows your odds before you click. Stronger monsters
  are harder to catch, and better jars (Bronze, Iron, Rune, Dragon) improve your chance.

- **See the catch happen**

  Your character makes a throwing motion when you throw a jar, and a creature you catch vanishes for five
  seconds before fading back in. Only you see this: the real monster never goes anywhere. A monster that is in a
  fight with anyone is never hidden, and one that starts fighting while hidden comes straight back. You can switch it
  off in the settings.

- **Earn jars by playing**

  You start with 10 Bronze jars. Walking earns more, every level-up in the real game gives three, and battle wins
  can drop better ones as your trainer level grows.

- **Find specialty jars while skilling**

  Training a skill can turn up a jar that is much better on one kind of creature. Mining finds Cave jars for
  creatures you meet underground, Agility finds Swift jars for fast attackers, Prayer finds Blessed jars for
  undead and demons, and so on for almost every skill. When you own one that suits the creature in front of
  you, it shows up as its own **Catch** option with its odds.

- **Everything shows where it happens**

  News about a creature floats over that creature: it appeared, you caught it, it broke free. Things you find or
  earn, such as jars and level-ups, float over your own head.

### Your team

- **Your XP trains your creatures**

  Every XP drop you earn, in any skill, also trains your team of up to six creatures. The creature in the lead
  slot learns twice as fast.

- **Your lead walks with you**

  Your lead creature follows a step behind you as a small copy of itself, so it never gets in the way. A gilded
  lead follows in gold. Only you can see it, it cannot be clicked, and you can switch it off in the settings.

- **Nicknames**

  Right-click a creature in Team or Storage and choose **Rename...** to give it a name of 1 to 12 letters, numbers,
  spaces or hyphens. The nickname shows on its card (the species moves to the second line), in Storage, in battle
  lines and in level-up and evolution lines, and a creature keeps it when it evolves. Leave the box empty to go
  back to the species name. With **Show lead's nickname** on (the default), your lead's nickname floats above it
  as it follows you. Nicknames stay on your computer: friends never see them, and a creature you trade away
  arrives without one.

- **Battles that play themselves**

  Choose **Battle** on a wild creature and your lead fights it right away. Each hit floats over the creature's
  head, and the hits your own creature takes float over yours, so you never have to stop skilling or fighting. Melee beats Ranged, Ranged beats Magic, and Magic beats
  Melee. A creature that loses to you is easier to catch. Dragons and wyverns bite in melee but about one hit in
  four is a fire or ice breath, which counts as Magic for that hit and says so ("Green dragon breathes fire for 12").

  With **Battle animations** on (the default), your lead walks up to the wild creature and they face each other.
  Each hit plays as an attack by one and a flinch by the other, one every 3 game ticks like the floating text, and the
  loser lies down for a few seconds before getting up. Your lead then walks back to you. The wild creature is
  hidden for the battle: within 1 game tick it fades into a solid copy of itself, drawn at 75% size, that stands in
  its place and fights; a faint (90% see-through) ghost of it fades in over 3 game ticks whenever the real monster
  walks off its tile, and at the end the copy fades back into the real monster over 3 game ticks. Only you see any
  of this, and
  the real monster is untouched. A breath plays the creature's breath animation. Creatures with no animation for a
  move lunge or tip over instead. Switch it off in the settings.

  **Catch a knocked-out creature.** When your lead wins, the wild creature lies knocked out for about 10 seconds.
  Right-click the lying copy to get the same **Catch** options as the real monster (you can also do this while it
  fights). A knocked-out creature is twice as easy to catch (x2.0); once it is back up it is x1.5 as before. Caught:
  the catch plays out as usual. Broke free: it gets up and the scene ends. Ran away: the scene ends and it is no
  longer wild.

- **Creatures evolve**

  Many creatures grow into a stronger relative when they reach a certain level: an Imp becomes a Lesser demon,
  then a Greater demon, then a Black demon. There are over 150 evolutions to find.

- **Eggs hatch as you walk**

  Battle wins sometimes drop an egg. Carry up to three, and each hatches into a new creature once you have
  walked far enough. Rarer eggs take longer and hatch rarer creatures.

- **Gilded creatures**

  One wild creature in 500 is gilded: gold outline in the world, gold name in your panel. They are only for
  showing off.

### City Champions

- **Eight cities, eight champions**

  From Lumbridge to Yanille, each city has a champion with a team of three. Stand in the city, open the Battle
  tab and challenge them with your whole team. Your first win earns the city's sigil and a stack of better jars.

### Friends

- **Battle a friend's team**

  Join a RuneLite party with a friend, using RuneLite's Party plugin, and everyone in it who plays Gielinor Go
  shows up on your Battle tab. Challenge one and your two teams fight it out at once, with the result floating
  over both your heads. Friend battles are for fun: both teams fight at full health and nothing is won or lost.

- **Trade creatures and eggs**

  Offer a friend one of your creatures or eggs, for one of theirs or as a gift. They see the offer on your card
  and choose Accept or Decline, and nothing changes hands until they accept. A traded creature keeps its level,
  and an egg keeps the walking already done. Jars cannot be traded. Promised creatures and eggs cannot be released or hatched until the trade ends. There is no server to referee, so a trade
  can complete on one side only: if either player leaves the party, logs out or changes world in the middle of a
  trade, one creature can end up with both of you while the other one is lost.

- **Only your party hears about it**

  The only things the plugin ever sends are your team (species and levels), your trainer level, the rarities of
  the eggs you carry and whatever you put in a trade (never nicknames), and only to the people in your own party. You can turn
  battle challenges off in the settings.

### Side panel

- **Team, Storage, Creature Log and Battle**

  The side panel shows your team with portraits, health and XP bars, the eggs you are carrying, the creatures
  you have stored, and a Creature Log of every species in the game so you can see what you have caught, what
  you have only seen, and how rare the missing ones are. The Battle tab lists the friends and City Champions
  you can challenge.

## Links

- [Report a bug](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/new?template=bug_report.yml)
- [Request a feature](https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/new?template=feature_request.yml)
- [Buy me a coffee](https://buymeacoffee.com/oveduumnakal)
