# Codex content handoff log

This append-only log is under `docs/store/` because run E2 authorizes only
the listed content/doc paths, not a repository-root `STATUS-codex.md`.

## 2026-11-09 — run E2, branch `content`

### Completed

- Read the sister cosmetics build/layout, loader metadata, Stonecutter
  configuration, and harness reference without modifying that repository.
- Authored sixteen standalone `1.8.0` Bedrock animation files, with the
  document-level `quarkos` extension and canonical filename/ID pairs.
  The requested list had fifteen names; `stretch` is the sixteenth.
- Added anticipation/recovery for gestures, matching loop endpoints,
  sustained thinking/laugh poses, two 100-BPM clap contacts, and a visual
  cheer hop. Authored the dances on a 120-BPM grid: 2-second groove and
  8-second spotlight routine, with an unwrapped full turn.
- Added a five-cube oak table, prop rotation/translation and landing cue,
  and held final poses for table flip and sit. The seated pose lowers all
  six sibling bones by 10 pixels; the turn includes shoulder/hip orbit offsets.
- Supplied sixteen English name entries in `en_us.emotes.json`. This file
  is intentionally a merge fragment, not the actual Minecraft language file.
- Added `tools/validate_emotes.py`, the full animation format/Blockbench
  guide, README, changelog, credits, and draft Modrinth/CurseForge store copy.
  Store metadata lists both loaders and game versions, client required/server
  optional, with explicit pending verification and screenshot placeholders.
- Kept all source changes in the allowed paths. No Java, build files, sister
  repository files, network resources, or publication endpoints were changed.

### Verified locally

- `python3 tools/validate_emotes.py`: **16 valid emotes, 0 errors**;
  1,728 channel keys/constants in total. Every moving channel has 13–29 keys,
  apart from genuinely constant channels. Exactly sixteen unique IDs and
  corresponding English names are present.
- An ad hoc check script in `/tmp` passed **63** validator acceptance,
  malformed-input rejection, and CLI exit-code checks, including duplicate
  JSON keys, invalid vectors/times/bones/metadata, sounds and cube props.
  This temporary harness is not part of the committed content.
- Checked **51** looping channel endpoints for continuity (rotation modulo
  360°) and **20** held channels for a stable last pair of keys.
- The complete `nod.animation.json` example in the specification validates.
- `summary.txt` is **105 characters**, below the 120-character maximum.

### Unverified and integration findings

No Gradle build, Java tests, network request, client launch, screenshot capture,
or multiplayer run was performed in this content worktree. The parent session
owns the four-target build, pure-Java tests, and xvfb harness. Visual timing,
hand contact, ground contact, slim skins, prop attachment, and sound balance
remain unverified. The external `/home/box/blockato/emotes/` replacements have
not been imported or represented as reviewed.

Read-only inspection of the parallel code worktree during E2 found these
integration issues; the code may change independently after this snapshot:

1. `EmoteParser.animation` reads `quarkos` from the animation object, whereas
   the fixed contract and every authored file place it at the **document
   root**. The code session must pass/read that root extension. Without the
   fix, names, category, props, easing, and explicit ID are lost.
2. Its fallback ID prepends the source namespace to the entire animation
   key after removing `animation.`. For `animation.quarkos_emotes.wave` that
   produces `quarkos_emotes:quarkos_emotes.wave`, rather than
   `quarkos_emotes:wave`. Canonical fallback parsing must agree with the
   validator even when the optional extension is absent.
3. `BedrockAxes.position` currently negates X. Audit this against the
   canonical Steve right-arm pivot of `[-5,22,0]`: positive authored X moves
   that hand inward. Clap/chin offsets and full-turn pivot orbits are good
   tests for an accidental outward/mirrored translation. Do not reverse arm
   rotation X: authored X = -90 must remain forward.

### TODO for integration/release

- [ ] Resolve root metadata and canonical fallback ID parsing in the Java session.
- [ ] Merge `lang/en_us.emotes.json` into the code-owned `lang/en_us.json`.
- [ ] Validate X/Y/Z rotations and offsets against the Blockbench Steve rig.
- [ ] Verify pose blend, loop repeats, held poses, movement cancellation,
      props, clap/landing sound timing, and resource reload/ID override order.
- [ ] Build/test Fabric 1.21.1 and 1.21.11 and Forge 1.21.1 (52.1.20) and
      1.21.11 (61.2.4); verify client and optional-server environments.
- [ ] Run the parent xvfb harness for third-person front/side poses and
      interface captures; review classic and slim skins.
- [ ] Review any animator-delivered replacements by filename/ID, preserve
      standard `loop` and root metadata, then rerun validation and visual checks.
- [ ] Fill screenshot placeholders and update draft store claims/dependency
      instructions only after runtime verification; confirm category taxonomy.
