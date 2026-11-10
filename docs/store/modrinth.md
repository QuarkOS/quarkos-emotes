# QuarkOS Emotes

Wave hello, react with a facepalm or celebrate with a dance. QuarkOS Emotes
is an editable Minecraft player-emote project by **Emilio Schwaiger / QuarkOS**.

**Release draft:** targets Minecraft **1.21.1 and 1.21.11**, **Fabric and Forge**,
with Java **21+**. The parent verified the builds before the latest animator
integration; another build and visual/multiplayer review are required before
publication. This draft does not announce a published download.

## Forty-two built-in emotes

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

The wheel offers categories, paging, search and favorites, with settings for sound,
movement cancellation and first-person behavior. The flat player rig uses classic/slim pivots, with a temporary Steve arm-pivot
option for whole-body orbit emotes. In-game appearance still needs review.

## Editable content

Use numeric Bedrock/Blockbench `.animation.json` files in resource packs or
`config/quarkos_emotes/emotes/`. Flat and folder layouts are supported.
A folder loads only `<folder>.animation.json`, optional `<folder>_enter.animation.json`
and `<folder>_loop.animation.json`, prop pairs and `<folder>.quarkos.json`. Folders
starting with `_` and all proof/authoring files are ignored. Props may parent
to a player bone or stay at the player root. Folder props use `.geo.json` plus matching `.png` files; metadata may live
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
side views, pushups/meditate in their loop segments, held sit/sleep/kneel/lay_down,
first-person sleep, wheel and settings. Test Steve and Alex, prop textures
and floor placement, cancellation and first-person view before selecting images.

**Project:** Emilio Schwaiger / QuarkOS · **Animator:** Blockato (41 emotes, batches 1–4)
· **License:** MIT · **Version:** 1.0.0 (release preparation).
Vanilla item/sound assets come from the user's Minecraft installation.
