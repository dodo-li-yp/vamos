# Vamos

Vamos is an Android AI agent derived from the Apache-2.0 Mika project. It operates apps on a background display and retains browser, scheduling, memory, skills and model account integrations.

V1 development and acceptance status: [TEST_REPORT.md](TEST_REPORT.md). Build with JDK 17 and Android SDK 36: `./gradlew assembleDebug`. Only debug signing is used here. Production COS endpoint/public keys and legal documents remain placeholders.

Linux/PRoot/Alpine runtimes and executable script tools are removed in V1; ordinary private workspace files remain available. CLI/MCP code is retained with its UI disabled by default. No analytics or crash-reporting SDK is included.

See the [Chinese README](README.md), [release preparation](tools/release/README.md), [LICENSE](LICENSE), [NOTICE](NOTICE) and [third-party notices](licenses/THIRD_PARTY_NOTICES.md).