- [ ] Preserve license/author credit when incorporating later animation assets.

### Commit handoff — filesystem limitation

The requested local commit on branch `content` could not be created:
`git add` failed with `Unable to create .../index.lock: Read-only file system`
for `/workspace/krill-emotes/.git/worktrees/krill-emotes-content/`.
The sandbox exposes the shared Git metadata read-only, and approval escalation
is unavailable. No commit or push was made. All 28 authored files remain in
this worktree; an applyable patch is provided at
`/tmp/quarkos-emotes-content-E2.patch` for the parent session.

- [ ] In a session with writable Git metadata, stage these allowed content
      paths and commit on `content` as Emilio Schwaiger <emilio@quarkos.dev>,
      e.g. `git -c user.name='Emilio Schwaiger' -c user.email=emilio@quarkos.dev commit -m 'Add initial emote content, validator, and authoring docs'`.
      The failed `git add` did not stage the files; stage them before committing.

## 2026-11-09 — run E3, Blockbench intake and content review

### Completed

- Added `tools/import_blockato.py` using only the Python standard library.
  It recursively discovers animation JSON, resolves names/aliases and
  filename/folder fallbacks, splits multi-animation exports, preserves native
  root metadata and exported Bedrock payloads, and canonicalizes prop aliases.
  Unmatched names require `--new`; ambiguous names remain skipped. Duplicate
  targets and colliding prop aliases are rejected with an explicit reason.
- Preview is the default and writes no files, directories, temporary files,
  backups, or bytecode. `--apply` validates candidates first, copies replaced
  files to `tools/replaced/<UTC timestamp>/`, writes atomically, and validates
  the output directory. The report shows source, ID, length, loop, bone/key
  counts, status, reasons, and missing-prop-model warnings.
- Extracted the validator's `validate_data` entry point for previews without
  temporary files. Accepted standard numeric Bedrock single-sided `pre`/`post`
  keys: the missing side takes the supplied side. Actual Blockbench exports
  use `post` alone; their keyframes and interpolation are preserved unchanged.
  Unknown fields, empty keys, invalid vectors, and unsupported tracks still fail.
- Added twelve `unittest` tests with temporary synthetic fixtures covering
  names/aliases/fallbacks, dance ambiguity, all three loop modes, prop aliases
  and warnings, metadata/sound/model preservation, new opt-in, invalid input,
  duplicate targets, exact backups, atomic output validation, empty work in
  progress, and previews with no writes.
- Documented commands, mapping precedence, backup/rollback behavior, report
  and exit-code semantics, generated metadata/translations, and review limits
  in “Replacing built-in emotes with Blockbench exports.”

### Review of all sixteen built-ins

Reviewed every channel, then sampled the independent sibling-bone Steve and
Alex rigs at 20 FPS. Corrected ten files while retaining IDs, lengths, loop
modes, sound events, and presentation/prop metadata (author notes updated):

- `dance_elaborate`: reversed the incorrect Z pivot orbits during the turn.
  The old shoulder/hip paths turned opposite the bones, causing torso
  intersections and crossed legs. Added position keys every 0.05 seconds
  during the spin to follow circular paths between the old quarter-second
  keys, cleared shoulders outward by one pixel, and removed arm roll that
  pushed the raised arm into the head. Preserved the unwrapped 360° turn.
- `clap`: replaced inward shoulder slides with opposing arm yaw and a small
  forward offset. Hands converge ahead of the chest; shoulders no longer
  pass deeply through it. Kept both contact/sound times.
- `dab`: moved the crossing reach ahead of the face using yaw, reduced its
  excessive overhead pitch, and corrected the opposite arm's recovery roll.
- `facepalm`: reduced inward shoulder movement and moved the palm ahead of
  the forehead, clearing the chest and deep head penetration.
- `wave`: kept the overhead sweeps outside the head instead of swinging
  through its side.
- `salute` and `thinking`: used shoulder yaw and forward clearance instead
  of placing a shoulder inside the chest; reduced deep face intersections.
- `dance_groove`, `laugh`, and `stretch`: corrected inward hanging-arm roll
  and/or inward shoulder offsets that drove arms into the torso.

`bow`, `cheer`, `point`, `shrug`, `sit`, and `table_flip` required no keyframe
changes. Normal shoulder/hip junction overlap and shallow intentional face
contact remain; this review does not claim zero cube intersections. In
particular, torso dips overlap the hip junction on a rigid Steve rig.

### Verified locally

- `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest tools.test_import_blockato -v`:
  **12 tests passed**.
- `python3 tools/validate_emotes.py`: **16 valid emotes, 0 errors**;
  **1,856 channel keys/constants**, including 128 added spin position keys.
- Checked **51 looping channels** for matching endpoints (rotations modulo
  360°), and **20 held channels** for stable final keys. All pass.
