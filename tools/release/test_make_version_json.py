# Modified by Vamos (2026): ephemeral signing and non-destructive release preparation tests.
import base64
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.key = Ed25519PrivateKey.generate()
        self.pem = self.root / 'offline.pem'
        self.pem.write_bytes(self.key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        self.apk = self.root / 'sample.apk'
        self.apk.write_bytes(b'fixture APK bytes')
        self.notes = self.root / 'changes.txt'
        self.notes.write_text('测试更新', encoding='utf-8')

    def run_signer(self, output):
        return subprocess.run([sys.executable, str(Path(__file__).with_name('make_version_json.py')),
                               '--apk', str(self.apk), '--private-key', str(self.pem),
                               '--apk-url', 'https://updates.invalid/test.apk', '--version-code', '34',
                               '--version-name', '1.0.1', '--min-supported-version-code', '33',
                               '--changelog', str(self.notes), '--output', str(output)], capture_output=True)

    def test_exact_bytes_and_detached_signature(self):
        output = self.root / 'version.json'
        self.assertEqual(0, self.run_signer(output).returncode)
        raw = output.read_bytes()
        signature = base64.b64decode(output.with_name('version.json.sig').read_bytes())
        self.key.public_key().verify(signature, raw)
        manifest = json.loads(raw)
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(), manifest['sha256'])
        self.assertEqual(self.apk.stat().st_size, manifest['size'])
        self.assertEqual('version.json.sig', manifest['signature'])

    def test_never_overwrites_inputs(self):
        for path in [self.apk, self.pem, self.notes]:
            previous = path.read_bytes()
            self.assertNotEqual(0, self.run_signer(path).returncode)
            self.assertEqual(previous, path.read_bytes())

    def test_empty_apk_rejected(self):
        self.apk.write_bytes(b'')
        output = self.root / 'version.json'
        self.assertNotEqual(0, self.run_signer(output).returncode)
        self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
