# Changelog

## 1.0.0 — release preparation

- Ship 16 emotes: wave, thinking, tableflip, facepalm, clap, bow, cheer, dab,
  dance_elaborate, dance_groove, laugh, point, salute, shrug, sit and stretch.
- Replace the five initial wave/thinking/table flip/facepalm/clap drafts with
  unchanged one-shot Blockbench exports by **Blockato** for QuarkOS. The
  canonical table ID is now `quarkos_emotes:tableflip`.
- Render the table's Bedrock geometry with its supplied PNG and box UVs,
  bind animation channels by internal geometry bone names, and root props
  in entity space. Props disappear on playback completion/cancellation.
- Apply flat sibling transforms additively to vanilla poses, preserving
  classic/slim arm pivots and matching Blockbench position/rotation axes.
- Load flat and folder animation layouts, optional metadata sidecars and
  same-folder geometry/PNG assets from resource packs and local config.
- Merge English names into the runtime language file, update wheel ordering
  and harness tableflip captures, and add geometry/animator contract tests.
- Preserve animator files and folder assets in the import tool; validate
  sidecars, geometry, textures and the complete built-in catalog offline.

The parent verified the previous four-target Gradle build and original Java
suite. This integration needs a fresh Gradle run and in-game classic/slim,
prop, reset, first-person and multiplayer review before release.
