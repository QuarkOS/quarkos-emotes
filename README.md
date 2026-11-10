# QuarkOS Emotes

Editable player emotes for Minecraft by **Emilio Schwaiger / QuarkOS**,
with 41 built-in emotes by Blockbench animator **Blockato**.

The set contains **42 emotes** (32 from E1–E8 + 10 batch-4 additions):

`wave`, `thinking`, `clap`, `tableflip`, `facepalm`, `groove`, `bigdance`, `cheer`, `bow`, `salute`, `laugh`, `shrug`, `point`, `dab`, `sit_full`, `stretch`, `yes`, `no`, `cry`, `rage`, `victory`, `heart_hands`, `wave_big`, `backflip`, `spin`, `worm`, `floss`, `tpose`, `zombie_walk`, `slow_clap`, `sleep`, `sit`, `jump_hype`, `fishing_cast`, `sword_salute`, `guitar`, `drums`, `pushups`, `default_dance`, `kneel`, `lay_down`, `meditate`.

Groove, bigdance, floss, zombie_walk, guitar, drums, pushups and default_dance
loop with smooth Catmull–Rom seams. Pushups plays its 1.3-second stand-to-plank
entry once before the 2-second loop. Sit, sleep, kneel and lay_down hold their
last frame; meditate plays its 5.6-second main clip then loops a 2-second breath.
Chains share one emote ID and resume the correct segment/phase for late viewers.
Handoffs have no blend; entry blends for 0.15 seconds and cancel/natural end
fades over 0.2 seconds. Sit_full retains the complete four-second sequence.
The other 29 emotes play once. Backflip and spin preserve complete Euler turns.
Backflip, spin, worm, sleep, lay_down and pushups temporarily use Steve arm
pivots on slim models. Tableflip includes its textured table; sword_salute now
includes the delivered textured sword with copied right-arm channels.
The 41 Blockato emotes plus two companion chain clips and both prop pairs
are copied byte-for-byte from batches 1–4 (`NOTES_v4.md`, `RENAMES.md`, `md5_delivered.txt`; the arrived sword
prop is included). Floss retains the v4 bytes now in `_archive/floss/`.
Stretch is the remaining original emote.

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
`*.animation.json` and folder `emotes/<name>/<name>.animation.json` layouts work
in resource packs and `config/quarkos_emotes/emotes/`. Put metadata in
`<name>.quarkos.json` to keep Blockbench exports untouched. Props load every
same-folder `.geo.json` plus its matching PNG, bind by geo bone name, and
attach to the player root or an explicit player-bone parent. Optional
`<name>_enter.animation.json` / `<name>_loop.animation.json` are chain segments,
never separate wheel entries. Folders starting with `_` and all extra files
(sit_v1, sit_v2, clipcheck, GIF, bbmodel) are ignored.

```sh
python3 tools/validate_emotes.py
python3 tools/import_blockato.py --dry-run
python3 tools/import_blockato.py --apply --include jump_hype,fishing_cast,sword_salute,guitar,drums,pushups,default_dance,kneel,lay_down,meditate,sit
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer adds new and changed animator folders from `/home/box/blockato/emotes/`
in one command, preserving sidecars and backing up replacements. Unchanged
exports are skipped. The include list pins the E9 additions and canonical sit refresh; the other
32 emotes remain from E8. The current intake also contains a v5 floss revision;
this v4 run retains the E8 bytes now archived in `_archive/floss/`. Omit it to import all delivered names, or use a curated source path.
English emote names are in the runtime `lang/en_us.json`.

## Release checks

The opt-in xvfb harness captures every `emote-<name>.png` from the front and
`emote-<name>-34.png` at a ¾ angle. Backflip and worm add side views; tableflip,
groove and bigdance add back/side views. Backflip, worm and floss each add
`emote-<name>-steve.png` and `emote-<name>-alex.png`, forcing the local player's
wide Steve / slim Alex default skin and model on both Minecraft versions.
Backflip freezes upside down (0.8 s), worm mid-wave (1.25 s), floss mid-swing
(0.25 s), spin near 270° (0.9 s) plus `emote-spin-90.png` (0.63 s), and sit/sleep
on their held seated/lying final poses. New kneel/lay_down shots hold their
final pose; pushups freezes at total elapsed 1.8 s and meditate at 6.6 s. The harness table defines every freeze time.
`fp-wave.png` / `fp-sleep.png` show animated arms; their `-camera` variants
check switching to third person and restoration after the 0.2-second cancel fade.
Show-arms mode replaces held items with animated arms; visual offsets never move
the entity, hitbox or first-person camera. Held poses are checked after anchoring;
rendered backflip X / spin Y must exceed 150° in `EMOTE_CHECK` logs.

Pass `-Pharness=true -Pshots=<directory> -PguiScale=3 -Pwidth=1280 -Pheight=720`
to a client task. The requested scale is included in `ui-wheel-s3.png`; Minecraft
may clamp its effective scale to preserve the minimum GUI size. Use distinct shot
directories for each size/scale run. `-Ponly=wave,ui-wheel,ui-wheel-hover,ui-config`
limits captures. Full runs produce 107 shots and 77 checks. `-Pquick=true` captures only
the 42 front views and four UI shots (46 shots / 50 checks). Pushups and meditate
wait for a real segment switch before freezing in their loop; require
`EMOTE_CHECK chain=... result=PASS` and a zero-failure `EMOTE_SUMMARY`.

The batch 4 screenshots and playback still need the parent's in-game run. Inspect both classic
and slim skins, table UVs and floor placement, and end/cancel reset.
Dedicated-server startup and two-client synchronization also need verification.

[Changelog](CHANGELOG.md) · [License](LICENSE) · [Handoff log](STATUS-codex.md)
