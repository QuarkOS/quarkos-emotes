
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

## E5 — screenshot-driven UI and visual harness pass (2026-11-10)

Reviewed the parent's real pre-E4 Fabric 1.21.11 wheel, hovered wheel, config,
first-person and all sixteen emote screenshots using the image viewer (contact
sheet for the emotes), plus Forge sit/tableflip captures. These images predate
E4's importer/metadata/prop corrections; they are not evidence of E5 rendering.
No Gradle invocation, client launch, commit, or write to `.git` in this session.

Changes:

- Harness disables/saves accessibility onboarding on its first tick and dismisses
  an already-visible AccessibilityOnboardingScreen to TitleScreen, resetting the
  title stage. Works through shared code on both versions/loaders. Timeouts and
  empty capture filters now fail instead of producing a misleading success.
- Verified E4 filename-derived IDs against all 16 actual resources, including
  sidecars, and verified that every display-name key exists in runtime English.
  Filenames are `emote-bow.png`, `emote-tableflip.png`, etc.; no doubled namespace.
- Added a harness-only CameraZoomMixin based on the sister cosmetics approach:
  2.85-block distance, lowered body target, true camera orbit without rotating
  the head. Upright full bodies should fill approximately 65% of frame height
  at FOV 54. Every emote gets `-34`; tableflip/dances retain `-back` and `-side`.
  Tableflip uses .75 s (table rotated 110 degrees and raised 12 pixels); its
  straight views use 3.1 blocks, angled views include a .55-block forward target
  shift to contain the prop. Sit has separate distance/height for visible feet.
  Numeric projection checks found and corrected prop cropping in angled views.
- Replaced blanket 40% freezes with a per-emote seconds table: wave .5, thinking
  1.2, facepalm .65, clap .6 (contact), tableflip .75, bow 1.1, salute .8, laugh
  .65, dab .82, sit 1.4, groove .5, elaborate 3.25, shrug .84, point 1, cheer .75,
  stretch 1.15. Config's newly-created preview is frozen after screen init too.
- Wheel now reserves separate header/search, ring, label, help and footer areas;
  opaque center paging, sector gaps, stronger contrast, consistent vanilla
  buttons/tooltips, clear favorites/all-emotes toggle with count, localized
  labels with readable custom-pack fallback, and an initial selection prompt.
  Search does not initially steal number/F/hold-release shortcuts. Page/favorite
  controls and all text fit a 320x240 logical GUI, with no label inside the ring.
- Config has a separate framed/scissored right preview panel, bounded entity
  scale/rotation, selected-emote caption, title, option tooltips and bottom Done.
  Shorter camera-mode value keeps the smallest option column readable.
- Fixed the empty first-person capture: hideGui also suppresses Minecraft hands,
  so fp-wave keeps the HUD enabled. SHOW_ARMS now renders both arms through a
  stable player-space transform, replacing item-holding transforms/items for
  the duration; existing per-arm/sleeve pose hooks apply authored animation.
  Added an independent animated-hand hook check. fp-wave-camera uses real play
  from FIRST_PERSON and verifies THIRD_PERSON_BACK plus a rendered pose.
- Both build scripts pass -PguiScale=<n> as quarkos_emotes.guiScale; Forge's
  existing options seed respects it. Runtime applies/saves it once and resizes.
  Added ui-wheel-s<n>.png; EMOTE_SHOT logs requested/effective scale, freeze time
  and camera profile. Full capture set is 44 PNGs and 18 render checks.
- Fixed E2 sit entry: delayed the shared drop until the rigid legs clear the
  floor, ending with a 9.7-pixel drop and small clearance. Shared six-bone root
  translations now preserve floor clearance during pose blending too; otherwise
  seated blend-out dipped nearly 2.8 pixels below ground. Independent sibling
  relationships are preserved. E2 bow now compensates its hip pivot and carries
  head/shoulder positions explicitly; the original flat pose separated torso
  from hips by about 7.4 pixels in depth at its peak. All other E2 JSON unchanged.
- All five animator animation files and both table assets remain byte-identical
  to /home/box/blockato/emotes. No author-delivery bytes were edited.

Verified without Gradle:

- Four-target cached-jar javac --release 21 -proc:none succeeds: Fabric 1.21.1 /
  1.21.11 and Forge 52.1.20 / 61.2.4. Existing loader deprecation warnings only.
  Used /tmp/check_emotes_compile.py; this does not run Mixin annotation processing,
  Stonecutter generation, Gradle Kotlin scripts or remapping.
- 25/25 pure-Java JUnit tests pass (/tmp/e4_test.py). Three new tests cover all
  16 actual IDs/translations, bow's hip/neck/shoulder connections and sit floor
  clearance at 240 Hz across 21 blend weights (entry, hold and exit).
- 20/20 Python tests pass; validator reports 16 valid emotes / 0 errors.
- Direct byte comparisons against animator deliveries pass for all seven assets.
- git diff --check passes. Only intended source/resources/docs edits are left;
  temporary audit, compile and test artifacts are under /tmp.

Unverified / TODO for parent:

1. Build all four REAL Gradle targets and run the harness from fresh run dirs
   without a manually supplied options.txt. Confirm onboarding dismissal, Mixin
   injection/remapping (including new camera/hand mixins), and EMOTE_SUMMARY
   pass=18 fail=0 for a full run. This session cannot produce new in-game images.
2. Review all 44 captures, especially tableflip front/34/side, bow side geometry,
   sit feet during entry AND cancellation, fp-wave's visible raised arm and
   fp-wave-camera's switched camera. Hook/projection checks cannot prove pixels.
   Inspect Steve and Alex, armor/outer layers, held-item restoration and camera
   restoration after normal end/cancel. The delivered five gestures rely on E4's
   corrected importer and still need their first post-E4 visual review.
3. Run -Pwidth=854 -Pheight=480 -PguiScale=2, then 3 and 4; also 2560x1440 at
   several scales, using separate -Pshots directories. Read effectiveScale in
   EMOTE_SHOT: Minecraft normally clamps 854x480 to scale 2 even if 3/4 requested.
   Review hover/search, empty favorites, page changes, keyboard navigation,
   translated labels and option tooltips (layout math is not a live UI test).
4. Retain E4's reload/custom-prop and multiplayer/dedicated-server checks. No
   networking changes here. Parent reviews/stages/commits the working files.
