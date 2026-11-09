
## E1 — core implementation (2026-11-09)

Implemented the code in the shared `dev.quarkos.emotes` source tree:

- Pure Java/Gson Bedrock format model/importer: multiple animations, aliases,
  constants/key maps, pre/post keys, linear/step/Catmull–Rom interpolation,
  easing, loop/hold/once, Molang fallback warnings, sounds and quarkos metadata.
- Ordered override registry; client resource reload listeners on both loaders;
  sorted `config/quarkos_emotes/emotes/*.json` custom imports after resource packs.
- Per-player tick/partial-tick playback, 0.15-second blending, movement/jump/
  attack/hurt cancellation, camera restore, first-person arm hooks, PlayerModel
  pose hooks and outer-layer handling for both Minecraft APIs.
- Optional Fabric payload and Forge SimpleChannel sharing, bounded ids, separate
  rate limits for play/stop, server-supplied player UUIDs, viewer broadcasts,
  absent-channel local fallback and one-time visibility hint. Dedicated-server
  bootstrap paths avoid client initialization.
- Eight-sector wheel with paging/search/hover/numbers/hold/click modes, eight
  editable favorites and four initially unbound favorite keys. Config UI and
  atomic JSON persistence, including a selected-emote inventory-style preview.
- Prop item layer plus basic cube lists / Bedrock geometry cube extraction.
- Opt-in creative-flat-world harness, deterministic 40% pose freeze, front/back/
  side/world/UI/first-person screenshots and checks on actual rendered bone
  changes (EMOTE_CHECK / EMOTE_SUMMARY).
- Loader metadata, English UI translations, a generated 128px placeholder icon,
  ONE wave placeholder animation and `docs/DESIGN.md`.
- Removed GeckoLib/Ears/Jackson/gallery dependencies and stale screenshot paths.
  Kept optional compile-only Mod Menu and the shared Forge Stonecutter branch.
  Both build scripts use resource format 75.0 for 1.21.11 (cached version.json
  confirms 94.1 is the **data** format), resource format 34 for 1.21.1.

Verified locally without Gradle or network:

- Read the sister cosmetics implementation without modifying it; inspected real
  named Minecraft and Forge cached jars/sources with javap/zipfile.
- `javac --release 21 -proc:none` using JDK 25 and cached named dependencies
  succeeds for Fabric 1.21.11, Fabric 1.21.1, Forge 61.2.4 and Forge 52.1.20.
  A small temporary Python preprocessor selects the same branches/replacements;
  this is NOT a verification of Stonecutter generation or remapped artifacts.
- 14 JUnit 5 tests pass, compiled with --release 21 and run using cached JUnit
  5.11.4 / Gson 2.11.0; the tested core imports no Minecraft classes. Tests cover
  parsing/errors/Molang/metadata/sounds/aliases, interpolation/easing/bounds,
  looping/playback/blends/sound crossings, the fixed arm-axis convention,
  registry replacement and network request validation/rate limits.
- `git diff --check` passes. No Gradle commands, network access, pushes, remotes,
  publishing or edits to the cosmetics repository.

Unverified / TODO for the parent run:

1. Run all four REAL Gradle builds and Fabric unit-test tasks. Manual javac used
   disabled annotation processing, so loader startup, Mixin injection/remapping,
   Stonecutter handling and final jar resource expansion still need validation.
2. Run the xvfb harness on the four targets and review emote axes, idle-vs-pose
   checks, first-person wave, outer layers, screenshots, and GUI scales 1–4 /
   854x480–2560x1440. World creation and GPU readback follow cosmetics' pattern
   but no client was launched in this session.
3. Merge the content session's 16 emotes and its translations/docs; the wave file
   here is intentionally only a placeholder. Check every content emote at 40%.
4. Exercise two real clients plus dedicated-server startup on each loader, vanilla
   server fallback, mixed mod/vanilla clients, rapid cancellation and reconnects.
   Channel absence APIs and optional flags compile but were not tested live.
