
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

## E6 — animator batch 2, loop seams and hold/exit playback (2026-11-10)

Read `/home/box/blockato/emotes/NOTES_v2.md` as the authoritative animator
contract. No Gradle invocation, harness/client launch, commit, or `.git` write.

Changes:

- Ship groove, bigdance, bow, salute, laugh, shrug, cheer, point, dab and the
  original four-second one-shot sit as `sit_full`. All ten animation payloads
  copy byte-for-byte; sit's filename alone changes, retaining animation.emote.sit
  inside it. Each has an explicit quarkos_emotes ID and presentation sidecar.
  Groove/bigdance loop; all eight other batch 2 clips play once. Removed the
  ten superseded E2 flat files, including both dance IDs and sit. No sit
  placeholder is shipped. Stretch is untouched. Total: fifteen Blockato exports
  across batches 1+2, plus stretch = sixteen built-ins.
- Updated runtime English names, wheel order and initial eight favorites:
  wave, thinking, clap, tableflip, facepalm, groove, bigdance, cheer. Existing
  saved favorite lists retain their settings. Updated representative harness
  freezes, seated framing and dance back/side selections for the new IDs.
- Catmull–Rom uses Blockbench 5.2.2 loop neighbours: second-to-last before the
  first key, second after the last duplicate seam key. Applies to player and
  prop tracks only for Loop.LOOP; once/hold retain clamped neighbours. Tests
  compare both seam velocities, verify the synthetic expected derivative,
  and check every rotation/position/scale axis in shipped groove AND bigdance
  across wrap. Sparse/empty tracks remain supported.
- Explicit player states: PLAYING -> HOLDING -> BLENDING_OUT -> DONE. Hold
  reaches the final pose once and stays there until stop; cancellation freezes
  its current pose. Exit takes .2 s, including normal one-shot completion;
  entry remains .15 s. Partial-tick sampling also fades across natural end.
  Cancellation during entry fades from the current weight without increasing
  the entry weight. Tests cover all modes, repeated stops, a long hold, early
  cancellation, local/remote play+stop equivalence, and natural endings of all
  fourteen shipped one-shots (including wave).
- The existing movement/jump/attack/hurt/death/explicit-cancel path still calls
  EmotesClient.stop(), which sends the empty-ID stop request. HOLD is not
  filtered out of movement cancellation. Both Fabric and Forge relay this stop
  to viewers, whose receive() calls the same EmotePlayer.stop(). No hold-state
  packets are required. Movement cancellation respects metadata/global settings.
- Adapted E5's floor correction to the new sit's shared drop plus independent
  shoulder/torso pivot compensation. Matching leg translations permit a common
  lift through all six siblings without losing their relative offsets. The
  four-second sequence is checked at 240 Hz and 21 blend weights for foot
  clearance throughout entry, seated poses, rising and cancellation blending.
  Bow tests now use Blockato's front hip-edge pivot and export rounding.
- `python3 tools/import_blockato.py --apply` now adds unmatched new folders by
  default, copies changed exports/assets byte-identically, keeps existing
  sidecar formatting/settings, creates readable default metadata for new IDs,
  reports unchanged folders without writes/backups, backs up replacements and
  removes obsolete prop pairs. `--new` remains compatible. One-shot sit maps
  to sit_full, while a later hold-mode sit maps to sit. If the source retains
  both old sit and an equivalent canonical sit_full copy, the canonical folder
  wins; genuinely conflicting duplicate targets still fail.
- Updated README, format guide, credits, changelogs and store drafts to credit
  Blockbench animator Blockato for all fifteen batch 1+2 emotes, explain the
  corrected playback modes/IDs, and document the one-command importer.

Verified without Gradle:

- 34/34 pure-Java JUnit tests pass with cached JUnit 5.11.4 / Gson 2.11.0,
  `javac --release 21` and the existing `/tmp/e4_test.py` runner.
- 26/26 Python tests pass with
  `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v`.
  Validator reports exactly 16 valid built-ins / 0 errors; only groove and
  bigdance loop, all IDs/names resolve, and sit is absent.
- All four cached-jar `javac --release 21 -proc:none` targets pass using
  `/tmp/check_emotes_compile.py`: Fabric 1.21.1 / 1.21.11, Forge 52.1.20 /
  61.2.4. Existing loader deprecation warnings only. This does not validate
  Stonecutter generation, Mixin annotation processing/remapping or jar builds.
- Direct byte comparison against animator delivery passes for all fifteen
  animation exports and both table assets. Stretch has no diff against HEAD.
