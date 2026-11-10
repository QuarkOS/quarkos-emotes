# QuarkOS Emotes

Editable player emotes for Minecraft by **Emilio Schwaiger / QuarkOS**,
with 41 built-in emotes by Blockbench animator **Blockato**.

The final set contains **42 built-ins: 41 animator emotes by Blockato + Stretch (ours)**.
The two chain clips are files within existing emotes, not extra IDs.

| Category | Emotes |
| --- | --- |
| Greeting (8) | `bow`, `clap`, `heart_hands`, `point`, `salute`, `sword_salute`, `wave`, `wave_big` |
| Reaction (12) | `cheer`, `cry`, `facepalm`, `jump_hype`, `laugh`, `no`, `rage`, `shrug`, `slow_clap`, `thinking`, `victory`, `yes` |
| Dance (8) | `bigdance`, `default_dance`, `drums`, `floss`, `groove`, `guitar`, `worm`, `zombie_walk` |
| Pose (9) | `kneel`, `lay_down`, `meditate`, `pushups`, `sit`, `sit_full`, `sleep`, `stretch`, `tpose` |
| Fun (5) | `backflip`, `dab`, `fishing_cast`, `spin`, `tableflip` |

Groove, bigdance, floss, zombie_walk, guitar, drums, pushups and default_dance
loop with smooth Catmull–Rom seams. Pushups plays its 1.3-second stand-to-plank
entry once before the 2-second loop. Sit, sleep, kneel and lay_down hold their
last frame; meditate plays its 5.6-second main clip then loops a 2-second breath.
Chains share one emote ID and resume the correct segment/phase for late viewers.
Handoffs have no blend; entry blends for 0.15 seconds and cancel/natural end
fades over 0.2 seconds. Sit_full retains the complete four-second sequence.
The other 29 emotes play once. Backflip and spin preserve complete Euler turns.
Backflip, spin, worm, sleep, lay_down, pushups and sword_salute use Steve arm
pivots on slim models. Tableflip includes its textured table; sword_salute now
includes the delivered textured sword with copied right-arm channels.
The 41 Blockato emotes, two chain clips and both prop pairs match the current
final delivery byte-for-byte and by MD5: **47/47 runtime files**. This includes
Floss v2 and Sword Salute v2 at their canonical paths, following `NOTES_v5.md`,
`RENAMES_v2.md`, `alex_recheck_v5.txt` and `md5_delivered.txt`.
The sword pops in at 0.25 s and out at 2.20 s; scale-zero props submit no cubes.
Stretch is the remaining original emote.

## Targets and status

Minecraft **1.21.1 and 1.21.11**, on **Fabric and Forge**, using Java **21+**.
Use the artifact matching both your loader and exact game version.
The parent reported E8 Fabric 1.21.11 in-game evidence: **59 passing / 0 failing
checks and 87 screenshots**, including Steve/Alex backflip, worm and floss,
plus spin, sleep and sit. That predates this final delivery. E10 has offline
Java/Python and four-target javac checks; final Gradle packaging, new sword/Floss
visual review and multiplayer checks remain in the
[release checklist](docs/RELEASE_CHECKLIST.md). This is release preparation.

Client installation provides local playback. A compatible server component
and shared animation set are needed for synchronized playback. Installing the
client alone does not make other players on vanilla servers see the animation.

The wheel supports categories, paging, search and favorites; settings include sounds,
movement cancellation and first-person behavior. Review current loader metadata
for dependencies. No third-party animation library is introduced by this set.

## Author or replace emotes

Read [the animation contract](docs/ANIMATION_FORMAT.md). The six player bones
are siblings; transforms add to vanilla poses and use model pivots by default; orbit emotes can opt into Steve arm pivots.
Files use numeric Bedrock animation JSON. Flat `emotes/*.json` or
`*.animation.json` and folder `emotes/<name>/<name>.animation.json` layouts work
in resource packs and `config/quarkos_emotes/emotes/`. Put metadata in
`<name>.quarkos.json` to keep Blockbench exports untouched. Props load every
same-folder `.geo.json` plus its matching PNG, bind by geo bone name, and
attach to the player root or an explicit player-bone parent. Optional
`<name>_enter.animation.json` / `<name>_loop.animation.json` are chain segments,
never separate wheel entries. Folders starting with `_` and all extra files
(sit_v1, sit_v2, clipcheck, GIF, bbmodel) are ignored.

```sh
python3 tools/validate_emotes.py
python3 tools/import_blockato.py --dry-run
python3 tools/import_blockato.py --apply
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools -v
```

The importer reads the final delivery from `/home/box/blockato/emotes/`, skips
`_archive/`, `rig/` and `tools/`, preserves sidecars and backs up replacements.
Every `--apply` recopies all selected animator runtime files, including unchanged
ones, then reports each delivery/installed MD5 match against the ledger.
Unchanged files need no backup. `--include` is available for a partial intake;
omit it for this final set. English names live in runtime `lang/en_us.json`.

## Release checks

The opt-in xvfb harness captures every `emote-<name>.png` from the front and
`emote-<name>-34.png` at a ¾ angle. Backflip and worm add side views; tableflip,
groove and bigdance add back/side views. Backflip, worm and floss each add
`emote-<name>-steve.png` and `emote-<name>-alex.png`, forcing the local player's
wide Steve / slim Alex default skin and model on both Minecraft versions.
Backflip freezes upside down (0.8 s), worm mid-wave (1.25 s), floss mid-swing
(0.25 s), spin near 270° (0.9 s) plus `emote-spin-90.png` (0.63 s), and sit/sleep
on their held seated/lying final poses. New kneel/lay_down shots hold their
final pose; pushups freezes at total elapsed 1.8 s and meditate at 6.6 s. The harness table defines every freeze time.
`fp-wave.png` / `fp-sleep.png` show animated arms; their `-camera` variants
check switching to third person and restoration after the 0.2-second cancel fade.
Show-arms mode replaces held items with animated arms; visual offsets never move
the entity, hitbox or first-person camera. Held poses are checked after anchoring;
rendered backflip X / spin Y must exceed 150° in `EMOTE_CHECK` logs.

Pass `-Pharness=true -Pshots=<directory> -PguiScale=3 -Pwidth=1280 -Pheight=720`
to a client task. The requested scale is included in `ui-wheel-s3.png`; Minecraft
may clamp its effective scale to preserve the minimum GUI size. Use distinct shot
directories for each size/scale run. `-Ponly=wave,ui-wheel,ui-wheel-hover,ui-config`
limits captures. Full runs produce 115 shots and 103 checks. `-Pquick=true` captures only
the 42 front views and four UI shots (46 shots / 51 checks). Pushups and meditate
wait for a real segment switch before freezing in their loop; require
`EMOTE_CHECK chain=... result=PASS` and a zero-failure `EMOTE_SUMMARY`.

Sword Salute adds `emote-sword_salute-{front,side,34,hidden}-{steve,alex}.png`
in full mode: visible shots at 0.8 s, hidden shots at 0.249 s. Each sword shot
checks sampled visibility and actual cube submission, including an active render
pass for the hidden assertion. Run `-Ponly=sword_salute` for all ten sword shots
(27 checks), or add `-Pquick=true` for one front shot (2 checks).
Inspect grip alignment, the provisional texture, pop boundaries and cancel/reset
on both skins. See the [release checklist](docs/RELEASE_CHECKLIST.md) for the
remaining build, visual, multiplayer and publishing work.

[Changelog](CHANGELOG.md) · [License](LICENSE) · [Handoff log](STATUS-codex.md)