- Sampled **1,776 rig frames** across all sixteen emotes and both skin rigs,
  using the animator's read-only `clipcheck.py` math with a 0.25-pixel overlap
  grid. The corrected spin no longer crosses the legs beyond the rig's
  normal 0.2-pixel rest overlap; the major arm/torso and arm/head penetrations
  are removed. Maximum head rotation **relative to the torso** is about
  **29.28°**, below 70°. Large absolute head Y values in the full-body dance
  are the intentional shared turn, not neck rotation.
- Review scratch scripts, original content copies, and detailed audit output
  are under `/tmp/review_emotes_e3.py`, `/tmp/emotes-e3-original/`, and
  `/tmp/emotes-e3-pose-audit.json`; they add no project dependency.
- Ran the importer against `/home/box/blockato/emotes` in dry-run. Five
  exports had arrived during this session: **clap, facepalm, table_flip,
  thinking, wave**. All five map and validate as prospective replacements;
  none were applied. Geometry and other JSON without `animations` are ignored.
  Export files are being written in parallel, so future intake counts can vary.

### Parent handoff and remaining verification

Changes are left in the `content` worktree for the parent to review and commit.
No Git commit was attempted. No Java or build files were changed.

The parallel runtime session should support a missing keyframe `pre` or `post`
by falling back to the supplied side, as documented above. Review actual
replacements after applying: retained sound times, notes, table geometry,
and attachment space may need intentional updates for a new motion. The
current exports also change some loop modes; the importer preserves their
standard `loop` field. Client rendering, classic/slim skin appearance, prop
anchoring, and sound playback still require in-game verification; numerical
rig checks do not establish runtime compatibility.


## E4 integration update — 2026-11-09

This update supersedes the earlier content/intake TODOs above. The runtime now
ships exactly 16 emotes: five unchanged one-shot Blockato exports (wave,
thinking, tableflip, facepalm, clap), plus the eleven retained E2 animations.
The previous `table_flip` ID and its flat file are replaced by `tableflip`.
English translations are merged into `lang/en_us.json`; the fragment is removed.
Folder discovery, sidecars, geometry/PNG props, wheel/harness IDs, credits and
store drafts are updated. The importer preserves bone names/export bytes and
backs up/copies runtime folders and sidecars; proof/authoring files are excluded.
See the root `STATUS-codex.md` E4 entry for verification and remaining checks.

## E6 — current content supersedes earlier draft catalog (2026-11-10)

Current release drafts now describe fifteen byte-identical animator exports by
Blockbench animator Blockato (batches 1+2) plus the original stretch. Batch 2
replaces the remaining flat drafts except stretch. Groove and bigdance loop;
all other built-ins play once. The delivered four-second sit-down/stand-up
sequence is sit_full, while sit remains free for a later hold delivery.

Updated README, format guide, credits, changelog, store descriptions and animator
metadata. Documented one-command import, loop seam interpolation and .2-second
exit blending. The root STATUS-codex.md contains E6 implementation/validation
results and parent-run build, screenshot and multiplayer follow-ups. No store
publication or external messages were sent.

## E9 — 2026-11-10

Store drafts and changelog now list all 42 built-ins (41 Blockato emotes plus
stretch), including the ten batch-4 additions, two companion chain clips and
arrived sword prop. Strict discovery, chaining and prop parenting are documented
in ../ANIMATION_FORMAT.md. Full implementation/validation and parent-only live
proof requirements are in ../../STATUS-codex.md under E9. This task produced no
Gradle artifacts, Minecraft screenshots or multiplayer run. Offline checks:
61 Java tests, 35 Python tests, four-target javac, 42 valid emotes / 0 errors,
47 runtime payloads matching delivered bytes/MD5. Parent must review full
107-shot / 77-check or quick 46-shot / 50-check evidence before publication.

## E10 — final delivery (2026-11-10)

This entry supersedes E9's v4 pin and shot counts. Store drafts now list all 42
emotes by their shipped categories: 41 by Blockato + Stretch. All 47 animator
runtime files match the current delivery bytes/MD5, including Floss v2 and Sword
Salute v2. Credits include both props; the 32×32 sword texture is provisional.
Screenshot slots explicitly remain placeholders; no new game images were made.

Offline checks: 69 Java tests, 38 Python tests, four-target javac, 42 valid emotes
and 47 exact runtime payload matches. Full harness plan is 115 shots / 103 checks;
quick 46 / 51; sword-only 10 / 27. These are planned counts, not game results.
The parent reported E8 Fabric 1.21.11 59/0 + 87 shots; that predates this update.
Follow ../RELEASE_CHECKLIST.md before publishing and use ../BLOCKATO_FINAL.md
for the final runtime hash manifest. No publishing/uploading occurred in E10.