- Live intake dry-run succeeds. Later batches are already present, including
  sit_v2 (hold) and a canonical sit_full copy. Tested the real default-source
  --apply twice against a TEMPORARY copy of the catalog: first run validates
  32 emotes (16 new / 1 changed / 14 unchanged / 1 superseded legacy copy),
  second run makes no changes (31 unchanged / 1 superseded copy). Logs are
  `/tmp/emotes-e6-import-preview.txt` and `/tmp/emotes-e6-import-apply-{0,1}.txt`.
  These later batches were not added to the E6 repository catalog.
- `git diff --check` passes. Temporary scripts, test/compile products, smoke
  output and intake copies remain under /tmp.

Parent follow-up:

1. Build all four real Gradle targets and run the visual harness (44 captures /
   18 render checks). Review new batch 2 poses, groove/bigdance wrap over several
   cycles, entry transitions, sit_full floor placement and cancellation, and
   every one-shot ending (especially wave). Check Steve AND Alex, first-person,
   armor/held-item/outer layers and camera restoration as in E5.
2. Use a temporary custom hold animation (or the now-arrived animator sit_v2 in
   a separate test pack) to verify play -> hold -> movement/explicit cancel ->
   .2 s blend-out on two clients for both loaders. E6 intentionally leaves the
   built-in sit ID free; import later batches in their own content run.
3. Retain dedicated-server startup, vanilla-server fallback, resource reload,
   custom geometry/texture, tracking/reconnect and GUI-scale checks from E4/E5.
   Parent reviews/stages/commits this worktree; no commit was attempted here.

## E7 — multiplayer proof harness and release hardening (2026-11-10)

Implemented against the parent's `main @ 0bfc47c`. Read all shared Java runtime,
render/mixin, animation, config, networking and harness code, both loader build
scripts, metadata, existing tests and asset/import validation tools. No Gradle,
Minecraft/server launch, commit, or write to `.git` in this run. Parent retains
real build/run/commit ownership. Full instructions: `docs/MP_PROOF.md`.

Proof deliverables:

- Both loaders have dedicated `runServer` wiring; Forge now registers its server
  run with both runtime and dev proof source sets. Shared run script handles
  `-PrunDir`, `-Pport` (25599 default), EULA acceptance, offline mode, flat creative
  peaceful world, loopback binding, console input, and server heap. Client-only
  username/window/harness arguments cannot reach the dedicated server.
- `-PserverHarness=true` logs `EMOTE_SERVER play/stop player=... id=...`, including
  cleanup broadcasts, and places A/B three blocks apart through the server's
  command dispatcher. No client operator privilege is required. The game JVM
  property `quarkos_emotes.disableServer=true` skips channel/payload registration
  on both loaders. `-PdisableServer=true` or the Gradle `-D` property forwards it;
  the proof's vanilla mode uses it while keeping harness placement enabled.
- Client `-PjoinServer=host:port -Pusername=A|B -Prole=actor|viewer` connects from
  title, handles accessibility onboarding and polls the real tab list. Actor
  plays wave, then bigdance for five seconds (over a complete loop), then holds
  the ordinary forward key. Movement cancellation is NOT bypassed by this
  harness. Viewer observes actual remote EmotePlayer starts/stops and times,
  validates three-block placement and changed bones in the remote render hook,
  and captures `mp-viewer-wave.png` / `mp-viewer-bigdance.png` without freezing or
  manufacturing remote playback. Expected summaries: actor 4/0, viewer 10/0.
- `-Pvanilla=true` asserts channel unavailable, local playback and animated hands,
  exactly one displayed hint across two plays, no disconnect, and completed
  `mp-vanilla-hint.png`. Expected summary: 7/0. Disconnects, exceptions, screenshot
  failures and wall-clock timeouts cannot produce a successful summary.
- `tools/mp_proof.sh LOADER VERSION OUT [sync|vanilla] [PORT]` starts server, waits
  for Done, starts viewer and waits for its join before actor, collects logs and
  screenshots, stops server and cleans up process groups. Uses xvfb-run,
  --no-daemon, 1024 MiB Gradle / 1536 MiB client heaps, distinct run directories
  and distinct project caches to avoid Gradle's long-running task lock. Server
  startup builds all runtime/proof/resources; clients exclude those build tasks
  and reuse the completed outputs, avoiding compilation races with live JVMs. Seeds
  copied options with onboarding/focus pause/vsync disabled. Rejects old evidence.
  `check_mp_proof.py` requires complete assertions, successful summaries, both
  server play relays plus stop, screenshots, and no ERROR logs.

