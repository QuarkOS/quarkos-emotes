# QuarkOS Emotes

Wave hello, react with a facepalm or celebrate with a dance. QuarkOS Emotes
is an editable Minecraft player-emote project by **Emilio Schwaiger / QuarkOS**.

**Release draft:** targets Minecraft **1.21.1 and 1.21.11**, **Fabric and Forge**,
with Java **21+**. The parent verified the builds before the latest animator
integration; another build and visual/multiplayer review are required before
publication. This draft does not announce a published download.

## Sixteen built-in emotes

`wave`, `thinking`, `tableflip`, `facepalm`, `clap`, `bow`, `cheer`, `dab`,
`dance_elaborate` (Spotlight Dance), `dance_groove` (Groove), `laugh`, `point`,
`salute`, `shrug`, `sit`, `stretch`.

**Blockato**, the Blockbench animator, authored wave, thinking, tableflip,
facepalm and clap for QuarkOS. These five are one-shot exports, shipped
unchanged with metadata sidecars. Tableflip includes Blockato's geometry
and PNG texture, animated by its internal `table` bone. The prop follows
the player's root and disappears as soon as playback stops or is cancelled. Cheer, laugh and both dances loop; sit holds until cancelled.

The wheel offers paging, search and favorites, with settings for sound,
movement cancellation and first-person behavior. The flat player rig preserves
classic and slim model pivots. In-game appearance still needs review.

## Editable content

Use numeric Bedrock/Blockbench `.animation.json` files in resource packs or
`config/quarkos_emotes/emotes/`. Flat and folder layouts are supported.
Folder props use `.geo.json` plus matching `.png` files; metadata may live
in `<name>.quarkos.json`, preserving the exported animation. The repository
includes a format guide, an offline validator and an importer with preview
and replacement backups.

## Compatibility and shared playback

Use the artifact matching your exact game version and loader. Environment:
**client required, server optional**. Local emotes work on the client; shared
playback needs compatible server synchronization and a shared animation set.
Client installation alone does not make vanilla-server viewers see emotes.
Final dependency and multiplayer instructions follow release verification.

## Screenshots pending

Capture the gesture set, front/back/side tableflip, dances, sit, wheel and
settings after the animator integration. Test Steve and Alex, prop textures
and floor placement, cancellation and first-person view before selecting images.

**Project:** Emilio Schwaiger / QuarkOS · **Animator:** Blockato (five emotes)
· **License:** MIT · **Version:** 1.0.0 (release preparation).
Vanilla item/sound assets come from the user's Minecraft installation.
