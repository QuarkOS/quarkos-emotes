# QuarkOS Emotes

Editable player emotes for Minecraft by **Emilio Schwaiger / QuarkOS**,
with 31 built-in animations by Blockbench animator **Blockato**.

The set contains **32 emotes** (16 retained + 15 batch-3 additions + sit):

`wave`, `thinking`, `clap`, `tableflip`, `facepalm`, `groove`, `bigdance`, `cheer`, `bow`, `salute`, `laugh`, `shrug`, `point`, `dab`, `sit_full`, `stretch`, `yes`, `no`, `cry`, `rage`, `victory`, `heart_hands`, `wave_big`, `backflip`, `spin`, `worm`, `floss`, `tpose`, `zombie_walk`, `slow_clap`, `sleep`, `sit`.

Groove, bigdance, floss and zombie_walk loop with smooth Catmull–Rom seams.
Sit (1.10 s) sits down and holds; sleep (2.20 s) lies down and holds. Movement
or cancellation blends back over 0.2 seconds. Sit_full retains the complete
four-second sit-down/look-around/stand-up sequence; the other emotes play once.
Backflip and spin preserve complete Euler turns. Backflip, spin, worm and sleep
use the Steve arm pivot temporarily on slim models for their baked orbit offsets.
Tableflip includes a textured temporary table bound to its `table` bone.
All 31 Blockato exports and the table geometry/texture are byte-identical
to the animator's files from batches 1–3 (`NOTES_v3.md`). Stretch is the
remaining original emote. See [credits](CREDITS.md).

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

The wheel supports categories, paging, search and favorites; settings include sounds,
movement cancellation and first-person behavior. Review current loader metadata
for dependencies. No third-party animation library is introduced by this set.

## Author or replace emotes

Read [the animation contract](docs/ANIMATION_FORMAT.md). The six player bones
are siblings; transforms add to vanilla poses and use model pivots by default; orbit emotes can opt into Steve arm pivots.
Files use numeric Bedrock animation JSON. Flat `emotes/*.json` or
`*.animation.json` and folder `emotes/<name>/*.animation.json` layouts work
in resource packs and `config/quarkos_emotes/emotes/`. Put metadata in
`<name>.quarkos.json` to keep Blockbench exports untouched. Props load every
same-folder `.geo.json` plus its matching PNG, bind by geo bone name, and
attach to the player root for the duration of the emote.

```sh
python3 tools/validate_emotes.py
python3 tools/import_blockato.py --dry-run
python3 tools/import_blockato.py --apply --include wave,thinking,clap,tableflip,facepalm,groove,bigdance,cheer,bow,salute,laugh,shrug,point,dab,sit_full,yes,no,cry,rage,victory,heart_hands,wave_big,backflip,spin,worm,floss,tpose,zombie_walk,slow_clap,sleep,sit
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer adds new and changed animator folders from `/home/box/blockato/emotes/`
in one command, preserving sidecars and backing up replacements. Unchanged
exports are skipped. The include list pins E8 because the delivery folder also
contains later batches. Omit it to import all delivered names, or use a curated source path.
English emote names are in the runtime `lang/en_us.json`.

## Release checks

The opt-in xvfb harness captures every `emote-<name>.png` from the front and
`emote-<name>-34.png` at a ¾ angle. Backflip and worm add side views; tableflip,
groove and bigdance add back/side views. Backflip, worm and floss each add
`emote-<name>-steve.png` and `emote-<name>-alex.png`, forcing the local player's
wide Steve / slim Alex default skin and model on both Minecraft versions.
Backflip freezes upside down (0.8 s), worm mid-wave (1.25 s), floss mid-swing
(0.25 s), spin near 270° (0.9 s) plus `emote-spin-90.png` (0.63 s), and sit/sleep
on their held seated/lying final poses. The harness table defines every freeze time.
`fp-wave.png` / `fp-sleep.png` show animated arms; their `-camera` variants
check switching to third person and restoration after the 0.2-second cancel fade.
Show-arms mode replaces held items with animated arms; visual offsets never move
the entity, hitbox or first-person camera. Held poses are checked after anchoring;
rendered backflip X / spin Y must exceed 150° in `EMOTE_CHECK` logs.

Pass `-Pharness=true -Pshots=<directory> -PguiScale=3 -Pwidth=1280 -Pheight=720`
to a client task. The requested scale is included in `ui-wheel-s3.png`; Minecraft
may clamp its effective scale to preserve the minimum GUI size. Use distinct shot
directories for each size/scale run. `-Ponly=wave,ui-wheel,ui-wheel-hover,ui-config`
limits captures. Full runs produce 87 shots and 59 checks (including model, hold, turn and camera checks).

The batch 3 screenshots and playback still need the parent's in-game run. Inspect both classic
and slim skins, table UVs and floor placement, and end/cancel reset.
Dedicated-server startup and two-client synchronization also need verification.

[Changelog](CHANGELOG.md) · [License](LICENSE) · [Handoff log](STATUS-codex.md)
