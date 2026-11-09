#!/usr/bin/env bash
# Parent-only live proof: tools/mp_proof.sh fabric|forge 1.21.1|1.21.11 OUT [sync|vanilla] [PORT]
set -euo pipefail
loader=${1:?loader required}; version=${2:?version required}; destination=${3:?output directory required}
mode=${4:-sync}; port=${5:-25599}
[[ $loader == fabric || $loader == forge ]] || { echo 'loader must be fabric or forge' >&2; exit 2; }
[[ $version == 1.21.1 || $version == 1.21.11 ]] || { echo 'unsupported version' >&2; exit 2; }
[[ $mode == sync || $mode == vanilla ]] || { echo 'mode must be sync or vanilla' >&2; exit 2; }
[[ $port =~ ^[0-9]+$ ]] && ((port > 0 && port < 65536)) || { echo 'invalid port' >&2; exit 2; }
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
mkdir -p "$destination"
out=$(cd "$destination" && pwd)
# A fresh evidence directory prevents stale logs/screenshots from satisfying checks.
[[ ! -e $out/server.log && ! -e $out/actor.log ]] || { echo 'use a fresh output directory' >&2; exit 2; }
for cmd in xvfb-run timeout setsid rg python3; do command -v "$cmd" >/dev/null; done
prefix=":$version"; [[ $loader == fabric ]] || prefix=":forge:$version"
common=(--no-daemon --console=plain -Dorg.gradle.jvmargs=-Xmx1024m)
pids=(); server_pid=; viewer_pid=; actor_pid=
stop_server() {
    if [[ -n $server_pid ]]; then
        printf 'stop\n' >&3 || true
        for ((i=0;i<20;i++)); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
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
settings=dict(onboardAccessibility='false',pauseOnLostFocus='false',enableVsync='false',maxFps='30',renderDistance='6',simulationDistance='4',guiScale='2',mipmapLevels='0',narrator='0',fullscreen='false')
lines=[line for line in lines if line.split(':',1)[0] not in settings]
p.write_text('\n'.join(lines+[f'{k}:{v}' for k,v in settings.items()])+'\n')
PY
done
await_log() {
    local log=$1 pattern=$2 pid=$3 limit=$4
    for ((i=0;i<limit;i++)); do
        if [[ -f $log ]] && rg -q "$pattern" "$log"; then return; fi
        if ! kill -0 "$pid" 2>/dev/null; then tail -60 "$log"; return 1; fi
        sleep 1
    done
    echo "timeout waiting for $pattern in $log" >&2; tail -60 "$log"; return 1
}
mkfifo "$out/server.stdin"
exec 3<>"$out/server.stdin"
disabled=false; [[ $mode != vanilla ]] || disabled=true
# Separate project caches avoid Gradle's whole-build lock while the three JavaExec tasks run.
setsid timeout 900 ./gradlew "${common[@]}" --project-cache-dir "$out/cache-server" "$prefix:runServer" \
    "-PrunDir=$out/run-server" "-Pport=$port" -PserverHarness=true "-PdisableServer=$disabled" \
    <"$out/server.stdin" >"$out/server.log" 2>&1 &
server_pid=$!; pids+=("$server_pid")
await_log "$out/server.log" 'Done \(' "$server_pid" 360
start_client() {
    local role=$1 username=$2 vanilla=$3
    # runServer has already compiled runtime + proof + resources. Keep those
    # shared outputs immutable while live JVMs load classes from them.
    local reuse=(-x "$prefix:compileJava" -x "$prefix:compileProofJava"
                 -x "$prefix:processResources" -x "$prefix:processProofResources"
                 -x "$prefix:stonecutterGenerate")
    setsid timeout 480 xvfb-run -a -s '-screen 0 960x540x24' ./gradlew "${common[@]}" \
        --project-cache-dir "$out/cache-$role" "${reuse[@]}" "$prefix:runClient" "-PrunDir=$out/run-$role" \
        "-PjoinServer=127.0.0.1:$port" "-Pusername=$username" "-Prole=$role" "-Pvanilla=$vanilla" \
        "-Pshots=$out/shots-$role" -PguiScale=2 >"$out/$role.log" 2>&1 &
    client_pid=$!; pids+=("$client_pid")
}
if [[ $mode == sync ]]; then
    start_client viewer B false; viewer_pid=$client_pid
    await_log "$out/viewer.log" 'EMOTE_MP joined role=viewer' "$viewer_pid" 360
fi
start_client actor A "$disabled"; actor_pid=$client_pid
await_log "$out/actor.log" 'EMOTE_MP_SUMMARY' "$actor_pid" 420
wait "$actor_pid"
if [[ -n $viewer_pid ]]; then
    await_log "$out/viewer.log" 'EMOTE_MP_SUMMARY' "$viewer_pid" 60
    wait "$viewer_pid"
fi
stop_server
rg 'EMOTE_MP_SUMMARY|EMOTE_MP vanilla|EMOTE_SERVER' "$out"/*.log
python3 tools/check_mp_proof.py "$out" "$mode"