Audit findings and fixes:

- Missing late-tracker playback: server now keeps bounded identifier/start-time
  state and actual vanilla entity tracker sets. Common addPairing/removePairing
  mixins cover both loaders; a new tracker receives current emote plus bounded
  elapsed milliseconds. Client seek advances normally and skips historical
  sounds. Natural one-shot endings now send stop, preventing stale server state.
  Server tick cleanup clears state/rate records/trackers on disconnect, death,
  entity replacement/respawn and dimension change; shutdown clears all state.
  Protocol is v2: Fabric request_v2/play_v2 IDs and Forge protocol version 2.
- Existing bounded UTF (128), identifier grammar and separate play/stop rate
  budgets remain enforced. Unknown server IDs and invalid elapsed values are
  ignored; no animation/path data is loaded from a packet. Fabric explicitly
  schedules both directions on the game thread; Forge uses consumerMainThread
  with packet directions enforced. Common server code has no client imports.
- Remote looping states previously survived unload forever. Client state now
  removes unloaded/dead/replaced entities, along with identity references.
  Lifecycle synchronization also runs BEFORE accepting a packet or starting
  playback: otherwise a valid late-join packet arriving before the first tick
  in a new world would be cleared by that tick's world-change handling.
- Resource apply is confined to the client thread. Reload cancels/broadcasts
  local playback and restores its camera before texture release; remote active
  players rebind to replacement definitions at current elapsed time, without
  replaying old sounds. Removed IDs/stopping states are discarded. Existing
  geometry/model caches and registered local textures are invalidated/released.
  Added rejection of non-finite geometry inflation before ModelPart creation.
- Camera restoration covers world/player replacement, disconnect, death, any
  open screen, reload and normal finish. Config previews stop previous shared
  playback before replacing the local player, preventing a remote stuck loop.
  Hint lifetime now follows the connection, surviving dimension/respawn changes.
- Corrupt/empty config is backed up as `.corrupt-<time>` before default recovery;
  each load starts with fresh defaults. Existing enum/list/volume normalization
  and atomic save remain. Overlapping emote bindings are drained with one action
  per tick (wheel first, then first favorite), avoiding multiple simultaneous
  play requests. Keys remain rebindable in vanilla Controls; favorites default
  unbound. Other mods can still choose B: the user must rebind a conflict.
- Proof implementation now compiles in a separate `proof` source set. Runtime
  code has small opt-in interfaces/reflection hooks, so release main/source jars
  omit harness classes without dangling static references. Both binary jars now
  include META-INF/LICENSE and exclude test/dev assets. Added MIT/author/icon,
  both-side Fabric environment, optional Forge display-test and placeholder
  checks in `tools/audit_release.py`; added Blockato contributor/credits metadata.
  No authoritative contact/project URLs were supplied; optional links remain
  absent, with no invented or placeholder URL shipped.
- `tools/check_compile.py` replaces the old temporary verifier. It applies BOTH
  directions of the Stonecutter Identifier/ResourceLocation and
  AvatarRenderer/PlayerRenderer substring replacements, strips inactive branches,
  selects the correct Authlib version, and compiles runtime and proof separately.
  Grep found no further executable getPlayerRenderer/ResourceLocation substring
  victims; the existing explanatory getPlayerRenderer comment is harmless.

Validation (without Gradle):

- Four-target cached-jar `javac --release 21 -proc:none`, compiling runtime then
  proof against it, succeeds for Fabric 1.21.1 / 1.21.11 and Forge 52.1.20 /
  61.2.4. Runtime class output contains no harness classes. Existing deprecated
  loader API and unchecked renderer-cast warnings remain. Inspected javap of
  both versions: ServerEntity entity/addPairing/removePairing and MinecraftServer
  tickChildren/stopServer signatures match the new common mixins.
- 43/43 pure Java JUnit tests pass with cached Gson/JUnit and SLF4J API using
  `/tmp/e7_test.py`. Includes rate-bound/reset checks, elapsed state updates and
  removal, late loop/hold/expired-once seeking, skipped historical sounds,
  resource definition rebinding, non-finite inflate rejection, and malformed
  config backup/default recovery plus normalization. Existing 34 tests retained.
- 33/33 Python tests pass (`PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover
  -s tools -v`): existing 26 plus bidirectional preprocessing, incomplete/failed
  evidence rejection and binary release audit fixtures. Asset validator still
  reports 16 valid built-ins / 0 errors; animation/prop payloads are unchanged.
