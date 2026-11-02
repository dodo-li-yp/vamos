# Vamos COS release preparation

Dodo signs the release APK locally with the existing release keystore. This repository and the assistant use debug builds only. No COS write credentials belong in the app.

Configure the public read endpoint and raw 32-byte Ed25519 public key (Base64, comma-separated keys for rotation) at build time:

```sh
./gradlew assembleDebug \
  -PvamosUpdateUrl='https://<bucket>.cos.ap-guangzhou.myqcloud.com/vamos/version.json' \
  -PvamosUpdatePublicKeys='<BASE64_PUBLIC_KEY>'
```

Both values remain placeholders until supplied. The default app rejects updates without a public key. Never substitute a private key here.

Install Python `cryptography` locally, then generate metadata from the **final signed APK**:

```sh
python3 tools/release/make_version_json.py \
  --apk /path/to/final.apk --private-key /offline/path/ed25519.pem \
  --apk-url 'https://<bucket>.cos.ap-guangzhou.myqcloud.com/vamos/apk/vamos-1.0.1-34.apk' \
  --version-code 34 --version-name 1.0.1 --min-supported-version-code 33 \
  --changelog /path/to/changelog.txt --output /path/to/staging/version.json
```

Use `--force` only for a mandatory release. The script writes the manifest and Base64 `version.json.sig`; the signature covers the exact UTF-8 bytes including the final newline. The `signature` JSON field names the detached file; it does not contain a circular embedded signature. Both `force` and `forceUpdate` are emitted for PRD/TASK compatibility.

Verify the APK's package, monotonically increasing versionCode and release certificate with `apksigner` before upload. Upload the APK first and check its public size/hash. Upload `.sig` and then `version.json` in the same release batch, with the manifest last. Do not rewrite JSON or line endings afterward. Keep a strong ETag and Range support for resumable APK downloads. Production installation and the six update scenarios still require device acceptance with a matching installed certificate.
