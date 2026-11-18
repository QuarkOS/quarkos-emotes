# QuarkOS Emotes — STATUS (times Europe/Vienna)

Current verification, the multiplayer matrix, lifecycle proof, and store assets
are in [docs/STATUS.md](docs/STATUS.md). Values still required before a public
release are in [docs/RELEASE.md](docs/RELEASE.md). Nothing has been published.

## Where it stood at 02:05, before this pass
- Branch `main`. 42 built-in emotes: 41 by the Blockbench animator "Blockato" (byte-identical to /home/box/blockato/emotes, minus `_archive/`) and `stretch` (written by Sol).
- Own animation layer, no playerAnimator dependency. Fabric and Forge for 1.21.1 and 1.21.11, built from one Stonecutter source tree.
- Verified by the parent with real Gradle and xvfb runs:

| target | build | unit tests | harness (final code c7d6010 + forge fix) |
|---|---|---|---|
| Fabric 1.21.11 | OK | 69/69 | EMOTE_SUMMARY pass=103 fail=0, 115 shots |
| Fabric 1.21.1 | OK | 69/69 | pass=103 fail=0, 115 shots |
| Forge 1.21.11 | OK | 69/69 | pass=103 fail=0, 115 shots |
| Forge 1.21.1 | OK | 69/69 | pass=103 fail=0, 115 shots |

- Release audit (`tools/audit_release.py`) passes on all 4 binary jars.
- NOT verified: multiplayer server sync. The only attempt, on Fabric 1.21.11 with E7 code, was aborted when the dedicated server's watchdog killed it at box load ~205. The viewer had joined. E9 then set max-tick-time=-1 and MP_TIMEOUT_SCALE; that change hasn't been rerun.
- Vanilla-server fallback is also unverified in game.

## Krill / Codex log
- Model: gpt-6.1-sol, reasoning high, non-fast.
- 10 Codex sessions (E1–E10). Estimated spend is ~$29.9 at an assumed $5/M input, $0.50/M cached, $30/M output, from tokens; Krill doesn't publish its price.
- **Krill ran out at 01:36.** E11 was refused at start with `HTTP 403 Forbidden: account balance is negative, please recharge first` (api.kbsfdxc.net /backend-api/codex/responses).
- E11's prompt was the Forge proof fix plus a visual review. The parent did the Forge fix by hand; the visual review wasn't done.
- Prompts are in /workspace/krill-emotes-ops/prompts/. The per-run log is below.

