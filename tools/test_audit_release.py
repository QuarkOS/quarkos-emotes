import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from audit_release import audit

class ReleaseAuditTests(unittest.TestCase):
    def fixture(self,path,extra=None):
        with zipfile.ZipFile(path,'w') as z:
            z.writestr('META-INF/LICENSE','MIT License')
            z.writestr('assets/quarkos_emotes/icon.png',b'icon')
            z.writestr('fabric.mod.json',json.dumps(dict(environment='*',authors=['QuarkOS'],license='MIT',version='1.0.0')))
            z.writestr('quarkos_emotes.mixins.json',json.dumps(dict(mixins=['ServerTickMixin','ServerTrackingMixin'])))
            for i in range(40):z.writestr(f'assets/quarkos_emotes/emotes/{i}.animation.json','{}')
            for name in ('pushups','meditate'):
                z.writestr(f'assets/quarkos_emotes/emotes/{name}/{name}.animation.json','{}')
                suffix='enter' if name=='pushups' else 'loop'
                z.writestr(f'assets/quarkos_emotes/emotes/{name}/{name}_{suffix}.animation.json','{}')
            if extra:z.writestr(*extra)
    def test_clean_runtime_passes_and_development_code_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'release.jar';self.fixture(p);self.assertEqual([],audit(p))
            for entry in ['dev/quarkos/emotes/harness/ClientHarness.class','tools/debug.py','test/fixture.json','projects/rig.bbmodel']:
                self.fixture(p,(entry,'dev'));self.assertTrue(any('development files' in e for e in audit(p)),entry)
    def test_archive_and_missing_chain_cannot_satisfy_release_inventory(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'release.jar'
            self.fixture(p,('assets/quarkos_emotes/emotes/_archive/sit/sit.animation.json','{}'))
            self.assertTrue(any('42 main emotes' in e for e in audit(p)))
            self.fixture(p)
            with zipfile.ZipFile(p) as z: entries={n:z.read(n) for n in z.namelist()}
            entries.pop('assets/quarkos_emotes/emotes/pushups/pushups_enter.animation.json')
            with zipfile.ZipFile(p,'w') as z:
                for name,body in entries.items():z.writestr(name,body)
            self.assertTrue(any('42 main emotes' in e for e in audit(p)))
    def test_release_requires_license_metadata_icon_and_server_hooks(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'release.jar'
            with zipfile.ZipFile(p,'w') as z:
                z.writestr('fabric.mod.json',json.dumps(dict(environment='client',authors=[],license='???',version='${version}',contact=dict(homepage='https://example.com'))))
                z.writestr('quarkos_emotes.mixins.json','{}')
            errors=audit(p)
            for expected in ['MIT','icon','environment','authors','unexpanded','placeholder','hooks','42 main emotes']:
                self.assertTrue(any(expected in e for e in errors),expected)

if __name__=='__main__':unittest.main()
