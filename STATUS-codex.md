
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
