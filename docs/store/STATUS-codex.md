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
