# Credits

**Project author and maintainer:** Emilio Schwaiger / QuarkOS.

The 31 built-in animator emotes (all built-ins except stretch) were authored
in **batches 1–3 by Blockato**, the Blockbench animator, for **QuarkOS**.
The complete 32-emote list is in [README](README.md). Animation exports and the
table's geometry/PNG ship byte-identical; presentation lives in sidecars.
Batch 3 follows the authoritative `NOTES_v3.md` and `md5_delivered.txt`.
`sit_full/sit_full.animation.json` now uses `animation.emote.sit_full` exactly
as delivered. Sit uses the down-and-hold `animation.emote.sit` export: accept
`sit.animation.json` when it is hold-mode, otherwise use `sit_v2.animation.json`.
The older four-second one-shot is never shipped as sit.

**Stretch**, the remaining original emote, and accompanying implementation and
documentation were authored for QuarkOS Emotes with Codex assistance.

The project is provided under the repository's [MIT license](LICENSE),
copyright 2026 Emilio Schwaiger. The build layout was adapted from the sister
QuarkOS Cosmetics project. Blockbench is not bundled and its use does not
imply endorsement.

Minecraft is developed by Mojang Studios. Referenced vanilla items and sounds
resolve to assets supplied by the user's Minecraft installation; those assets
are not redistributed or relicensed here. QuarkOS Emotes is an independent
project, not an official Minecraft product or affiliated with Mojang or Microsoft.
