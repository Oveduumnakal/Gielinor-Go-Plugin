# UI Lab

A page in your browser that draws the plugin's interface with the plugin's own painters, and lets you change its
numbers and colours while you look.

## Run it

From the repo root:

```
python tools/ui-lab/ui_lab.py
```

It builds (about half a minute), then opens `http://127.0.0.1:8917/`. Stop it with Ctrl+C. Run it again after you
change the plugin's code. It needs Python and a JDK 11 or newer (`JAVA_HOME` is used when set).

## Use it

- **Scene** picks what to look at; **Theme** and **Scale** how it is drawn. **Zoom** only enlarges the picture.
- The left side lists the controls. Search narrows them: try `glass`, `radius`, `open` or `hp`. Hover a name for its
  description and where it lives.
  - **NEW: pane heights on this page** sets how high each pane of the shown page sits (Summary, Party, Bag,
    Collection, Tasks). A raised pane casts a shadow on the page and on lower panes beside it; below zero it sinks.
    **Layer gap** spreads all the heights further apart.
  - **NEW: depth and light** has effects the plugin does not have yet: extrusion, bevel and shadow for panels, rows,
    buttons and tabs, chips and bubbles, lit from one angle. All start at zero.
  - **Theme colours** are the colour table of the chosen theme.
  - Each file has its named values. **(inside drawing code)** groups hold numbers and colours written straight into
    the painting code, labelled with the method, line and the code around them.
- The window pages show a made-up save from `java/LabData.java`; edit it to see other cases.
- **Pause**, **Speed** and the slider under them control time. **Restart** starts the scene over.
- The picture takes hover and clicks: tabs, rows and bubbles answer.
- **Edit layout** puts a box over every pane and button of the window pages. Drag a
  box to move it; drag a yellow handle to resize it. Arrows move the picked box one unit and Shift+arrows resize it.
  Boxes snap to each other's edges (hold Alt to stop that). What is inside a pane moves with it. Changes are saved in
  presets, in layout units.
  - Set **Move** to **Things inside them** for health and XP bars, text, icons, chips, hearts and portraits. Each is
    known by the line of code that draws it, so every copy moves together (all six health bars). This also works on
    Storage, Battle, the battle controls, the pinned party box and the info card. It changes only what is drawn:
    clickable areas stay put, so move buttons as **Panes and buttons**. Resizing text changes its room; **Text
    size − +** (or the - and + keys) changes the size of the letters, for text, names, button and chip labels. The
    game's font is made for its own sizes, so other sizes look softer.
- **Hold: original** shows the scene as the code has it, for comparison.
- Changes are **saved as you go** (in `tools/ui-lab/.autosave.json`, never committed) and are there next time.
- **Undo** (Ctrl+Z) and **Redo** (Ctrl+Y) step through them. **My changes** lists each one with its own reset.
- **Save to project** writes a named preset into `tools/ui-lab/presets/`, where `./gradlew build` keeps an eye on it.
- **Export preset** saves what you changed as `ui-preset.json`: each value with its file, line, old and new value.
  Hand that file over to have it applied to the code. **Load preset** brings it back later.

## What it does to the code

Nothing under `src/` is written to. The sources are copied to `build/ui-lab`. In the copy, the UI classes' named
values stop being `final`, numbers written into drawing code move into generated fields (`literals.py`), and the
painters call the depth effects (`hooks.py`, `java/LabDepth.java`). That copy is compiled with the server in `java/`.
The folder is not part of the plugin build. If a painter changes shape, the build stops and says which hook to update.

The server listens on this computer only and refuses requests from other sites.

## The build check

`./gradlew build` runs `tools/ui-lab/check.py` (task `checkUiLab`). It fails when:

- the lab no longer builds against the plugin, for example because a painter changed shape (`hooks.py` says which);
- `controls.snapshot` no longer lists the values, theme colours, panes and buttons the code has. If you meant the
  change, run `python tools/ui-lab/check.py --update` and commit the snapshot with it;
- a preset in `presets/` points at something that is gone, or was made from a value the code no longer has. Update or
  delete the preset.

## What it cannot show

- Portraits are pictures from the OSRS Wiki, not the plugin's own renders from the game's models. They are downloaded
  once into `.portraits/` (never committed; `--no-download` skips it). Put your own `<name>.png` there to replace one.
- Creature animation in the world needs the game. The damage, name tag and catch scenes use the plugin's own drawing
  lines at fixed spots, over pictures standing in for creatures.
- Whole numbers written into drawing code, and the plain 0 and 1, have no control.
- Resizing a pane does not always rearrange what is inside it: the Moves pills, jar cells and rows of text keep
  their size.
- Marks painted by hand have no box: the egg dots, rarity squares, stars, lead arrow and the Storage grid's health
  strip. The low-health flash does not follow a moved health bar.
- A layout or depth effect you keep has to be built into the plugin; a preset only records the amounts.
- It is a close view, not the game: check what you keep in the client.
