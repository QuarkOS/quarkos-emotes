"""No Gradle: compile runtime and dev proof separately against this box's cached named jars.
Applies Stonecutter replacements in BOTH directions, including identifier substrings.
"""
from pathlib import Path
import re,subprocess,sys,concurrent.futures,shutil
jdk=Path('/workspace/krill-codex/jdk25/jdk-25.0.5+8/bin')
modules=Path('/home/box/.gradle/caches/modules-2/files-2.1')
common=[p for p in modules.glob('**/*.jar') if not any(s in str(p) for s in ['-sources.jar','-javadoc.jar','/net.fabricmc.fabric-api/','/maven.modrinth/','/com.terraformersmc/','/software.bernie','/net.minecraftforge/','/net.minecraftforge.forge/'])]
# Include matching Forge bootstrap/event bus artifacts per version, while retaining other shared libraries.

def preprocess(s,version,loader):
 out=[];stack=[True]
 for line in s.splitlines():
  m=re.search(r'//\?\s*(if ([^{}]+)\{|} else \{|})',line)
  if m:
   d=m.group(1)
   if d.startswith('if '):
    cond=m.group(2).strip();valid=cond==loader or cond=='>=1.21.11' and version=='1.21.11' or cond=='<1.21.11' and version=='1.21.1'
    stack.append(stack[-1] and valid)
   elif 'else' in d: stack[-1]=stack[-2] and not stack[-1]
   else:stack.pop()
   continue
  if all(stack):
   line=re.sub(r'^(\s*)/\*(?!\*)',r'\1',line)
   out.append(line)
 s='\n'.join(out)
 if version=='1.21.1':s=s.replace('Identifier','ResourceLocation').replace('AvatarRenderer','PlayerRenderer')
 else:s=s.replace('ResourceLocation','Identifier').replace('PlayerRenderer','AvatarRenderer')
 return s

def check(version,loader):
 out=Path('/tmp/emotes-check')/loader/version;src=out/'src';src.mkdir(parents=True,exist_ok=True)
 java=[];proof=[]
 shutil.rmtree(out/'classes',ignore_errors=True)
 shutil.rmtree(out/'proof-classes',ignore_errors=True)
 for p in Path('src/main/java').rglob('*.java'):
  dest=src/p.relative_to('src/main/java');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(preprocess(p.read_text(),version,loader));(proof if '/harness/' in str(p) else java).append(dest)
 cp=[]
 if loader=='fabric':
  cp.extend(Path('/home/box/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged').glob(version+'-*/*.jar'))
  token='4abd26ae' if version=='1.21.11' else '37f46004'
  cp.extend(p for p in Path('/workspace/krill-codex/cosmetics/.gradle/loom-cache/remapped_mods').rglob('*.jar') if token in str(p) and '-sources' not in p.name and ('fabric-api/' in str(p) or '/modmenu' in str(p)))
 else:
  cp.extend(Path('/home/box/.gradle/caches/minecraftforge').glob('**/forge/'+version+'-*/official/'+version+'/recompiled.jar'))
  for p in modules.glob('net.minecraftforge/*/*/*/*.jar'):
   if '-sources' in p.name:continue
   if '/1.21.' in str(p) and ('/'+version+'-') not in str(p):continue
   if '/eventbus/' in str(p) and (('/7.' in str(p))!=(version=='1.21.11')):continue
   cp.append(p)
 cp.extend((modules/"com.google.code.gson/gson/2.11.0").glob("*/*.jar"))
 cp.extend(p for p in common if '/com.mojang/authlib/' not in str(p) or ('/7.0.61/' if version=='1.21.11' else '/6.0.54/') in str(p))
 args=['--release','21','-proc:none','-cp',':'.join(map(str,cp)),'-d',str(out/'classes')]+list(map(str,java))
 argfile=out/'javac.args';argfile.write_text('\n'.join('"'+s+'"' for s in args))
 r=subprocess.run([str(jdk/'javac'),'@'+str(argfile)],capture_output=True,text=True)
 output=r.stdout+r.stderr;code=r.returncode
 if code==0:
  proofargs=['--release','21','-proc:none','-cp',':'.join(map(str,cp))+':'+str(out/'classes'),'-d',str(out/'proof-classes')]+list(map(str,proof))
  proofargsfile=out/'proof-javac.args';proofargsfile.write_text('\n'.join('"'+a+'"' for a in proofargs))
  r=subprocess.run([str(jdk/'javac'),'@'+str(proofargsfile)],capture_output=True,text=True)
  output+=r.stdout+r.stderr;code=r.returncode
 (out/'output.txt').write_text(output)
 return loader,version,code,output[:16000]
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
  results=list(pool.map(lambda p:check(*p),[(v,l) for v in ['1.21.11','1.21.1'] for l in ['fabric','forge']]))
  for result in results:print(*result,sep='\n')
  sys.exit(1 if any(r[2] for r in results) else 0)
