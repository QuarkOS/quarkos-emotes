# QuarkOS Emotes

Editable player emotes for Minecraft by **Emilio Schwaiger / QuarkOS**,
with fifteen built-in animations by Blockbench animator **Blockato**.

The set contains **16 emotes**: `wave`, `thinking`, `clap`, `tableflip`,
`facepalm`, `groove`, `bigdance`, `cheer`, `bow`, `salute`, `laugh`, `shrug`,
`point`, `dab`, `sit_full` (Sit Down & Stand Up), and `stretch`.

Groove and bigdance loop with smooth Catmull–Rom seams. The other fourteen
play once and blend back to the default pose over 0.2 seconds. `sit_full` is
the animator's complete four-second sit-down/look-around/stand-up sequence;
the `sit` ID is free for a future hold animation. Custom animations can use
`"loop": "hold_on_last_frame"` to hold until movement or cancellation.
Tableflip includes a textured temporary table bound to its `table` bone.
All fifteen Blockato exports and the table geometry/texture are byte-identical
to the animator's files. Stretch is the remaining original emote.
See [credits](CREDITS.md).

## Targets and status

Minecraft **1.21.1 and 1.21.11**, on **Fabric and Forge**, using Java **21+**.
Use the artifact matching both your loader and exact game version.
The parent verified the four Gradle builds and original 14 Java tests before
this animator integration. New contract changes require another Gradle pass;
visual and multiplayer verification remain release checks. This repository
is release preparation, not a claim of published downloads.

Client installation provides local playback. A compatible server component
and shared animation set are needed for synchronized playback. Installing the
client alone does not make other players on vanilla servers see the animation.

The wheel supports paging, search and favorites; settings include sounds,
movement cancellation and first-person behavior. Review current loader metadata
for dependencies. No third-party animation library is introduced by this set.

## Author or replace emotes

Read [the animation contract](docs/ANIMATION_FORMAT.md). The six player bones
are siblings; transforms add to vanilla poses and preserve Steve/Alex pivots.
Files use numeric Bedrock animation JSON. Flat `emotes/*.json` or
`*.animation.json` and folder `emotes/<name>/*.animation.json` layouts work
in resource packs and `config/quarkos_emotes/emotes/`. Put metadata in
`<name>.quarkos.json` to keep Blockbench exports untouched. Props load every
same-folder `.geo.json` plus its matching PNG, bind by geo bone name, and
attach to the player root for the duration of the emote.

```sh
python3 tools/validate_emotes.py
python3 tools/import_blockato.py --dry-run
python3 tools/import_blockato.py --apply
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer adds new and changed animator folders from `/home/box/blockato/emotes/`
in one command, preserving sidecars and backing up replacements. Unchanged
exports are skipped. Use a source path to import a curated subset.
English emote names are in the runtime `lang/en_us.json`.

## Release checks

The opt-in xvfb harness captures every `emote-<name>.png` from the front and
`emote-<name>-34.png` at a ¾ angle, plus back/side views for tableflip and dances.
It uses representative authored poses, closer framing, and handles first-launch
accessibility onboarding automatically. `fp-wave.png` shows animated first-person
arms; `fp-wave-camera.png` exercises the automatic third-person camera switch.
Show-arms mode replaces held items with the animated arms while an emote plays.

Pass `-Pharness=true -Pshots=<directory> -PguiScale=3 -Pwidth=1280 -Pheight=720`
to a client task. The requested scale is included in `ui-wheel-s3.png`; Minecraft
may clamp its effective scale to preserve the minimum GUI size. Use distinct shot
directories for each size/scale run. `-Ponly=wave,ui-wheel,ui-wheel-hover,ui-config`
limits captures. Full runs now produce 44 shots and 18 render checks.

The batch 2 screenshots and playback still need the parent's in-game run. Inspect both classic
and slim skins, table UVs and floor placement, and end/cancel reset.
Dedicated-server startup and two-client synchronization also need verification.

[Changelog](CHANGELOG.md) · [License](LICENSE) · [Handoff log](STATUS-codex.md)