5. Props currently render cube geometry using block-item surfaces (oak planks by
   default / optional per-cube item). Bedrock texture UVs, nested geometry bone
   transforms and custom materials are not supported. Check table_flip's prop
   against the content session's concrete schema and desired appearance.
6. Already-running remote emotes are not replayed when a new viewer starts
   tracking or joins; they receive the next play/stop action. Add state snapshots
   if persistence across tracking boundaries is required.
7. The fixed content convention is X=-90 => forward (-Z) for a resting arm. Tests
   explicitly check both X=-90 and X=+90. Generic Bedrock X-sign-flip recipes
   conflict with that stated bind-space convention; verify with content previews.
8. Forge bootstrap/config registration and Fabric's older reload interface emit
   deprecation warnings; use the compiling compatibility paths until runtime
   validation warrants loader-specific migration.

Commit handoff: the workspace filesystem mounts `.git` read-only. The requested
`git add`/commit failed with `Unable to create .git/index.lock: Read-only file
system`; no permission escalation is available. A copied Git directory at
`/tmp/quarkos-emotes-e1.git` is used to create the local commit on its copied
`main`, with the requested author. `build/E1-core.patch` and
`build/E1-core.bundle` preserve the commit for the parent. The primary workspace
HEAD cannot be advanced here; the parent must import the commit or commit the
existing working files. These handoff artifacts are ignored build outputs.

## E4 — Blockato animator contract integration (2026-11-09)

Implemented in the shared source tree; no commits or writes to `.git`. Kept
`forge/build.gradle.kts` and the parent's JUnit/syncBranchSources ordering fixes
unchanged. No Gradle invocation or client launch was performed in this run.

Changes:

- Shipped byte-identical copies of Blockato's `wave`, `thinking`, `tableflip`,
  `facepalm`, and `clap` exports in `emotes/<name>/<name>.animation.json`.
  All are one-shot, with lengths 2 / 3 / 2.4 / 2.4 / 2.4 seconds and original
  `animation.emote.<name>` keys. Copied table_prop.geo.json and table_prop.png
  unchanged. Only runtime files are bundled; no rigs/projects/previews/reports.
  Added five metadata sidecars and removed the five E2 flat replacements.
  Verified the other eleven E2 animation files against HEAD byte-for-byte.
  There are exactly 16 animations and no duplicate IDs.
- Added shared flat/folder filename discovery and filename-derived IDs; resource
  packs and recursive config-folder animations support matching sidecars and
  every same-folder geo/PNG pair. Fixed reading document-level E2 metadata as
  well as animation-level legacy metadata; inline metadata takes precedence
  over sidecars. Missing metadata gives a title-cased filename-based name.
- Added pure-Java Bedrock geometry records/parser for cubes, box UVs, pivots,
  rotations, mirror/inflate and geometry parent validation. Geometry binds by
  its exact internal bone name (including custom case), never the filename.
- Added textured geometry rendering via ModelPart.Cube with entityCutout,
  entity lighting/hurt overlay, feet origin and root/body-yaw attachment.
  Resource PNGs load normally; local config PNGs register dynamic textures and
  are released/reloaded on reload. Props hide immediately when natural stop
  or cancellation starts; player poses retain their normal 0.15 s blend-out.
  Prop transforms follow the full authored trajectory instead of blending an
  inverted table back through the floor. Geometry supersedes stale legacy
  inline prop rendering to avoid drawing a second table.
- Player poses now add rotation and pixel offsets to each model's own vanilla
  pose/default pivots. The six parts remain independent siblings; slim arm
  Y=2.5 (Bedrock 21.5) is preserved instead of forcing Steve Y=2 (22).
