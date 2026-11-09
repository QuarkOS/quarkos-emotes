#!/usr/bin/env python3
"""Inspect parent-built binary jars without launching Gradle or Minecraft."""
import json
import re
import sys
import tomllib
import zipfile
from pathlib import Path

def audit(path):
    errors=[]
    with zipfile.ZipFile(path) as jar:
        names=set(jar.namelist())
        forbidden=[n for n in names if re.search(r'(^|/)(test|tests|tools|docs|build|\.git|\.gradle)/|dev/quarkos/emotes/harness/|Test\$?[^/]*\.class$|\.(java|py|sh|bbmodel|blend|psd)$',n)]
        if forbidden:errors.append('development files: '+', '.join(sorted(forbidden)))
        if 'META-INF/LICENSE' not in names or b'MIT License' not in jar.read('META-INF/LICENSE'):errors.append('missing MIT license')
        if 'assets/quarkos_emotes/icon.png' not in names:errors.append('missing icon')
        if 'fabric.mod.json' in names:
            meta=json.loads(jar.read('fabric.mod.json'))
            if meta.get('environment')!='*':errors.append('Fabric environment must support client and server')
            if not meta.get('authors'):errors.append('missing authors')
            if meta.get('license')!='MIT':errors.append('incorrect license')
            if '${' in json.dumps(meta):errors.append('unexpanded Fabric metadata')
            if 'META-INF/mods.toml' in names:errors.append('wrong-loader metadata')
        elif 'META-INF/mods.toml' in names:
            meta=tomllib.loads(jar.read('META-INF/mods.toml').decode())
            mod=meta['mods'][0]
            if mod.get('displayTest')!='IGNORE_ALL_VERSION':errors.append('missing optional-server display test')
            if not mod.get('authors'):errors.append('missing authors')
            if meta.get('license')!='MIT':errors.append('incorrect license')
            if '${' in str(meta):errors.append('unexpanded Forge metadata')
        else:errors.append('missing loader metadata')
        for n in ('fabric.mod.json','META-INF/mods.toml'):
            if n in names and re.search(r'example\.(com|org|invalid)|YOUR[_ -]|TODO|PLACEHOLDER',jar.read(n).decode(),re.I):errors.append('placeholder release metadata')
        mixins=json.loads(jar.read('quarkos_emotes.mixins.json'))
        if not {'ServerTickMixin','ServerTrackingMixin'} <= set(mixins.get('mixins',[])):errors.append('missing common server hooks')
        if len([n for n in names if n.endswith('.animation.json')])!=16:errors.append('expected 16 animations')
    return errors

if __name__=='__main__':
    if len(sys.argv)<2:sys.exit('usage: audit_release.py BINARY_JAR [...]')
    failed=False
    for name in sys.argv[1:]:
        try:errors=audit(Path(name))
        except Exception as exc:errors=[str(exc)]
        print(f'{name}: '+('FAIL: '+'; '.join(errors) if errors else 'PASS'))
        failed|=bool(errors)
    sys.exit(failed)
