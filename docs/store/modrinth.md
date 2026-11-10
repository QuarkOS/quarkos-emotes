# QuarkOS Emotes

Wave hello, react with a facepalm or celebrate with a dance. QuarkOS Emotes
is an editable Minecraft player-emote project by **Emilio Schwaiger / QuarkOS**.

**Release draft:** targets Minecraft **1.21.1 and 1.21.11**, **Fabric and Forge**,
with Java **21+**. The parent verified the builds before the latest animator
integration; another build and visual/multiplayer review are required before
publication. This draft does not announce a published download.

## Thirty-two built-in emotes

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
remaining original emote. See the repository credits.

The wheel offers categories, paging, search and favorites, with settings for sound,
movement cancellation and first-person behavior. The flat player rig uses classic/slim pivots, with a temporary Steve arm-pivot
option for whole-body orbit emotes. In-game appearance still needs review.

## Editable content

Use numeric Bedrock/Blockbench `.animation.json` files in resource packs or
`config/quarkos_emotes/emotes/`. Flat and folder layouts are supported.
Folder props use `.geo.json` plus matching `.png` files; metadata may live
in `<name>.quarkos.json`, preserving the exported animation. The repository
includes a format guide, an offline validator and an importer with preview
and replacement backups. Import new/changed animator folders with
`python3 tools/import_blockato.py --apply`; sidecars are preserved. Use
`--include` to pin a batch when later deliveries coexist (see README).

## Compatibility and shared playback

Use the artifact matching your exact game version and loader. Environment:
**client required, server optional**. Local emotes work on the client; shared
playback needs compatible server synchronization and a shared animation set.
Client installation alone does not make vanilla-server viewers see emotes.
Final dependency and multiplayer instructions follow release verification.

## Screenshots pending

Capture every emote front/¾, Steve/Alex backflip/worm/floss, backflip/worm
side views, held sit/sleep, first-person sleep, wheel and settings. Test Steve and Alex, prop textures
and floor placement, cancellation and first-person view before selecting images.

**Project:** Emilio Schwaiger / QuarkOS · **Animator:** Blockato (31 emotes, batches 1–3)
· **License:** MIT · **Version:** 1.0.0 (release preparation).
Vanilla item/sound assets come from the user's Minecraft installation.
