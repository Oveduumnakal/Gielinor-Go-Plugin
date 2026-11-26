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
  - **NEW: depth and light** has effects the plugin does not have yet: extrusion, bevel and shadow for panels, rows,
    buttons and tabs, chips and bubbles, lit from one angle. All start at zero.
  - **Theme colours** are the colour table of the chosen theme.
  - Each file has its named values. **(inside drawing code)** groups hold numbers and colours written straight into
    the painting code, labelled with the method, line and the code around them.
- The window pages show a made-up save from `java/LabData.java`; edit it to see other cases.
- **Pause**, **Speed** and the slider under them control time. **Restart** starts the scene over.
- The picture takes hover and clicks: tabs, rows and bubbles answer.
- **Hold: original** shows the scene as the code has it, for comparison.
- **Export preset** saves what you changed as `ui-preset.json`: each value with its file, line, old and new value.
  Hand that file over to have it applied to the code. **Load preset** brings it back later.

## What it does to the code

Nothing under `src/` is written to. The sources are copied to `build/ui-lab`. In the copy, the UI classes' named
values stop being `final`, numbers written into drawing code move into generated fields (`literals.py`), and the
painters call the depth effects (`hooks.py`, `java/LabDepth.java`). That copy is compiled with the server in `java/`.
The folder is not part of the plugin build. If a painter changes shape, the build stops and says which hook to update.

The server listens on this computer only and refuses requests from other sites.

## What it cannot show

- Creature portraits and in-world animation need the game, so a blue square stands in for a portrait.
- Whole numbers written into drawing code, and the plain 0 and 1, have no control.
- A depth effect you keep has to be built into the plugin; a preset only records the amounts.
- It is a close view, not the game: check what you keep in the client.