- `bash -n tools/mp_proof.sh` and `git diff --check` pass. Exercised shell startup,
  separate directories, FIFO stop, summaries/evidence checks and cleanup with
  MOCK Gradle/xvfb processes in a temporary copy, for sync and vanilla modes.
  This validates orchestration only; it is NOT a multiplayer game proof.

Parent TODO / release gates:

1. Run real four-target Gradle build/test tasks. In particular validate Kotlin run
   wiring, proof source-set/Forge dev-mod classpaths, Stonecutter generation,
   common Mixin application/remapping and dedicated-server class loading. javac
   cannot validate these. Re-run the existing visual harness after the source-set
   split. Audit the four new BINARY jars with tools/audit_release.py; cached/pre-E7
   jars are not evidence of these packaging changes.
2. Run tools/mp_proof.sh for sync and vanilla on each loader/version, using fresh
   output directories. Require game summaries and EMOTE_MP_PROOF PASS, review all
   screenshots and actual logs. No live E7 proof or new screenshots were produced
   here. Strict ERROR checking intentionally surfaces launch/environment errors.
3. Exercise late join and tracking leave/re-enter while bigdance loops. Require
   nonzero EMOTE_SERVER track elapsed and matching advancing remote phase; test
   respawn, dimension change, death, disconnect/reconnect and vanilla peers. The
   default two-player script automates play/stop/movement, not these scenarios.
4. Exercise F3+T during local/remote animation, removed/changed resource IDs and
   local prop textures; verify local stop/camera restoration, remote rebinding
   and texture release. Check camera on death/disconnect/open screens, Controls
   rebinding conflicts, Steve/Alex, armor/capes/items and first-person restoration.
5. Supply verified release contact/source/issues URLs if desired, review/stage
   and commit the working tree. No git write or commit was attempted.

## 2026-11-10 — E8, animator batch 3 / held sit / Euler turns / Alex pivot

Completed in the main working tree; no Git writes or Gradle invocations.

- Read `/home/box/blockato/emotes/NOTES_v3.md` and `md5_delivered.txt`.
  Imported the 15 requested batch-3 emotes and held sit via the importer, and
  re-copied canonical sit_full (now `animation.emote.sit_full`). Kept stretch.
  Exactly **32 built-ins**: wave, thinking, clap, tableflip, facepalm, groove,
  bigdance, cheer, bow, salute, laugh, shrug, point, dab, sit_full, stretch,
  yes, no, cry, rage, victory, heart_hands, wave_big, backflip, spin, worm,
  floss, tpose, zombie_walk, slow_clap, sleep, sit. Four loops (groove,
  bigdance, floss, zombie_walk), two holds (sit, sleep), 26 one-shots.
- The live delivery has advanced since the request: canonical
  `sit/sit.animation.json` is now already hold-mode and byte-identical to
  sit_v2; the old one-shot is archived as sit_v1. The importer accepts both
  stages: prefer canonical sit ONLY if hold-mode, otherwise v2. Equivalent
  archived one-shots defer to canonical sit_full. Fixture tests cover the old
  canonical, both holds coexisting with different bytes, and v2 removed.
- Later deliveries are also present in the intake (12 names beyond E8). Added
  `--include name1,name2,...` so the E8 command in README stays pinned at 32.
  An initial broad apply imported those extra names; removed those additions
  before completing E8. Final selected re-import: 0 replaced, 0 new,
  **31 unchanged**, 14 skipped, 32 valid / 0 errors. Import replacement backups
  from this run are in `/tmp/emotes-e8-import-backups/`, outside shipped files.
- Verified the existing Euler path end to end: parser retains raw signed degree
  vectors; Track interpolates per component (catmullrom/linear/step);
  BedrockAxes converts without normalization; PoseMath entry/exit blends
  components; EmotePose writes ModelPart Euler fields. No shortest path or
  quaternion interpolation. Shipped tests check backflip X=-180 at .8 s,
  an interpolated value below -180, final -360; spin Y=270.5 at .9 s and
  final +360; half-weight entry/exit still contain half of the full turn.
- Added parsed/validated `arm_pivot` enum (`model` default / `steve`) and set
  steve for backflip, spin, worm and sleep. Slim models are recognized by
  their baked initial arm Y=2.5 (PartPose field in 1.21.1, accessor in 1.21.11).
  Blend the temporary Java-space Y shift of -.5 px with playback weight,
  starting from reset vanilla defaults each frame. Wide models stay at Y=2;
  first-person arms use the same helper. Metadata survives withProps/reloads.
  Tests cover weighted/default/wide behavior, both rig baselines yielding
  the same authored orbit pivot, and restoration after cancellation.
