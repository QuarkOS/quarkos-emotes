# Dedicated multiplayer proof (E7)

Run these **as the parent**, after building the selected target. This change has
only been checked with cached-jar javac; it has not launched Gradle or Minecraft.
Use a fresh output directory for every run:

```bash
tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-e7-fabric-sync sync 25599
tools/mp_proof.sh fabric 1.21.11 /tmp/emotes-e7-fabric-vanilla vanilla 25599
```

Replace `fabric` with `forge` and/or `1.21.11` with `1.21.1` for the other targets.
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
`python3 tools/check_mp_proof.py OUT sync|vanilla` independently checks complete
assertions, screenshots, server relays and absence of ERROR logs. It fails closed;
inspect and resolve any launch/environment error before accepting evidence.

After the four real builds, inspect **binary jars**, excluding sources jars:

```bash
python3 tools/audit_release.py /path/to/fabric.jar /path/to/forge.jar
```

This checks MIT/license inclusion, author/icon/loader metadata, server hooks,
exactly 16 animations, and absence of tests, harness classes, source/dev assets
and placeholder metadata. Contact/project URLs are intentionally absent because
no authoritative release URL is supplied; add real URLs when available.

Additional parent checks: while A loops bigdance, have B leave/rejoin or move
out of/back into tracking range. `EMOTE_SERVER track` must report the active ID
and nonzero elapsed; B must resume that phase, without historical sounds. Exercise
respawn, dimension changes, death, reconnect, F3+T during local/remote playback,
missing/changed pack IDs, camera restoration and controls conflicts on both
loaders. The default two-player flow does not automate those lifecycle scenarios.
