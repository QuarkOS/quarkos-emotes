#!/usr/bin/env bash
# Parent-only live proof: tools/mp_proof.sh fabric|forge 1.21.1|1.21.11 OUT [sync|late|vanilla] [PORT]
# sync: modded server, viewer joins, then actor plays wave and bigdance.
# late: modded server, actor loops bigdance, then viewer joins and catches the phase.
# vanilla: official Mojang server (no mod), one modded client plays locally and is not kicked.
# life: modded server, actor respawns, changes dimension, reconnects, and both clients reload.
set -euo pipefail
loader=${1:?loader required}; version=${2:?version required}; destination=${3:?output directory required}
mode=${4:-sync}; port=${5:-25599}
export MP_TIMEOUT_SCALE=${MP_TIMEOUT_SCALE:-3}
[[ $MP_TIMEOUT_SCALE =~ ^[1-9][0-9]*$ ]] || { echo 'MP_TIMEOUT_SCALE must be a positive integer' >&2; exit 2; }
[[ $loader == fabric || $loader == forge ]] || { echo 'loader must be fabric or forge' >&2; exit 2; }
[[ $version == 1.21.1 || $version == 1.21.11 ]] || { echo 'unsupported version' >&2; exit 2; }
[[ $mode == sync || $mode == vanilla || $mode == late || $mode == life ]] || { echo 'mode must be sync, late, vanilla, or life' >&2; exit 2; }
[[ $port =~ ^[0-9]+$ ]] && ((port > 0 && port < 65536)) || { echo 'invalid port' >&2; exit 2; }
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
mkdir -p "$destination"
out=$(cd "$destination" && pwd)
# A fresh evidence directory prevents stale logs/screenshots from satisfying checks.
[[ ! -e $out/server.log && ! -e $out/actor.log ]] || { echo 'use a fresh output directory' >&2; exit 2; }
for cmd in xvfb-run timeout setsid rg python3 java; do command -v "$cmd" >/dev/null; done
prefix=":$version"; [[ $loader == fabric ]] || prefix=":forge:$version"
common=(--no-daemon --console=plain -Dorg.gradle.jvmargs=-Xmx1024m -PgameMemory=1024M)
pids=(); server_pid=; viewer_pid=; actor_pid=
stop_server() {
    if [[ -n $server_pid ]]; then
        printf 'stop\n' >&3 || true
        for ((i=0;i<20*MP_TIMEOUT_SCALE;i++)); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
        if kill -0 "$server_pid" 2>/dev/null; then
            kill -TERM -- "-$server_pid" 2>/dev/null || true
            sleep 1
            kill -KILL -- "-$server_pid" 2>/dev/null || true
        fi
        wait "$server_pid" 2>/dev/null || true
        server_pid=
    fi
}
cleanup() {
    trap - EXIT INT TERM
    stop_server
    for pid in "${pids[@]}"; do kill -TERM -- "-$pid" 2>/dev/null || true; done
    for pid in "${pids[@]}"; do kill -KILL -- "-$pid" 2>/dev/null || true; done
    for pid in "${pids[@]}"; do wait "$pid" 2>/dev/null || true; done
    for instance in server actor viewer; do
        if [[ -d $out/run-$instance/logs ]]; then mkdir -p "$out/logs-$instance"; cp -a "$out/run-$instance/logs/." "$out/logs-$instance/"; fi
    done
    rm -f "$out/server.stdin"
}
trap cleanup EXIT
trap 'exit 130' INT TERM
for instance in server actor viewer; do
    mkdir -p "$out/run-$instance" "$out/shots-$instance" "$out/cache-$instance"
    seed="$root/run/options.txt"
    [[ ! -f $seed ]] || cp "$seed" "$out/run-$instance/options.txt"
    python3 - "$out/run-$instance/options.txt" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); lines=p.read_text().splitlines() if p.exists() else []
