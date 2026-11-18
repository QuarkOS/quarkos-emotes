# Dedicated multiplayer proof

All four loader/version targets passed sync, late join, and vanilla-server
proof on 2026-11-10. Fabric 1.21.11 and Forge 1.21.1 also passed lifecycle
(`life`: respawn, End dimension, disconnect/reconnect, and F3+T). Screenshots
and summaries are in [docs/proof](proof/README.md). Use a fresh output
directory for every run:

```bash
tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-fabric-sync sync 25571
tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-fabric-late late 25572
tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-fabric-vanilla vanilla 25573
```

Replace `fabric` with `forge` and/or `1.21.11` with `1.21.1` for the other targets.
The fourth argument is `sync`, `late`, `vanilla`, or `life`.
`late` starts the actor first and holds a looping bigdance; the viewer joins
afterward and must render that emote from the server's elapsed time, not from
zero. `vanilla` downloads the official Mojang server for that game version and
does not load this mod on it. The client must stay connected, play locally, and
show the one-time local-only hint.
`life` starts the viewer first. The actor dances, dies and respawns, travels
to the End and back, disconnects and reconnects, then both clients reload
resource packs. The viewer must go idle after each interruption and see the
next emote. Its reload must keep the remote dance moving. Expected summaries
are actor **8/0** and viewer **12/0**. The viewer window is 1920×1080 so
`store-multiplayer.png` can be used as a gallery frame.
The tasks are `:<version>:runServer|runClient` and
`:forge:<version>:runServer|runClient`. The script starts one dedicated server,
then B/viewer, waits for its connection, then A/actor. Vanilla mode starts only A.
It uses `xvfb-run -a`, `--no-daemon`, a 1024 MiB Gradle heap, and a 1536 MiB client
heap. Separate `run-{server,viewer,actor}` directories and Gradle project caches
prevent shared game locks and Gradle's whole-build lock. Server startup compiles
runtime/proof/resources first; client invocations reuse those outputs by excluding
their compile/resource/generation tasks, so live JVMs never race a second compiler
rewriting their class directories. Copied options disable
accessibility onboarding, focus pausing and vsync. Existing evidence is rejected.

Server runs write EULA acceptance and offline, creative, peaceful, flat-world
properties bound to **127.0.0.1**, port `-Pport` (default 25599). Harness placement
uses the server command dispatcher with server privileges, without making clients
operators. A is at (0.5,-60,0.5), B three blocks in front, looking back at A.
`-PserverHarness=true` enables placement and `EMOTE_SERVER` logs.
`-PdisableServer=true` (also the Gradle JVM system property
`-Dquarkos_emotes.disableServer=true`) forwards the disable switch to the game JVM.
The switch skips payload/channel registration; placement remains available.

Proof server properties include `max-tick-time=-1`, disabling the watchdog so
a single heavily delayed tick cannot kill the proof. Every runner timeout,
join/log wait, shutdown grace and client proof deadline defaults to **3×** the
original budget. `MP_TIMEOUT_SCALE` is a positive integer multiplier, default 3,
read by both the script and client harness (including direct Gradle runs):

```bash
MP_TIMEOUT_SCALE=6 tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-e9-sync sync
```

Gesture playback intervals stay at their authored tick lengths; timeouts and
allowed remote-stop delay scale. Use fresh evidence and a zero-failure summary.

Clients accept `-PjoinServer=127.0.0.1:25599 -Pusername=A|B
-Prole=actor|viewer -PrunDir=<absolute path> -Pshots=<absolute path>`.
`-Pvanilla=true` selects the fallback proof. Do not also pass `-Pharness=true`:
that selects the separate singleplayer visual harness and bypasses ordinary
movement cancellation. Development runs compile a separate `proof` source set;
release main/source jars omit the harness implementation. Runtime hooks load it
only when proof flags are enabled. Forge registers both source sets in its dev
mod; Fabric adds proof output to the dev run classpath.

The actor waits for B in the tab list, plays wave, then bigdance for five seconds
(over one loop), and holds the ordinary forward key to cancel. The viewer checks
remote playback, distance, actual changed bones in the remote render hook, both
stops, and completed screenshots. It never freezes or creates remote playback.
Expected summaries are actor **4/0**, viewer **10/0**. Vanilla expects **7/0**:
canSend=false, local playback and rendered hands, exactly one displayed hint
across two plays, a live connection and the completed hint screenshot.

Evidence is under OUT: `server.log`, `actor.log`, `viewer.log`, copied game logs,
`shots-viewer/mp-viewer-{wave,bigdance}.png` and
`shots-actor/mp-vanilla-hint.png`. The runner sends `stop` to the server, bounds
all processes with timeouts, cleans up its process groups, and prints summaries.
`python3 tools/check_mp_proof.py OUT sync|late|vanilla|life` independently checks complete
assertions, screenshots, server relays and absence of ERROR logs. It fails closed;
inspect and resolve any launch/environment error before accepting evidence.

After the four real builds, inspect **binary jars**, excluding sources jars:

```bash
python3 tools/audit_release.py /path/to/fabric.jar /path/to/forge.jar
```

This checks MIT/license inclusion, author/icon/loader metadata, server hooks,
exactly 42 main emotes plus two chain clips, and absence of tests, harness classes, source/dev assets
and placeholder metadata. Contact/project URLs are intentionally absent because
no authoritative release URL is supplied; add real URLs when available.

Additional parent checks: while A loops bigdance, have B leave/rejoin or move
out of/back into tracking range. `EMOTE_SERVER track` must report the active ID
and nonzero elapsed; B must resume that phase, without historical sounds. Repeat
with pushups (entry/main) and meditate (main/breathing loop) so tracking seeks
verify the correct segment and loop phase under one ID. Exercise
respawn, dimension changes, death, reconnect, F3+T during local/remote playback,
missing/changed pack IDs, camera restoration and controls conflicts on both
loaders. The default two-player flow does not automate those lifecycle scenarios.