### Run log (ops)
    - 2026-11-09 23:11: repo /workspace/krill-emotes (main 7142045 skeleton from cosmetics build). Worktree /workspace/krill-emotes-content (branch content).
    - 23:10 E1 (core code, prompts/01-core.md) and E2 (content+docs, prompts/02-content.md) started in parallel on gpt-6.1-sol high non-fast. Codex has no network/Gradle; parent builds.
    - 23:24 E2 done (~$2.0 est): 16 emotes (15 requested + stretch), validator 16/0, docs/ANIMATION_FORMAT.md, docs/store/*. Committed by parent as content a7cacb4 (Codex sandbox can't write worktree git metadata).
    - 23:25 E3 started (Blockato intake tool + emote review, prompts/03-intake.md).
    - 23:26 E1 done (~$3.7 est): core code (35 Java files), committed by parent as 04177e7; content merged c8f492f.
    - 23:28 E3 done (~$1.0): tools/import_blockato.py + 12 tests, review fixes to 10 emotes; committed on content, merged befdd31.
    - 23:30-23:37 parent Gradle builds: b1/b2/b3 failed only on forge test wiring (implicit deps on syncBranchSources, missing junit) → fixed in forge/build.gradle.kts. b4 BUILD SUCCESSFUL, 4 jars, 14/14 tests on all 4 targets.
    - 23:40 harness h1 (fabric 1.21.11) started in worktree /workspace/krill-emotes-h @ befdd31. E4 started (animator contract: flat bones, geo props by bone name, folder layout, ship 5 Blockato emotes; prompts/04-blockato.md).
    - 23:52 h1 fabric 1.21.11 @befdd31: EMOTE_SUMMARY pass=16 fail=0, 20 shots (needed options.txt onboardAccessibility:false — first-run accessibility screen blocked the harness). Bugs seen: ids are 'quarkos_emotes:quarkos_emotes.bow' (animation-name derived), player small in frame, table prop not visible in table_flip shot.
    - 00:02 E4 done (~$2.9): committed 68c4331 (5 Blockato files byte-identical under emotes/<name>/, tableflip geo+png prop, sidecars, 22 Java + 20 Python tests per Codex). E5 started (visual/UI/harness pass from h1 screenshots, prompts/05-visual.md). Gradle limited to one job at a time (box load ~110-135).
    - 00:05 h1 all 4 targets @befdd31 PASS: fabric 1.21.11 16/0, forge 1.21.11 16/0, fabric 1.21.1 16/0, forge 1.21.1 16/0 (20 shots each).
    - 00:12-00:25 E4 @68c4331: Gradle build OK, 22/22 tests ×4 targets; harness h2 16/0 on all 4 targets (26 shots each, ids now file stems). Tableflip prop not visible at the 40% freeze yet.
    - 00:26 E5 done (~$2.4): committed 73411e8. E6 started (batch 2 animator emotes, Blockbench loop-seam catmullrom, hold_on_last_frame; prompts/06-batch2.md). Build+harness h3 of 73411e8 running in /workspace/krill-emotes-h.
    - 00:30 E5 73411e8 build failed on 1.21.11: getPlayerRenderer is rewritten to getAvatarRenderer by the Stonecutter PlayerRenderer→AvatarRenderer replacement (Codex's javac preprocessor doesn't do the reverse rewrite). Parent fixed EmoteHandsMixin (getRenderer + cast); rebuild OK. h3 harness chain running.
    - 00:26 E6 done (~$2.1): committed 0bfc47c (15 animator emotes + stretch; sit_full; loop seam; hold; blend-out; 34 Java/26 Python tests per Codex). 00:33 E7 started (multiplayer proof harness + hardening audit, prompts/07-multiplayer.md).
    - 00:45 h3 (E5+fix) all 4 targets 18/0 pass, 44 shots each.
    - 00:48 E6 0bfc47c: build OK; h4 fabric 1.21.11 18/0 (44 shots). E7 done (~$2.5) committed 84ebccc. 00:49 E8 started (batch 3: 15 emotes + sit hold, Euler unwrap, arm_pivot flag, Steve/Alex shots; prompts/08-batch3.md).
    - 00:54 E7 84ebccc: build OK, 43/43 tests ×4 targets, tools/audit_release.py PASS on fabric+forge 1.21.11 jars. Starting MP proof (fabric 1.21.11 sync).
    - 01:07 MP proof fabric 1.21.11 ABORTED: box load ~205; dedicated server watchdog killed itself (single tick 106 s). Viewer had joined (EMOTE_MP joined role=viewer). Needs a rerun with max-tick-time=-1 when load is lower.
    - 01:07 E8 done (~$2.8 est) committed 6e81597 (32 built-ins, 49 Java/35 Python tests per Codex). E9 started (batch 4, strict loader, chaining, parented props, MP timeouts; prompts/09-batch4.md).
    - 01:16 E8 6e81597: first build attempt hit a transient Loom 'Failed to remap minecraft' (shared cache under load); harness rebuild OK. h5 fabric 1.21.11: EMOTE_SUMMARY pass=59 fail=0, 87 shots (backflip/worm/floss Steve+Alex, spin, sleep, sit verified visually).
    - 01:25 E9 done (~$1.3+) committed 0a27850. E10 started (final 41-emote set, sword prop, instant keys, scale 0; prompts/10-final.md).
    - 01:25 h5 forge 1.21.1 @6e81597 FAIL: client crash ClassCastException ClientProofSupport vs ClientProofHooks$Support (E7's separate 'proof' source set lands in its own Forge module 'proof' → different classloader). Harness-only (release jars exclude proof). Queued for next Sol run.
    - 01:36 E9 0a27850: build OK; h6 fabric 1.21.11 77/0, 107 shots. E10 done (~$2.4) committed c7d6010 (42 built-ins, 69 Java/38 Python tests per Codex).
    - 01:36 KRILL STOPPED: E11 (prompts/11-forge-proof.md) refused at start — 'HTTP 403 Forbidden: account balance is negative, please recharge first' (api.kbsfdxc.net /codex/responses, 00:36:15Z = 01:36 Vienna). No more Krill runs. E10 c7d6010: build OK; h7 fabric 1.21.11 103/0 (115 shots), fabric 1.21.1 103/0 (115 shots).
    - 01:50 Parent fix (no Krill): Forge dev runs compiled the proof harness into its own module → ClassCastException. Fix in forge/build.gradle.kts (proof classes compile into main's classes dir for dev runs; the jar still excludes dev/quarkos/emotes/harness/**). h8 forge 1.21.11 103/0 (115 shots), forge 1.21.1 103/0 (115 shots). Committed 53ae332.
    - 02:01 Final build of main @53ae332: BUILD SUCCESSFUL, 69/69 unit tests on each of the 4 targets, tools/audit_release.py PASS on all 4 binary jars.

## Still open

The release gaps are the unset homepage, source, issues, Modrinth, CurseForge,
and contact URLs in [docs/RELEASE.md](docs/RELEASE.md). Do not invent them.

**Stonecutter gotcha:** on 1.21.11 any identifier containing `PlayerRenderer` is rewritten to `AvatarRenderer`. Use `getRenderer(..)` plus a cast.

## How to build and test
- Run the box's JDK25 Gradle with `--no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1536m build`. Never `gradle --stop`.
- Harness: `xvfb-run -a ./gradlew --no-daemon :<ver>:runClient` (or `:forge:<ver>:runClient`) with `-Pharness=true -Pshots=<dir> -Pwidth=1280 -Pheight=720`. Add `-Pquick=true` for a short run.
- The first run in a fresh run dir needs `options.txt` with `onboardAccessibility:false` (the harness also handles this now).
