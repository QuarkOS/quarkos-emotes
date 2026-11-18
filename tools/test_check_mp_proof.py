import tempfile
import unittest
from pathlib import Path
from check_mp_proof import validate

class ProofValidationTests(unittest.TestCase):
    def fixture(self, out, mode):
        roles=['actor'] if mode=='vanilla' else ['actor','viewer']
        for role in roles:(out/f'{role}.log').write_text('EMOTE_MP_SUMMARY pass=10 fail=0\n')
        if mode=='sync':
            for role,checks in [('actor',['canSend','local_wave','movement_cancel','no_disconnect']),
                                ('viewer',['remote_wave','viewer_distance','remote_bigdance','remote_wave_stop','remote_move_stop','wave_rendered','bigdance_rendered','wave_screenshot','bigdance_screenshot','no_disconnect'])]:
                p=out/f'{role}.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP check={c} result=PASS' for c in checks))
            (out/'server.log').write_text('\n'.join(f'EMOTE_SERVER play player=A id=quarkos_emotes:{e}' for e in ['wave','bigdance'])+'\nEMOTE_SERVER stop player=A\n')
            for e in ['wave','bigdance']:
                p=out/f'shots-viewer/mp-viewer-{e}.png';p.parent.mkdir(exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
        elif mode=='late':
            for role,checks in [('actor',['canSend','local_bigdance','saw_viewer','no_disconnect']),
                                ('viewer',['remote_bigdance','late_elapsed','viewer_distance','bigdance_rendered','late_screenshot','no_disconnect'])]:
                p=out/f'{role}.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP check={c} result=PASS' for c in checks))
            (out/'server.log').write_text('EMOTE_SERVER play player=A id=quarkos_emotes:bigdance\nEMOTE_SERVER track player=A viewer=B id=quarkos_emotes:bigdance elapsed=4200\n')
            p=out/'shots-viewer/mp-viewer-late-bigdance.png';p.parent.mkdir(exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
        elif mode=='vanilla':
            p=out/'actor.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP vanilla check={c} result=PASS' for c in ['canSend_false','local_playback','hint_shown_once','hint_still_once','no_disconnect','hint_screenshot','local_rendered']))
            (out/'server.log').write_text('Done (1.2s)! For help, type "help"\n')
            p=out/'shots-actor/mp-vanilla-hint.png';p.parent.mkdir(exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
        elif mode=='life':
            for role,checks in [('actor',['canSend','local_dance','respawned','changed_dimension','returned','reconnected','reloaded','no_disconnect']),
                                ('viewer',['saw_dance','playing_shot','respawn_cleared','respawn_recovered','dimension_cleared','dimension_recovered','disconnect_cleared','reconnect_recovered','reload_continued','reload_cleared','reload_recovered','no_disconnect'])]:
                p=out/f'{role}.log';p.write_text(p.read_text()+'\n'.join(f'EMOTE_MP check={c} result=PASS' for c in checks))
            (out/'server.log').write_text('EMOTE_SERVER play player=A id=quarkos_emotes:bigdance\nEMOTE_SERVER stop player=A id=\n')
            for name in ['life-playing','life-respawn-idle','life-respawn-recovered','life-dimension-idle','life-dimension-recovered','life-disconnect-idle','life-reconnect-recovered','life-reload-continued','life-reload-idle','life-reload-recovered','store-multiplayer']:
                p=out/f'shots-viewer/{name}.png';p.parent.mkdir(exist_ok=True);p.write_bytes(b'\x89PNG\r\n\x1a\n'+bytes(100))
    def test_missing_failed_or_duplicate_summary_cannot_pass(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'sync');self.assertEqual([],validate(out,'sync'))
            for bad in ['', 'EMOTE_MP_SUMMARY pass=0 fail=0','EMOTE_MP_SUMMARY pass=10 fail=1','EMOTE_MP_SUMMARY pass=10 fail=0\n'*2]:
                (out/'viewer.log').write_text(bad);self.assertTrue(validate(out,'sync'))
    def test_headless_environment_noise_is_not_a_proof_failure(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'sync')
            noise='\n'.join([
                '[Render thread/ERROR] (Minecraft) Error starting SoundSystem. Turning off sounds & music',
                '[Download-1/ERROR] (Minecraft) Failed to retrieve profile key pair',
                '[main/ERROR] (Minecraft) No key layers in MapLike[{}]',
                '[Download-1/ERROR] [mojang/RealmsClient]: Failed to fetch Realms feature flags',
                "[IO-Worker-1/ERROR] [mojang/RealmsAvailability]: Couldn't connect to realms",
                'Server thread ERROR An exception occurred processing Appender DebugFile',
                "[main/ERROR] [minecraft/AbstractPackResources]: Couldn't load mod:quarkos_emotes pack metadata: Pack declares support for format 75, but game versions supporting formats 17 to 81 require a supported_formats field.",
            ])
            for name in ['actor.log','viewer.log','server.log']:
                p=out/name;p.write_text(p.read_text()+'\n'+noise)
            self.assertEqual([],validate(out,'sync'))
    def test_errors_relay_and_screenshots_are_required(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'sync')
            (out/'actor.log').write_text('ERROR failure\nEMOTE_MP_SUMMARY pass=10 fail=0');self.assertTrue(validate(out,'sync'))
            self.fixture(out,'sync');(out/'server.log').write_text('Done');self.assertTrue(validate(out,'sync'))
            self.fixture(out,'sync');(out/'shots-viewer/mp-viewer-wave.png').unlink();self.assertTrue(validate(out,'sync'))
    def test_late_joiner_requires_nonzero_track_elapsed_and_viewer_shot(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'late');self.assertEqual([],validate(out,'late'))
            (out/'server.log').write_text('EMOTE_SERVER play player=A id=quarkos_emotes:bigdance\nEMOTE_SERVER track player=A viewer=B id=quarkos_emotes:bigdance elapsed=0\n')
            self.assertTrue(validate(out,'late'))
            self.fixture(out,'late');(out/'shots-viewer/mp-viewer-late-bigdance.png').unlink();self.assertTrue(validate(out,'late'))
    def test_lifecycle_requires_recovery_checks_and_shots(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'life');self.assertEqual([],validate(out,'life'))
            (out/'shots-viewer/life-reload-continued.png').unlink();self.assertTrue(validate(out,'life'))
    def test_vanilla_requires_hint_and_absent_server_relay(self):
        with tempfile.TemporaryDirectory() as d:
            out=Path(d);self.fixture(out,'vanilla');self.assertEqual([],validate(out,'vanilla'))
            p=out/'actor.log';p.write_text(p.read_text().replace('hint_still_once','omitted'));self.assertTrue(validate(out,'vanilla'))

if __name__=='__main__':unittest.main()
