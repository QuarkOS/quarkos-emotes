# Quark's Emotes

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

## Screenshots

Gallery frames are 1920×1080 with no debug screen, chat, or harness chrome.
The icon is [icon-512.png](icon-512.png). Proof frames remain in
[docs/proof](../proof/README.md).

| Slot | Capture |
| --- | --- |
| Wheel | [store-wheel.png](shots/store-wheel.png) |
| Wave, Steve | [store-wave-steve.png](shots/store-wave-steve.png) |
| Floss, Alex | [store-floss-alex.png](shots/store-floss-alex.png) |
| Backflip, Steve | [store-backflip-steve.png](shots/store-backflip-steve.png) |
| Meditate, Steve | [store-meditate-steve.png](shots/store-meditate-steve.png) |
| Sword salute, Steve | [store-sword-steve.png](shots/store-sword-steve.png) |
| Sword salute, Alex | [store-sword-alex.png](shots/store-sword-alex.png) |
| Multiplayer | [store-multiplayer.png](shots/store-multiplayer.png) |

**Credits:** Emilio Schwaiger / QuarkOS (project); Blockato (all 41 animator
emotes, chain clips and both props); Stretch authored with Codex assistance.
Vanilla items/sounds resolve from the user's Minecraft installation.
Independent project; not affiliated with Mojang or Microsoft.
