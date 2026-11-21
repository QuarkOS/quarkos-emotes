# Quark's Emotes — status (2026-11-10, v1.0.0)

Nothing has been published to Modrinth, CurseForge, or GitHub. The version in
`stonecutter.properties.toml` is `1.0.0`. Values still required before a public
release are listed in [RELEASE.md](RELEASE.md). Do not invent them.

## Multiplayer

`tools/check_mp_proof.py` returned `EMOTE_MP_PROOF PASS` for every row.
Screenshots are from the watching client. Evidence is in `docs/proof/mp/`.

| Target | Sync | Late join | Vanilla server |
| --- | --- | --- | --- |
| Fabric 1.21.11 | actor 4/0, viewer 10/0 | actor 4/0, viewer 6/0, elapsed 63049 ms | actor 7/0, no kick, local-only hint |
| Fabric 1.21.1 | actor 4/0, viewer 10/0 | actor 4/0, viewer 6/0, elapsed 59790 ms | actor 7/0, no kick, local-only hint |
| Forge 1.21.11 | actor 4/0, viewer 10/0 | actor 4/0, viewer 6/0, elapsed 63086 ms | actor 7/0, no kick, local-only hint |
| Forge 1.21.1 | actor 4/0, viewer 10/0 | actor 4/0, viewer 6/0, elapsed 61180 ms | actor 7/0, no kick, local-only hint |

A late joiner renders the looping big dance from the server's elapsed time.
A client on a vanilla server stays connected, plays locally, and shows the
local-only hint once.

## Lifecycle

`tools/mp_proof.sh <loader> <version> <dir> life <port>` on Fabric 1.21.11 and
Forge 1.21.1. Actor 8/0 and viewer 12/0 on both. The viewer frames are
1920×1080 with the HUD, chat, and toasts hidden.

| Case | What the viewer recorded |
| --- | --- |
| Respawn mid-emote | Idle after the respawn, then a new wave. No stuck pose. |
| Dimension change mid-emote | The player leaves for the End and returns idle, then waves. |
| Disconnect / reconnect | The player is gone, comes back idle, then dances. |
| F3+T mid-emote | The viewer's reload keeps the remote dance moving. The actor's reload clears it, and a later wave is seen again. |

`RemoteApply.dropOnEntityReplacement` drops a remote pose when the entity
object is replaced or is dead. The first sight of an entity does not drop it.
`RemoteApplyTest` covers that rule. Fabric 1.21.1 and Forge 1.21.11 were not
part of the lifecycle pass.

## Store assets

Eight 1920×1080 frames in `docs/store/shots/`: the wheel, wave on Steve, floss
on Alex, backflip on Steve, meditate on Steve, sword salute on Steve and Alex,
and the Fabric 1.21.11 multiplayer dance. No debug screen, chat, or harness
chrome. `docs/store/icon-512.png` is the mod icon at 512×512.

## Visual harness

Fabric 1.21.11 after the framing and wheel changes: `EMOTE_SUMMARY pass=103
fail=0`, 138 shots in `docs/proof/fabric-1.21.11/`. Animation JSON files were
not edited.

## Forge 1.21.11 pack metadata

The client resource pack uses `min_format` / `max_format` `[75, 0]`. That loads
the 42 emotes. The same file is also read as a data pack, whose compatibility
cutoff is 81, so a Forge 1.21.11 dedicated server logs one
`supported_formats` error at startup. Adding that key makes the 1.21.11 client
reject the pack. The dedicated-server proof still relays emotes. The checker
allowlists that one line.
