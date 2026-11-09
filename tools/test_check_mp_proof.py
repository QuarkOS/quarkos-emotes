import tempfile
import unittest
from pathlib import Path
from check_mp_proof import validate

class ProofValidationTests(unittest.TestCase):
    def fixture(self, out, mode):
        roles=['actor','viewer'] if mode=='sync' else ['actor']
        for role in roles:(out/f'{role}.log').write_text('EMOTE_MP_SUMMARY pass=10 fail=0\n')
        if mode=='sync':
            for role,checks in [('actor',['canSend','local_wave','movement_cancel','no_disconnect']),
                                ('viewer',['remote_wave','viewer_distance','remote_bigdance','remote_wave_stop','remote_move_stop','wave_rendered','bigdance_rendered','wave_screenshot','bigdance_screenshot','no_disconnect'])]:
                p=out/f'{role}.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP check={c} result=PASS' for c in checks))
            (out/'server.log').write_text('\n'.join(f'EMOTE_SERVER play player=A id=quarkos_emotes:{e}' for e in ['wave','bigdance'])+'\nEMOTE_SERVER stop player=A\n')
            for e in ['wave','bigdance']:
                p=out/f'shots-viewer/mp-viewer-{e}.png';p.parent.mkdir(exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
        else:
            p=out/'actor.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP vanilla check={c} result=PASS' for c in ['canSend_false','local_playback','hint_shown_once','hint_still_once','no_disconnect','hint_screenshot','local_rendered']))
            (out/'server.log').write_text('Done\n')
            p=out/'shots-actor/mp-vanilla-hint.png';p.parent.mkdir();p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
    def test_missing_failed_or_duplicate_summary_cannot_pass(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'sync');self.assertEqual([],validate(out,'sync'))
            for bad in ['', 'EMOTE_MP_SUMMARY pass=0 fail=0','EMOTE_MP_SUMMARY pass=10 fail=1','EMOTE_MP_SUMMARY pass=10 fail=0\n'*2]:
                (out/'viewer.log').write_text(bad);self.assertTrue(validate(out,'sync'))
    def test_errors_relay_and_screenshots_are_required(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'sync')
            (out/'actor.log').write_text('ERROR failure\nEMOTE_MP_SUMMARY pass=10 fail=0');self.assertTrue(validate(out,'sync'))
            self.fixture(out,'sync');(out/'server.log').write_text('Done');self.assertTrue(validate(out,'sync'))
            self.fixture(out,'sync');(out/'shots-viewer/mp-viewer-wave.png').unlink();self.assertTrue(validate(out,'sync'))
    def test_vanilla_requires_hint_and_absent_server_relay(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'vanilla');self.assertEqual([],validate(out,'vanilla'))
            p=out/'actor.log';p.write_text(p.read_text().replace('hint_still_once','omitted'));self.assertTrue(validate(out,'vanilla'))

if __name__=='__main__':unittest.main()