- Corrected Bedrock-to-Java position to [x,-y,z] and rotation to [+x,+y,+z]
  degrees-to-radians. Evidence: animator NOTES.md, tools/make_emotes.py and
  clipcheck.py's measured BB transform (BB Euler [-x,-y,+z], then reverse BB
  X/Y into Java model space). In particular wave Z=+148 raises the right arm
  outward and position X=-1.5 moves its shoulder outward. E1's old Y/Z rotation
  and X-position expectations were corrected, retaining both +/-90 forward/
  backward assertions and adding an authored wave regression. No assertions
  were removed to bypass a failure. This changes playback of the retained E2
  files too, so re-review all sixteen visually against these correct axes.
- Merged English names into runtime en_us.json and removed en_us.emotes.json.
  Updated wheel order (the five Blockato gestures first), harness tableflip
  front/back/side IDs, format docs, credits, README/changelogs and store drafts.
  Canonical table ID/capture is now quarkos_emotes:tableflip / emote-tableflip.
- Updated importer to preserve keys/bone names and single-animation source
  bytes, split multi-animation documents without renaming keys, copy geo plus
  matching PNG, write/keep metadata sidecars, migrate flat targets to folders,
  and back up all replaced runtime assets with relative paths. Proof PNGs,
  GIFs, bbmodels and clip reports are excluded. Existing ambiguity/duplicate/
  invalid-input and dry-run protections remain covered. Ran real-source
  dry-run only: the animator directory now contains other potential replacements;
  these were NOT applied to the eleven retained E2 emotes.
- Updated validator for flat/folder animations, sidecars, geo box UV/pivot/
  rotation/texture dimensions, bound prop names and the merged English file.

Verified:

- `javac --release 21 -proc:none` with cached named jars and the existing
  `/tmp/check_emotes_compile.py` version/loader preprocessor succeeds for
  Fabric 1.21.1, Fabric 1.21.11, Forge 52.1.20 and Forge 61.2.4. Only existing
  deprecated loader API warnings remain. This does not verify Stonecutter,
  Mixin processing/remapping or final Gradle artifacts.
- 22/22 pure-Java JUnit tests pass using cached JUnit 5.11.4 and Gson 2.11.0
  (`/tmp/e4_test.py`, resources loaded from src/main/resources). Includes all
  prior tests, seven new animator contract tests and the extra wave-axis test.
  Covers resource parsing of all five exports (length/loop/bones/20 fps keys),
  geometry cubes/UV/pivot/rotation, exact bone-name binding, prop lifecycle,
  flat/additive posing and both shoulder baselines, discovery/ID derivation,
  sidecar metadata/sound and inline precedence.
- 20/20 Python tests pass:
  `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v`.
- `python3 tools/validate_emotes.py`: 16 valid emotes, 0 errors.
- Direct byte comparisons with /home/box/blockato/emotes confirm all five
  animations and both prop assets are identical to the animator deliveries.
- `git diff --check` passes. Build scripts and retained E2 animations unchanged.

Unverified / TODO for parent:

1. Run the REAL four-target Gradle build + test tasks again for E4, keeping the
   existing Forge build fixes. Compile checks above use named cached jars only.
2. Run/review E4 xvfb harness captures on all targets (the separate-worktree
   pre-E4 captures do not cover these changes). Use `emote-tableflip`,
   `emote-tableflip-back`, `emote-tableflip-side`; inspect the actual table PNG
   UV orientation, floor/feet placement, root yaw, and prop removal on end,
   movement and explicit cancel. Review Steve AND Alex, all sixteen gestures,
   idle/additive blending, first-person wave, armor/cape/held-item/outer layers.
3. Exercise resource-pack overrides and local folder overrides with sidecars,
   multiple geo+PNG pairs, and F3+T texture reload/cleanup in a live client.
4. Retain the earlier dedicated-server, vanilla-server fallback and two-client
   synchronization/reconnect/tracking checks. No networking changes in E4.
5. Update release screenshots/store evidence using verified E4 captures, then
   have the parent review/stage/commit this worktree. No commit was attempted.
