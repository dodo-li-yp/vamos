#!/usr/bin/env python3
# Modified by Vamos (2026): reproducible local UI subset of OFL Noto Serif SC.
"""Requires fonttools. Supply NotoSerifSC[wght].ttf from google/fonts/ofl/notoserifsc."""
import argparse
from pathlib import Path
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('source',type=Path)
a=p.parse_args()
root=Path(__file__).resolve().parents[2]
text=''.join(f.read_text() for folder in ['app/src/main','docs/vamos'] for f in (root/folder).rglob('*') if f.suffix in ['.kt','.xml','.md'])
unicodes=set(map(ord,text)) | set(range(32,256)) | set(range(0x3000,0x3040))
for weight,name in [(500,'medium'),(600,'semibold')]:
 font=TTFont(a.source)
 instantiateVariableFont(font,{'wght':weight},inplace=True)
 sub=subset.Subsetter(options=subset.Options())
 sub.populate(unicodes=unicodes)
 sub.subset(font)
 font.save(root/f'app/src/main/res/font/vamos_serif_{name}.ttf')
print('Noto Serif SC UI subset generated at weights 500/600. Unavailable glyphs use Android fallback.')
