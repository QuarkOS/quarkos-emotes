# QuarkOS Emotes

Wave, react, dance and pose with **42 built-in emotes**: **41 by Blockato**
plus Stretch, our original. Created by **Emilio Schwaiger / QuarkOS**.

**Release draft, version 1.0.0:** Minecraft **1.21.1 / 1.21.11**, **Fabric / Forge**,
Java **21+**, MIT. Final artifacts and compatibility still require the
[release checklist](../RELEASE_CHECKLIST.md). No download is announced here.

## Final emote list

| Category | Emotes |
| --- | --- |
| Greeting (8) | `bow`, `clap`, `heart_hands`, `point`, `salute`, `sword_salute`, `wave`, `wave_big` |
| Reaction (12) | `cheer`, `cry`, `facepalm`, `jump_hype`, `laugh`, `no`, `rage`, `shrug`, `slow_clap`, `thinking`, `victory`, `yes` |
| Dance (8) | `bigdance`, `default_dance`, `drums`, `floss`, `groove`, `guitar`, `worm`, `zombie_walk` |
| Pose (9) | `kneel`, `lay_down`, `meditate`, `pushups`, `sit`, `sit_full`, `sleep`, `stretch`, `tpose` |
| Fun (5) | `backflip`, `dab`, `fishing_cast`, `spin`, `tableflip` |

Eight dances/actions loop: Groove, Bigdance, Floss, Zombie Walk, Guitar, Drums,
Pushups and Default Dance. Pushups enters the plank once before looping.
Sit, Sleep, Kneel and Lay Down hold until cancelled; Meditate sits then repeats
a breathing loop. Sit Full plays its complete sequence once. The other 29
main clips play once. Both chain clips belong to existing emote IDs.

Tableflip includes a textured table. Sword Salute includes the delivered
root sword with instant scale visibility (0.25–2.20 seconds); Alex uses its
matching arm pivot. The provisional flat-colour sword texture needs final
visual approval. All 47 animator animation/chain/prop files match the final
delivery bytes and MD5, including Floss v2 and Sword Salute v2.

The wheel supports categories, paging, search and favorites. Settings include
sound, movement cancellation and first-person behavior. Numeric Bedrock
Blockbench animations can be edited through resource packs or local config;
see the [format guide](../ANIMATION_FORMAT.md). No animation library is required.

## Compatibility

**Client required; server optional.** Local emotes work on the client. Shared
playback needs compatible server synchronization and a shared animation set;
a client install alone does not animate other players on vanilla servers.
Use the artifact for your exact loader and game version. Confirm final loader
dependencies and dedicated-server behavior before publishing.

## Screenshot slots — pending final captures

| Slot | Required capture | Status |
| --- | --- | --- |
| Cover | Wheel with readable categories/search | Placeholder; no image selected |
| Actions | Steve/Alex backflip, worm and Floss v2 | Placeholder; new Floss capture required |
| Sword | Steve/Alex front, side and ¾ at 0.8 s | Placeholder; grip/texture review required |
| Sword visibility | Both skins at 0.249 s with no sword | Placeholder; harness assertion required |
| Poses | Sit, Sleep, Kneel, Lay Down held; Meditate breathing | Placeholder; final-set review required |
| Controls | Settings and first-person/camera views | Placeholder; release-sized UI capture required |

The parent reported E8 Fabric 1.21.11: 59/0 checks and 87 screenshots, including
Steve/Alex backflip/worm/floss and spin/sleep/sit. That evidence predates this
final set. E10 offline checks do not establish the new sword/Floss appearance.

**Credits:** Emilio Schwaiger / QuarkOS (project); Blockato (all 41 animator
emotes, chain clips and both props); Stretch authored with Codex assistance.
Vanilla items/sounds resolve from the user's Minecraft installation.
Independent project; not affiliated with Mojang or Microsoft.
