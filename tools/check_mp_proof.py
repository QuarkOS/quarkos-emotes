#!/usr/bin/env python3
"""Fail closed on incomplete multiplayer evidence; also usable after a manual run."""
from pathlib import Path
import re
import sys

def validate(out, mode):
    errors=[]
    roles=['actor','viewer'] if mode=='sync' else ['actor']
    for role in roles:
        log=(out/f'{role}.log').read_text()
        summaries=re.findall(r'EMOTE_MP_SUMMARY pass=(\d+) fail=(\d+)',log)
        if len(summaries)!=1 or int(summaries[0][0])==0 or int(summaries[0][1])!=0:
            errors.append(f'{role}: missing or failed summary')
        required = (['canSend','local_wave','movement_cancel','no_disconnect'] if role=='actor' else
                    ['remote_wave','viewer_distance','remote_bigdance','remote_wave_stop','remote_move_stop',
                     'wave_rendered','bigdance_rendered','wave_screenshot','bigdance_screenshot','no_disconnect']) if mode=='sync' else []
        for check in required:
            if f'EMOTE_MP check={check} result=PASS' not in log:errors.append(f'{role}: missing {check}')
        if re.search(r'\bERROR\b|EMOTE_MP .*result=FAIL',log):errors.append(f'{role}: log contains errors')
    server=(out/'server.log').read_text()
    if re.search(r'\bERROR\b',server):errors.append('server: log contains errors')
    if mode=='sync':
        for emote in ['wave','bigdance']:
            if f'EMOTE_SERVER play player=A id=quarkos_emotes:{emote}' not in server:errors.append(f'server: missing {emote} relay')
            shot=out/f'shots-viewer/mp-viewer-{emote}.png'
            if not shot.is_file() or shot.stat().st_size<100:errors.append(f'missing screenshot {shot.name}')
        if 'EMOTE_SERVER stop player=A' not in server:errors.append('server: missing stop relay')
    else:
        log=(out/'actor.log').read_text()
        for check in ['canSend_false','local_playback','hint_shown_once','hint_still_once','no_disconnect','hint_screenshot','local_rendered']:
            if f'EMOTE_MP vanilla check={check} result=PASS' not in log:errors.append(f'vanilla: missing {check}')
        if 'EMOTE_SERVER play' in server:errors.append('disabled server relayed packets')
        shot=out/'shots-actor/mp-vanilla-hint.png'
        if not shot.is_file() or shot.stat().st_size<100:errors.append('missing hint screenshot')
    return errors

if __name__=='__main__':
    if len(sys.argv)!=3 or sys.argv[2] not in ('sync','vanilla'):sys.exit('usage: check_mp_proof.py OUT sync|vanilla')
    errors=validate(Path(sys.argv[1]),sys.argv[2])
    for error in errors:print('FAIL:',error)
    print(f'EMOTE_MP_PROOF {"FAIL" if errors else "PASS"} mode={sys.argv[2]}')
    sys.exit(bool(errors))
