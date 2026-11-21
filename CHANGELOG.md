# Changelog

## 1.0.1

- Renamed to "Quark's Emotes" (controls menu, emote wheel and mod list). Nothing else changed.

## 1.0.0 — release preparation

- Refresh the entire final animator delivery: 47/47 runtime files match bytes
  and MD5, including Floss v2 and Sword Salute v2; skip archive/rig/tools.
- Apply pre/post, duplicate parsed-time and step jumps exactly on time,
  including partial ticks; count ticks without accumulated 0.05-second drift.
  Clamp Catmull–Rom tangents at discontinuities to avoid crossing a jump.
- Accept scalar/vector scale and skip prop cube submission when any scale
  magnitude is below 1e-4, including hidden prop ancestors.
- Use arm pivot 22 for Sword Salute on Alex to match its root sword pivot;
  preserve all other v5 pivot policies. Ship the provisional sword PNG/geo.
- Add Steve/Alex sword front, side, ¾ and pre-pop captures with sampled and
  renderer prop-visibility checks. Keep quick mode and add a release checklist.

- Ship exactly **42 emotes** (41 Blockato emotes + Stretch):
  `wave`, `thinking`, `clap`, `tableflip`, `facepalm`, `groove`, `bigdance`, `cheer`, `bow`, `salute`, `laugh`, `shrug`, `point`, `dab`, `sit_full`, `stretch`, `yes`, `no`, `cry`, `rage`, `victory`, `heart_hands`, `wave_big`, `backflip`, `spin`, `worm`, `floss`, `tpose`, `zombie_walk`, `slow_clap`, `sleep`, `sit`, `jump_hype`, `fishing_cast`, `sword_salute`, `guitar`, `drums`, `pushups`, `default_dance`, `kneel`, `lay_down`, `meditate`.
- Ship 41 byte-identical Blockbench exports by **Blockato** across batches 1–4;
  stretch is the remaining original. Re-copy the renamed sit_full delivery.
  Sit now sits down and holds; sleep lies down and holds until cancel/movement.
- Preserve raw unwrapped Euler components through parsing, every interpolation
  mode, model application and entry/exit blends, including backflip −360° X
  and spin +360° Y. Test the shipped midpoint and full-turn values.
- Add sidecar `arm_pivot: steve` for backflip, spin, worm and sleep: blend slim
  arm pivots from Java Y=2.5 to Y=2 during playback and restore on exit.
- Capture front/¾ shots for all emotes, Steve/Alex variants for backflip/worm/floss,
  side views for backflip/worm, and spin near 90°/270°. Check rendered full turns,
  held sit/sleep, forced skin models and first-person sleep/camera restoration.
- Add category filtering alongside search, six wheel pages and sensible favorites.
- Enforce the same strict folder rule for built-ins, packs, config and importer:
  only the canonical folder animation, optional enter/loop files, matching prop
  pairs and sidecar. Ignore `_archive` and all extra proof/version files.
- Chain enter → main and hold → loop without blending at the handoff; keep one
  network ID and resume segment/phase from total elapsed time for late viewers.
- Follow explicit player-bone prop parents using the flat limb's final animated
  pivot, rotation, position and scale before the prop's own channels.
- Add `-Pquick=true` (42 front views + four UI shots) and chain switch logs.
  Full harness: 115 shots / 103 checks; quick: 46 shots / 51 checks.
- Disable the proof server watchdog with `max-tick-time=-1`; proof timeouts
  default to 3× and accept `MP_TIMEOUT_SCALE` for heavily loaded boxes.
- Set Steve arm pivots for lay_down/pushups; kneel/meditate retain model pivots.
- Keep importer `--include` for a catalog pinned to the requested delivery.
- Wrap Catmull–Rom neighbours across all eight looping emote seams like
  Blockbench 5.2.2. Keep clamped endpoints for non-looping clips.
- Hold `hold_on_last_frame` animations until cancellation; freeze cancelled
  poses and blend back to vanilla over 0.2 seconds. Natural one-shot endings
  use the same fade. Existing play/stop broadcasts also cover held emotes.
- Refresh names, metadata, default wheel/favorites and representative harness
  poses for batch 4, including seated sit_full and mid-beat dances.
- Import new/changed animator folders with `python3 tools/import_blockato.py
  --apply`, keeping sidecars, creating defaults and validating runtime assets.
  Re-copy selected runtime files and back up replacements, including removed props.
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
