#!/usr/bin/env python3
# Modified by Vamos (2026): reproducible V1 APK inventory and known-runtime exclusion audit.
"""Audit the debug APK without installing it. This checks inventory, not every dependency's legal provenance."""
import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path


def audit(path, aapt=None):
    with zipfile.ZipFile(path) as apk:
        names = apk.namelist()
        forbidden = ('assets/runtime/', 'assets/sandbox/', 'rootfs', 'libproot', 'talloc',
                     'libvamosrun', 'run_command.md', 'run_bundled_script.md', 'extract_pdf.py')
        bad = [n for n in names if any(term in n.lower() for term in forbidden)]
        allowed_native = {'libandroidx.graphics.path.so', 'libmlkit_google_ocr_pipeline.so'}
        native = sorted(n for n in names if n.startswith('lib/') and n.endswith('.so'))
        bad += [n for n in native if Path(n).name not in allowed_native]
        prohibited_classes = (b'Lorg/apache/commons/compress/', b'Lorg/tukaani/xz/',
                              b'Lcom/yongpeng/vamos/cap/sandbox/', b'Lcom/yongpeng/vamos/core/cap/sandbox/',
                              b'Lcom/google/firebase/analytics/', b'Lcom/google/firebase/crashlytics/',
                              b'Lio/sentry/', b'Lcom/bugsnag/')
        for n in names:
            if n.endswith('.dex'):
                dex = apk.read(n)
                if any(marker in dex for marker in prohibited_classes):
                    bad.append(n + ': removed runtime or prohibited reporting SDK class found')
        if bad:
            raise ValueError('V1 APK audit failed: ' + ', '.join(bad))
        required = {'assets/licenses/Apache-2.0.txt', 'assets/licenses/NOTICE.txt',
                    'assets/licenses/THIRD_PARTY_NOTICES.md', 'assets/licenses/CC0-1.0.txt',
                    'assets/licenses/OFL-Noto-Serif.txt'}
        if not required.issubset(names):
            raise ValueError('Missing license assets: ' + ', '.join(sorted(required-set(names))))
        result = dict(size=path.stat().st_size, sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                      native=native, assets=sorted(n for n in names if n.startswith('assets/')),
                      fonts=sorted(n for n in names if n.startswith('res/font/')))
    if aapt:
        badging = subprocess.run([str(aapt), 'dump', 'badging', str(path)], check=True, capture_output=True, text=True).stdout
        if "package: name='com.yongpeng.vamos'" not in badging:
            raise ValueError('Unexpected applicationId')
        result['package'] = badging.splitlines()[0]
    return result


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('apk', type=Path)
    p.add_argument('--aapt', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    if args.output and args.output.resolve() == args.apk.resolve():
        p.error("audit output must not overwrite the APK")
    result = audit(args.apk, args.aapt)
    encoded = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded)
    else:
        print(encoded, end='')


if __name__ == '__main__':
    main()