- Tests with the shipped sit/sleep run past their duration, assert HOLDING,
  seated/lying final poses, freeze on stop and fade over exactly .2 seconds.
  Existing movement/cancel dispatch remains the same; visual harness mode
  deliberately bypasses movement cancellation so anchoring cannot cancel sleep.
- Harness front + 3/4 shots cover all 32. Times: upside-down backflip .8,
  worm mid-wave 1.25, floss mid-swing .25, spin ~270 at .9 plus ~90 at .63,
  sit/sleep final held frames. Added backflip/worm side views and named
  `emote-{backflip,worm,floss}-{steve,alex}.png` variants. HarnessSkinMixin
  overrides only the local player's getSkin in visual harness mode, forcing
  wide Steve/slim Alex texture AND dispatcher model. 1.21.1 uses client
  PlayerSkin/Model and PNG texture; 1.21.11 uses entity PlayerSkin/PlayerModelType
  and ClientAsset.ResourceTexture. Inactive outside visual proof mode.
- Kept EMOTE_CHECK/EMOTE_SUMMARY. Render hook records actual body X/Y radians
  and checks backflip X / spin Y exceed 150 degrees. Variant checks verify
  forced model type; held shots check HOLDING at weight 1 after anchoring.
  Added fp-sleep/fp-sleep-camera; both camera shots wait through actual client
  ticks after cancellation and check camera restoration plus playback removal.
  Full 32-emote run: **87 shots / 59 checks** (expected pass=59 fail=0).
  First-person SHOW_ARMS keeps the normal camera and applies authored offsets;
  THIRD_PERSON_CAMERA switches/restores via the existing lifecycle code. Visual
  offsets do not move the entity/anchor, hitbox or ordinary gameplay camera.
- Wheel retains eight sensible default favorites and four pages; added category
  cycling that combines with search/favorites, grouped the 32-emote ordering,
  and added English names/categories/tooltips. Updated README, format guide,
  CREDITS (Blockato batches 1-3), changelog and store drafts with the 32 list,
  sit rename rule, whole-body orbit and raw Euler contract.

Validation:

- `PYTHONDONTWRITEBYTECODE=1 python3 tools/check_compile.py`: runtime then proof
  compile with cached jars, `javac --release 21 -proc:none`, for Fabric 1.21.1,
  Fabric 1.21.11, Forge 52.1.20 and Forge 61.2.4. Both directions of Stonecutter
  Identifier/ResourceLocation and AvatarRenderer/PlayerRenderer substring
  replacements apply. No getPlayerRenderer-style identifier was introduced.
  Only existing deprecated API / unchecked cast warnings remain.
- `/tmp/e7_test.py` cached Gson/JUnit pure Java runner: **49/49 tests pass**
  (43 previous + 6 batch-3 tests; previous catalog/seam tests updated).
- `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v`:
  **35/35 pass**. `tools/validate_emotes.py`: **32 valid, 0 errors**.
- Compared **31/31 animator animations** byte-for-byte with delivery and MD5
  against md5_delivered.txt (sit compared directly with sit_v2); table geometry
  and PNG also match bytes and MD5. No animator animation contents edited.
- `git diff --check` passes. Compile/test/import logs: `/tmp/e8-compile.txt`,
  `/tmp/e8-java-tests.txt`, `/tmp/e8-python-tests.txt`,
  `/tmp/e8-import-final.txt`, `/tmp/e8-validation.txt`.

Parent follow-up (explicitly outside this run's execution scope):

1. Run the four Gradle builds/tests and live harness. javac cannot validate
   Mixin application/remapping or produce visual proof. Review all 87 images,
   especially Steve/Alex shoulders, lying sleep, 360-turn poses, floor framing
   and first-person arms; require EMOTE_SUMMARY pass=59 fail=0 for a full run.
2. Focused visual selection: `-Ponly=backflip,worm,floss,spin,sit,sleep,fp-sleep,fp-sleep-camera`
   with a fresh `-Pshots=...` directory. Individual variant names are supported.
3. Check actual movement/cancel and camera modes during held sit/sleep, then
   armor/capes and multiplayer on both loaders/versions. No live screenshots
   or Gradle evidence were produced here. Stage/review/commit from the parent
   with writable .git; this run leaves all requested changes in the workspace.
