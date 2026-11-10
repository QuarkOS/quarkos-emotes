# Blockato final delivery manifest (E10)

Verified against the delivery in `/home/box/blockato/emotes/`, following
`RENAMES_v2.md`, `alex_recheck_v5.txt`, `NOTES_v4.md`, `NOTES_v5.md` and
`md5_delivered.txt`. Source folders `_archive/`, `rig/`, `tools/` are excluded.
The final intake has **41 emote folders**, all with canonical main animations.
With our original Stretch, this ships **42 built-in emote IDs**. See
[README](../README.md) for the complete categorized list, which is generated
from the shipped metadata. The two companion clips do not add IDs.

**47/47 runtime payloads match both source bytes and delivery MD5:** 41 main
animations, 2 chain clips (`pushups_enter`, `meditate_loop`) and 4 prop files
(table/sword geometry + PNG). Stretch makes 44 shipped animation files total.
No archival animation or animator proof file is shipped. Both Floss and Sword
Salute use v2 at their current canonical paths. `import_blockato.py --apply`
recopies every selected runtime file and reports these matches; unchanged
content needs no replacement backup. A missing/mismatched ledger entry fails
when the source includes a ledger; a generic import without one says so.

Arm pivot 22 is set by `arm_pivot: "steve"` on backflip, spin, worm, sleep,
lay_down and pushups (also governing pushups_enter), as marked `alex22` in the
v5 recheck. Sword Salute additionally uses this override as requested. All
other emotes retain model pivots (Alex 21.5). Animator JSON stays untouched.

The sword bone has no parent, pivot `[-5,22,0]`, and rotation/position tracks
identical to right_arm. Its scale pre/post keys select post at 0.25 s and 2.20 s.
The animator's `sword_salute_v2_swordcheck.json` reports zero Steve drift and
0.7918 px Alex drift with pivot 21.5. The Java transform test samples visible
frames at 100 Hz with both arm-pivot policies, confirming the copied channels
use the same transform as the hand after our Alex pivot-22 override. This is
offline transform evidence; it does not replace the in-game grip/texture review.
The delivered 32×32 sword texture is provisional artwork.

Paths below are relative to both the delivery root and the shipped `emotes/`
resource directory. MD5 is the animator's integrity convention; exact byte
comparison was performed independently.

| Runtime file | MD5 |
| --- | --- |
| `backflip/backflip.animation.json` | `d406d5a638c8957289a3d4f71c91988d` |
| `bigdance/bigdance.animation.json` | `b0e5d2a294b0bc496598246a0c704259` |
| `bow/bow.animation.json` | `b7621a538d3d348ef2189dfc8862dfff` |
| `cheer/cheer.animation.json` | `98649ac5ab3286356b66f1889edd4999` |
| `clap/clap.animation.json` | `c185871ccde1dc4473491f52d3c67d36` |
| `cry/cry.animation.json` | `d8c1305a04c2f8982ec66b97d3aa709c` |
| `dab/dab.animation.json` | `26f090a9241c170729e2907fecab2e5b` |
| `default_dance/default_dance.animation.json` | `87db8a6afdea0094eb56e215cfc2d8d3` |
| `drums/drums.animation.json` | `7cb0de3fcaa012f5d258a86f2406cb75` |
| `facepalm/facepalm.animation.json` | `5ac62c090b9a38affc5112de0b33594d` |
| `fishing_cast/fishing_cast.animation.json` | `b41f8c8be071ba46ce9a0bfef6c98348` |
| `floss/floss.animation.json` | `415f35b4b7a8139df16e3c7827aedf83` |
| `groove/groove.animation.json` | `f7f355f34ffa31c58f21f6720546fa3e` |
| `guitar/guitar.animation.json` | `21f1833be76c1dd75f7b1c026fff5c21` |
| `heart_hands/heart_hands.animation.json` | `8e617a6debfa477822e16d65fae14fc0` |
| `jump_hype/jump_hype.animation.json` | `5a927e83d14064a5320cb5aa863c3e85` |
| `kneel/kneel.animation.json` | `e9a7c44fc2ee95d046a2ed7b81dfa15f` |
| `laugh/laugh.animation.json` | `4a5c812d7c0313741672512ed6516ff4` |
| `lay_down/lay_down.animation.json` | `0fc56c3a9bb4ce7a9cfeddad75cfbbbc` |
| `meditate/meditate.animation.json` | `654bf3cce51161422a4c49f2ca7febfe` |
| `meditate/meditate_loop.animation.json` | `411f3552ec1789e467c32deca5dbea85` |
| `no/no.animation.json` | `3c8ef6c442ad1c15408ce676e2c5a852` |
| `point/point.animation.json` | `03abddab9ae67ffb6171ed9956a1f177` |
| `pushups/pushups.animation.json` | `8e24d9d76d761118c52a0c87f918ac3e` |
| `pushups/pushups_enter.animation.json` | `5b075f06180660b1fe8fc67581dc4a2e` |
| `rage/rage.animation.json` | `e05e99931d3509d204c8481ddef257c6` |
| `salute/salute.animation.json` | `73b0abc6e5846b4452e6db4cbfb851ed` |
| `shrug/shrug.animation.json` | `99829152b2794327729a85a8ae348b4b` |
| `sit/sit.animation.json` | `5d63a8701e77ff864b762638fbf6901d` |
| `sit_full/sit_full.animation.json` | `9869aca5f3e90a492821b7f681c7d522` |
| `sleep/sleep.animation.json` | `77293a800de95f27cd860c8fba95c58a` |
| `slow_clap/slow_clap.animation.json` | `929bcd08b19f2a2ede880574ad674007` |
| `spin/spin.animation.json` | `e257ea69138e476d17cc927cac7fdf76` |
| `sword_salute/sword_prop.geo.json` | `d32b8322dd9f8e1d1860f79a01ad38f5` |
| `sword_salute/sword_prop.png` | `7cc0f394655777c11f19777609427b34` |
| `sword_salute/sword_salute.animation.json` | `9c0b5256b8bd1fb05909eb2e8e55a79e` |
| `tableflip/table_prop.geo.json` | `94a637a31db2c9545d1c75be6d408127` |
| `tableflip/table_prop.png` | `c9e8a15fd43840d27310a30263bc4bc1` |
| `tableflip/tableflip.animation.json` | `12cb258c8833bee7661593db5395c0fe` |
| `thinking/thinking.animation.json` | `be2366bee4f5432e6db1e88aae8e86f3` |
| `tpose/tpose.animation.json` | `af19d2c8f4e3b2c52b8521660ceea672` |
| `victory/victory.animation.json` | `1ee11b67220438364655ef44520f7c72` |
| `wave/wave.animation.json` | `01d43bc2b0d97ae4558668404c7a4431` |
| `wave_big/wave_big.animation.json` | `35d29474b799a91960c36944604b3834` |
| `worm/worm.animation.json` | `49c617b60ad52ffb32414293d4970022` |
| `yes/yes.animation.json` | `c2b2805c4b9c6d94923afbd55bfc8f57` |
| `zombie_walk/zombie_walk.animation.json` | `c6bc2ad1f3c6d98c2f90077fbfa31d31` |