settings=dict(onboardAccessibility='false',pauseOnLostFocus='false',enableVsync='false',maxFps='30',renderDistance='6',simulationDistance='6',guiScale='2',mipmapLevels='0',narrator='0',fullscreen='false')
lines=[line for line in lines if line.split(':',1)[0] not in settings]
p.write_text('\n'.join(lines+[f'{k}:{v}' for k,v in settings.items()])+'\n')
PY
done
await_log() {
    local log=$1 pattern=$2 pid=$3 limit=$(($4 * MP_TIMEOUT_SCALE))
    for ((i=0;i<limit;i++)); do
        if [[ -f $log ]] && rg -q "$pattern" "$log"; then return; fi
        if ! kill -0 "$pid" 2>/dev/null; then tail -60 "$log"; return 1; fi
        sleep 1
    done
    echo "timeout waiting for $pattern in $log" >&2; tail -60 "$log"; return 1
}
mkfifo "$out/server.stdin"
exec 3<>"$out/server.stdin"
start_modded_server() {
    # Separate project caches avoid Gradle's whole-build lock while the JavaExec tasks run.
    local clean=false; [[ $mode == life ]] && clean=true
    setsid timeout "$((900 * MP_TIMEOUT_SCALE))" ./gradlew "${common[@]}" --project-cache-dir "$out/cache-server" "$prefix:runServer" \
        "-PrunDir=$out/run-server" "-Pport=$port" -PserverHarness=true -PdisableServer=false "-PcleanNames=$clean" \
        <"$out/server.stdin" >"$out/server.log" 2>&1 &
    server_pid=$!; pids+=("$server_pid")
    await_log "$out/server.log" 'Done \(' "$server_pid" 360
}
start_vanilla_server() {
    local cache="/tmp/mc-server-$version.jar" dir="$out/vanilla-server"
    if [[ ! -s $cache ]]; then
        python3 - "$version" "$cache" <<'PY'
import json, sys, urllib.request
version, dest = sys.argv[1], sys.argv[2]
manifest = json.load(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", timeout=90))
meta_url = next(v["url"] for v in manifest["versions"] if v["id"] == version)
meta = json.load(urllib.request.urlopen(meta_url, timeout=90))
url = meta["downloads"]["server"]["url"]
print("downloading", url, flush=True)
urllib.request.urlretrieve(url, dest)
PY
    fi
    mkdir -p "$dir"
    printf 'eula=true\n' > "$dir/eula.txt"
    cat > "$dir/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
prevent-proxy-connections=false
server-ip=127.0.0.1
server-port=$port
level-type=minecraft:flat
gamemode=creative
force-gamemode=true
difficulty=peaceful
max-tick-time=-1
spawn-protection=0
view-distance=6
simulation-distance=6
max-players=4
enable-status=true
white-list=false
motd=QuarkOS vanilla proof
EOF
    setsid timeout "$((900 * MP_TIMEOUT_SCALE))" bash -c 'cd "$1" && exec java -Xmx1024M -jar "$2" nogui' _ "$dir" "$cache" \
        <"$out/server.stdin" >"$out/server.log" 2>&1 &
    server_pid=$!; pids+=("$server_pid")
}
start_client() {
    local role=$1 username=$2 vanilla=$3 late=${4:-false} life=false
    [[ $mode == life ]] && life=true
    local width=1280 height=720 screen='1280x720x24'
    # The lifecycle viewer frames are also the clean multiplayer store shot.
    if [[ $mode == life && $role == viewer ]]; then width=1920; height=1080; screen='1920x1080x24'; fi
    # runServer has already compiled runtime + proof + resources. Keep those
    # shared outputs immutable while live JVMs load classes from them.
    # Vanilla mode has no modded server, so the client compiles itself.
    local reuse=()
    if [[ $mode != vanilla ]]; then
        reuse=(-x "$prefix:compileJava" -x "$prefix:compileProofJava"
               -x "$prefix:processResources" -x "$prefix:processProofResources"
               -x "$prefix:stonecutterGenerate")
    fi
    setsid timeout "$((480 * MP_TIMEOUT_SCALE))" xvfb-run -a -s "-screen 0 $screen" ./gradlew "${common[@]}" \
        --project-cache-dir "$out/cache-$role" "${reuse[@]}" "$prefix:runClient" "-PrunDir=$out/run-$role" \
        "-PjoinServer=127.0.0.1:$port" "-Pusername=$username" "-Prole=$role" "-Pvanilla=$vanilla" "-Plate=$late" "-Plife=$life" \
        "-Pshots=$out/shots-$role" -PguiScale=2 "-Pwidth=$width" "-Pheight=$height" >"$out/$role.log" 2>&1 &
    client_pid=$!; pids+=("$client_pid")
}
if [[ $mode == vanilla ]]; then
    start_vanilla_server
    await_log "$out/server.log" 'Done \(' "$server_pid" 360
else
    start_modded_server
fi
if [[ $mode == sync ]]; then
    start_client viewer B false false; viewer_pid=$client_pid
    await_log "$out/viewer.log" 'EMOTE_MP joined role=viewer' "$viewer_pid" 360
    start_client actor A false false; actor_pid=$client_pid
    await_log "$out/actor.log" 'EMOTE_MP_SUMMARY' "$actor_pid" 420
    wait "$actor_pid"
    await_log "$out/viewer.log" 'EMOTE_MP_SUMMARY' "$viewer_pid" 90
    wait "$viewer_pid"
elif [[ $mode == late ]]; then
    start_client actor A false true; actor_pid=$client_pid
    await_log "$out/actor.log" 'EMOTE_MP sent id=quarkos_emotes:bigdance' "$actor_pid" 420
    sleep 6
    start_client viewer B false true; viewer_pid=$client_pid
    await_log "$out/viewer.log" 'EMOTE_MP_SUMMARY' "$viewer_pid" 420
    wait "$viewer_pid"
    await_log "$out/actor.log" 'EMOTE_MP_SUMMARY' "$actor_pid" 180
    wait "$actor_pid"
elif [[ $mode == life ]]; then
    start_client viewer B false false; viewer_pid=$client_pid
    await_log "$out/viewer.log" 'EMOTE_MP joined role=viewer' "$viewer_pid" 360
    start_client actor A false false; actor_pid=$client_pid
    await_log "$out/actor.log" 'EMOTE_MP_SUMMARY' "$actor_pid" 900
    wait "$actor_pid"
    await_log "$out/viewer.log" 'EMOTE_MP_SUMMARY' "$viewer_pid" 180
    wait "$viewer_pid"
else
    start_client actor A true false; actor_pid=$client_pid
    await_log "$out/actor.log" 'EMOTE_MP_SUMMARY' "$actor_pid" 420
    wait "$actor_pid"
fi
stop_server
rg 'EMOTE_MP_SUMMARY|EMOTE_MP vanilla|EMOTE_SERVER|EMOTE_REMOTE' "$out"/*.log || true
python3 tools/check_mp_proof.py "$out" "$mode"
