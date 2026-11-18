# Final-set release checklist

These are the remaining parent/maintainer checks before publishing 1.0.0.
E10 ran no Gradle tasks, Minecraft clients or multiplayer servers. The parent
reported E8 Fabric 1.21.11 results (59/0 checks, 87 shots); those predate the
final Floss v2/sword delivery. Offline javac and tests cannot establish packaging,
Mixin application, texture appearance or network behavior.

- [ ] Review the E10 diff and commit from the parent with writable `.git`.
- [x] Build and test all four targets with Java 21+; retain task logs:
  `./gradlew :1.21.1:build :1.21.11:build :forge:1.21.1:build :forge:1.21.11:build`
  (`build` runs the tests). 2026-11-10: BUILD SUCCESSFUL, unit tests included,
  after the late-join cue fix. Stonecutter still rewrites `PlayerRenderer` on
  1.21.11; call sites use `getRenderer` plus a cast.
- [x] Run `python3 tools/audit_release.py` against the four new **binary** jars.
  2026-11-10: PASS on `quarkos_emotes-fabric-1.0.0+1.21.1`,
  `quarkos_emotes-fabric-1.0.0+1.21.11`, `quarkos_emotes-forge-1.0.0+1.21.1`
  and `quarkos_emotes-forge-1.0.0+1.21.11`. The script checks 42 main emotes, exactly `pushups_enter` and
  `meditate_loop`, both matching table/sword geo+PNG pairs, English names,
  sword `arm_pivot: "steve"`, icon/license and correct expanded loader metadata.
  Compare all 47 animator runtime payloads inside each jar with the
  [final delivery manifest](BLOCKATO_FINAL.md); exclude tools, archives, rigs,
  proof/authoring assets and development harness classes.
- [x] Run a fresh full visual harness on Fabric 1.21.11: **138 screenshots
  (the 115 plus the wheel matrix), `EMOTE_SUMMARY pass=103 fail=0`**.
  Shots are in `docs/proof/fabric-1.21.11/`. Pushups and the low poses are
  framed in full.
  A heavy-load quick pass is **46 shots / 51 checks** and does not replace the
  sword model/angle evidence. Use a fresh directory per run, for example:
  `xvfb-run -a ./gradlew :1.21.11:runClient -Pharness=true
  -Pshots=/tmp/emotes-e10-full -Pwidth=1280 -Pheight=720 -PguiScale=3`.
  Join wrapped command lines when executing.
- [ ] Run focused sword evidence on **all four loader/version targets**.
  Fabric 1.21.11 stills (Steve and Alex, front/side/¾ visible at 0.8s, hidden at
  0.249s) are in `docs/proof/fabric-1.21.11/props/`. The other three targets
  were not recaptured after the framing change. Original requirement:
  `-Pharness=true -Ponly=sword_salute`: **10 shots / 27 checks** each.
  Tasks are `:<version>:runClient` and `:forge:<version>:runClient`.
  Require visible front/side/¾ at 0.8 s and hidden at 0.249 s for Steve and Alex,
  correct forced models and `*-prop` checks for sampled/submitted visibility.
  Check actual on-screen grip alignment, orientation, texture UVs and no hidden
  sword pixels. Approve Blockato's provisional 32×32 texture or request an
  updated animator delivery and refresh bytes/manifest before publishing.
- [ ] Watch Sword Salute in normal ticking playback on both skins: hidden before
  0.25 s, visible at 0.25 s, hidden at 2.20 s, including smooth partial frames.
  Check immediate removal on movement/cancel/end, F3+T, emote switch and replay.
  Check no zero-scale quads/z-fighting and no arm-pivot change left after exit.
- [ ] Review final Floss v2 on Steve/Alex, full turns, holds (sit/sleep/kneel/
  lay_down), pushups entry→loop and its intentional 3.25 px shoulder slide,
  meditate main→breathing loop, table UVs/floor contact, armor, outer layers and
  held items. Accept or address the documented vanilla-rig leg/torso overlap
  (up to 1.88 px in seated/kneeling/tucked poses). Confirm movement cancellation,
  first-person animated arms, camera restoration and input conflicts on each
  loader/version; retain screenshots/logs identifying their target.
- [x] Run fresh dedicated sync, late-join, and vanilla-server proof for all four
  targets, following [MP_PROOF](MP_PROOF.md). Each run is `EMOTE_MP_PROOF PASS`
  with actor 4/0, viewer 10/0 (late viewer 6/0), vanilla 7/0. Evidence is in
  `docs/proof/mp/`.
- [x] Lifecycle on Fabric 1.21.11 and Forge 1.21.1: respawn, End dimension,
  disconnect/reconnect, and F3+T. Actor 8/0, viewer 12/0, proof frames in
  `docs/proof/mp/*-life/`. The same cases were not repeated on Fabric 1.21.1
  or Forge 1.21.11. Pushups/meditate stage tracking and a live sword-salute
  watch are still open.
- [x] Select actual release screenshots for the explicit slots in
  `docs/store/modrinth.md` and `curseforge.md`. Gallery frames at 1920×1080
  are in `docs/store/shots/`, plus `docs/store/icon-512.png`. Project URLs
  are still absent on purpose. See [RELEASE.md](RELEASE.md).
- [ ] Confirm Blockato credit and permission to distribute the final animation/
  prop artwork, including the provisional sword texture. Confirm release version,
  exact game versions/loaders, dependency declarations, client/server environment,
  MIT attribution, project URLs (if available), categories and final store text
  against the verified artifacts. No NeoForge/Quilt/additional-version claim.
- [ ] Publish only the reviewed, verified binary artifacts and selected images;
  archive their hashes, build/test logs and visual/multiplayer evidence. This run
  has neither published nor uploaded anything.
