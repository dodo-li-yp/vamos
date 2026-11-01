#!/usr/bin/env python3
# Modified by Vamos (2026): offline detached Ed25519 signing. Never embeds a private key.
"""Requires cryptography. Supply the offline private-key path; upload .sig before version.json."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
from urllib.parse import urlsplit
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--private-key", type=Path, required=True)
    parser.add_argument("--apk-url", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--min-supported-version-code", type=int, required=True)
    parser.add_argument("--changelog", type=Path, required=True)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--output", type=Path, default=Path("version.json"))
    args = parser.parse_args()
    url = urlsplit(args.apk_url)
    if url.scheme != "https" or not url.hostname or url.username or url.password or url.fragment:
        parser.error("APK URL must be HTTPS without credentials or fragment")
    if not 0 <= args.min_supported_version_code <= args.version_code or args.version_code <= 0:
        parser.error("invalid version codes")
    key = serialization.load_pem_private_key(args.private_key.read_bytes(), password=None)
    if not isinstance(key, Ed25519PrivateKey):
        parser.error("private key must be Ed25519")
    digest = hashlib.sha256()
    with args.apk.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    manifest = dict(versionCode=args.version_code, versionName=args.version_name,
                    apkUrl=args.apk_url, sha256=digest.hexdigest(), size=args.apk.stat().st_size,
                    force=args.force, forceUpdate=args.force,
                    minSupportedVersionCode=args.min_supported_version_code,
                    changelog=args.changelog.read_text(encoding="utf-8"),
                    signature=args.output.name + ".sig")
    raw = (json.dumps(manifest, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    signature = key.sign(raw)
    key.public_key().verify(signature, raw)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(raw)
    args.output.with_name(args.output.name + ".sig").write_text(base64.b64encode(signature).decode("ascii") + "\n", encoding="ascii")
    print("Signed manifest and detached signature written; private key was not copied.")


if __name__ == "__main__":
    main()
