# Final-set release checklist

These are the remaining parent/maintainer checks before publishing 1.0.0.
E10 ran no Gradle tasks, Minecraft clients or multiplayer servers. The parent
reported E8 Fabric 1.21.11 results (59/0 checks, 87 shots); those predate the
final Floss v2/sword delivery. Offline javac and tests cannot establish packaging,
Mixin application, texture appearance or network behavior.

- [ ] Review the E10 diff and commit from the parent with writable `.git`.
- [ ] Build and test all four targets with Java 21+; retain task logs:
  `./gradlew :1.21.1:build :1.21.1:test :1.21.11:build :1.21.11:test
  :forge:1.21.1:build :forge:1.21.1:test :forge:1.21.11:build :forge:1.21.11:test`.
  Run as one shell command. Confirm Stonecutter replacements in both directions,
  proof source-set separation and Fabric/Forge run configuration, including quick.
- [ ] Run `python3 tools/audit_release.py` against the four new **binary** jars.
  Inspect final jar contents: 42 main emotes, exactly `pushups_enter` and
  `meditate_loop`, both matching table/sword geo+PNG pairs, English names,
  sword `arm_pivot: "steve"`, icon/license and correct expanded loader metadata.
  Compare all 47 animator runtime payloads inside each jar with the
  [final delivery manifest](BLOCKATO_FINAL.md); exclude tools, archives, rigs,
  proof/authoring assets and development harness classes.
- [ ] Run a fresh full visual harness on Fabric 1.21.11: **115 screenshots,
  `EMOTE_SUMMARY pass=103 fail=0`** and pushups/meditate chain-switch logs.
  A heavy-load quick pass is **46 shots / 51 checks** and does not replace the
  sword model/angle evidence. Use a fresh directory per run, for example:
  `xvfb-run -a ./gradlew :1.21.11:runClient -Pharness=true
  -Pshots=/tmp/emotes-e10-full -Pwidth=1280 -Pheight=720 -PguiScale=3`.
  Join wrapped command lines when executing.
- [ ] Run focused sword evidence on **all four loader/version targets** with
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
- [ ] Run fresh dedicated sync and vanilla-server proof for all four targets,
  following [MP_PROOF](MP_PROOF.md). Keep watchdog disabled (`max-tick-time=-1`),
  `MP_TIMEOUT_SCALE=3` or higher if needed, and unique evidence directories.
  Require actor **4/0**, viewer **10/0**, vanilla **7/0**, independent
  `tools/check_mp_proof.py` success and `EMOTE_MP_PROOF PASS`.
- [ ] Manually verify late join/tracking resume during pushups entry/main and
  meditate main/loop; stop during each stage; F3+T during local/remote sword
  playback; dimension change, respawn/death/reconnect, missing pack IDs and
  camera reset. The default multiplayer flow does not automate these cases.
- [ ] Select actual release screenshots for the explicit slots in
  `docs/store/modrinth.md` and `curseforge.md`, including the final sword/Floss
  variants. Keep placeholders until reviewed images exist. Capture readable UI
  at supported resolutions/scales and avoid presenting animator previews as
  Minecraft screenshots.
- [ ] Confirm Blockato credit and permission to distribute the final animation/
  prop artwork, including the provisional sword texture. Confirm release version,
  exact game versions/loaders, dependency declarations, client/server environment,
  MIT attribution, project URLs (if available), categories and final store text
  against the verified artifacts. No NeoForge/Quilt/additional-version claim.
- [ ] Publish only the reviewed, verified binary artifacts and selected images;
  archive their hashes, build/test logs and visual/multiplayer evidence. This run
  has neither published nor uploaded anything.
