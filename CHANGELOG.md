# Changelog

## 1.0.0 — release preparation

- Ship 16 emotes: wave, thinking, clap, tableflip, facepalm, groove, bigdance,
  cheer, bow, salute, laugh, shrug, point, dab, sit_full and stretch.
- Ship fifteen byte-identical Blockbench exports by **Blockato** across batches
  1 and 2; stretch is the remaining original. Replace the earlier flat drafts,
  including dance_groove/dance_elaborate. The delivered four-second one-shot
  sit is named `sit_full`; `sit` stays free for a future hold animation.
- Wrap Catmull–Rom neighbours across looping groove/bigdance seams like
  Blockbench 5.2.2. Keep clamped endpoints for non-looping clips.
- Hold `hold_on_last_frame` animations until cancellation; freeze cancelled
  poses and blend back to vanilla over 0.2 seconds. Natural one-shot endings
  use the same fade. Existing play/stop broadcasts also cover held emotes.
- Refresh names, metadata, default wheel/favorites and representative harness
  poses for batch 2, including seated sit_full and mid-beat dances.
- Import new/changed animator folders with `python3 tools/import_blockato.py
  --apply`, keeping sidecars, creating defaults and validating runtime assets.
  Skip unchanged folders and back up replacements, including removed props.
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
