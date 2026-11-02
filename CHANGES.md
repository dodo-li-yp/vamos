# Vamos changes from upstream

## M1 — 2026-11-01
- Migrate Android/JVM/AIDL packages to `com.yongpeng.vamos`, including helper identities and test sources.
- Replace product branding, prompts, notifications, resource names, user agent and launcher artwork with Vamos.
- Preserve upstream LICENSE/NOTICE, append modification attribution and show legal texts on the open source license page.
- Version 1.0.0 (33); optional local/environment release signing; debug builds require no release credentials.
- Reserve a narrowly scoped `com.yongpeng.vamos.fileprovider` for update cache files.

## M2 foundation — 2026-11-01 (partial)
- Apply the day/night paper, ink, dai, qingbi, cinnabar and amber palette through `VmColors`; serif headings, 20dp cards, 48dp main controls and mountain background.
- Add welcome, draft legal-document consent, permission explanations, DeepSeek recommendation, key masking and provider/account entries.
- Add a locked confirmation sheet and 60-second timeout, cinnabar stop control, capsule long-press stop and volume-key stop chord.
- Apply execution colors to timeline/browser/background preview; use a centered update dialog and remove upstream contact/social routes.
- Hide V1.1 example cards and computer-connection settings entries. Sandbox settings entry is hidden, but sandbox removal is still outstanding.
- M2 is not fully accepted: five-category confirmation enforcement/settings, full screen layouts, screenshots and device timing tests remain. See TEST_REPORT.md.

### Lint follow-up
- Guard Android 12-only ADB/helper APIs while keeping Android 11 chat/web support.
- Register test broadcasts with an explicit exported policy through ContextCompat.
- Handle foreground volume keys through public Activity callbacks instead of restricted ComponentActivity dispatch APIs.
- Replace 19 verified producer lambdas with explicit remembered state and keyed effects, preserving cancellation/finally cleanup; escape the UTF-8 BOM literal in the file viewer.
- Keep one documented, method-scoped AndroidX WebView compatibility suppression for its restricted legacy feature constant; no global lint baseline or error disabling.
