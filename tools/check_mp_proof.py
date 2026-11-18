#!/usr/bin/env python3
"""Fail closed on incomplete multiplayer evidence; also usable after a manual run."""
from pathlib import Path
import re
import sys

# Headless xvfb and offline servers log these without affecting emote sync.
ENVIRONMENT_ERRORS = (
    'Error parsing option value',
    'Error starting SoundSystem',
    'Failed to open OpenAL',
    'Failed to retrieve profile key pair',
    'GL ERROR',
    '@ Render',
    'Standard cursor shape unavailable',
    'No key layers in MapLike',
    'Could not authorize you against Realms',
    'Failed to fetch Realms feature flags',
    "Couldn't connect to realms",
    'An exception occurred processing Appender DebugFile',
    # 1.21.11 reads one pack.mcmeta as a resource pack (cutoff 64) and a data
    # pack (cutoff 81). The resource range that loads emotes makes the dedicated
    # server ask for supported_formats; adding that key makes the client reject it.
    'require a supported_formats field',
)

def gameplay_log(text):
    return '\n'.join(line for line in text.splitlines() if not any(noise in line for noise in ENVIRONMENT_ERRORS))

def validate(out, mode):
    errors=[]
    roles=['actor'] if mode=='vanilla' else ['actor','viewer']
    for role in roles:
        log=gameplay_log((out/f'{role}.log').read_text())
        summaries=re.findall(r'EMOTE_MP_SUMMARY pass=(\d+) fail=(\d+)',log)
        if len(summaries)!=1 or int(summaries[0][0])==0 or int(summaries[0][1])!=0:
            errors.append(f'{role}: missing or failed summary')
        if mode=='sync':
            required = (['canSend','local_wave','movement_cancel','no_disconnect'] if role=='actor' else
                        ['remote_wave','viewer_distance','remote_bigdance','remote_wave_stop','remote_move_stop',
                         'wave_rendered','bigdance_rendered','wave_screenshot','bigdance_screenshot','no_disconnect'])
        elif mode=='late':
            required = (['canSend','local_bigdance','saw_viewer','no_disconnect'] if role=='actor' else
                        ['remote_bigdance','late_elapsed','viewer_distance','bigdance_rendered','late_screenshot','no_disconnect'])
        elif mode=='life':
            required = (['canSend','local_dance','respawned','changed_dimension','returned','reconnected','reloaded','no_disconnect'] if role=='actor' else
                        ['saw_dance','playing_shot','respawn_cleared','respawn_recovered','dimension_cleared','dimension_recovered',
                         'disconnect_cleared','reconnect_recovered','reload_continued','reload_cleared','reload_recovered','no_disconnect'])
        else:
            required=[]
        for check in required:
            if f'EMOTE_MP check={check} result=PASS' not in log:errors.append(f'{role}: missing {check}')
        if re.search(r'\bERROR\b|EMOTE_MP .*result=FAIL',log):errors.append(f'{role}: log contains errors')
    server=gameplay_log((out/'server.log').read_text())
    if re.search(r'\bERROR\b',server):errors.append('server: log contains errors')
    if mode=='sync':
        for emote in ['wave','bigdance']:
            if f'EMOTE_SERVER play player=A id=quarkos_emotes:{emote}' not in server:errors.append(f'server: missing {emote} relay')
            shot=out/f'shots-viewer/mp-viewer-{emote}.png'
            if not shot.is_file() or shot.stat().st_size<100:errors.append(f'missing screenshot {shot.name}')
        if 'EMOTE_SERVER stop player=A' not in server:errors.append('server: missing stop relay')
    elif mode=='late':
        if 'EMOTE_SERVER play player=A id=quarkos_emotes:bigdance' not in server:errors.append('server: missing bigdance relay')
        tracked=re.findall(r'EMOTE_SERVER track player=A viewer=B id=quarkos_emotes:bigdance elapsed=(\d+)',server)
        if not tracked or max(int(v) for v in tracked)<1000:errors.append('server: late track elapsed')
        shot=out/'shots-viewer/mp-viewer-late-bigdance.png'
        if not shot.is_file() or shot.stat().st_size<100:errors.append('missing late screenshot')
    elif mode=='vanilla':
        log=(out/'actor.log').read_text()
        for check in ['canSend_false','local_playback','hint_shown_once','hint_still_once','no_disconnect','hint_screenshot','local_rendered']:
            if f'EMOTE_MP vanilla check={check} result=PASS' not in log:errors.append(f'vanilla: missing {check}')
        if 'EMOTE_SERVER play' in server:errors.append('vanilla server relayed emote packets')
        if 'Done (' not in server:errors.append('vanilla server did not finish starting')
        shot=out/'shots-actor/mp-vanilla-hint.png'
        if not shot.is_file() or shot.stat().st_size<100:errors.append('missing hint screenshot')
    elif mode=='life':
        if 'EMOTE_SERVER play player=A id=quarkos_emotes:bigdance' not in server:errors.append('server: missing dance relay')
        if 'EMOTE_SERVER stop player=A' not in server:errors.append('server: missing stop relay')
        for name in ['life-playing','life-respawn-idle','life-respawn-recovered','life-dimension-idle','life-dimension-recovered',
                     'life-disconnect-idle','life-reconnect-recovered','life-reload-continued','life-reload-idle','life-reload-recovered','store-multiplayer']:
            shot=out/f'shots-viewer/{name}.png'
            if not shot.is_file() or shot.stat().st_size<100:errors.append(f'missing screenshot {shot.name}')
    return errors

if __name__=='__main__':
    if len(sys.argv)!=3 or sys.argv[2] not in ('sync','vanilla','late','life'):sys.exit('usage: check_mp_proof.py OUT sync|vanilla|late|life')
    errors=validate(Path(sys.argv[1]),sys.argv[2])
    for error in errors:print('FAIL:',error)
    print(f'EMOTE_MP_PROOF {"FAIL" if errors else "PASS"} mode={sys.argv[2]}')
    sys.exit(bool(errors))
