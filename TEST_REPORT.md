# Vamos V1 开发与测试报告

日期：2026-11-02。分支 `v1-dev`，draft PR [#1](https://github.com/dodo-li-yp/vamos/pull/1)。本报告替代旧的阶段性未完成列表；历史结果可在 Git 提交中查看。仅使用 debug 签名，尚未达到正式发布验收。

## 2026-11-02 手势导航下确认面板被截断修复

复现来源：用户在 4aed2b8 测试包的 Android 真机截图；全面屏手势导航，「要你同意：发送」底部按钮被屏幕底边/手势区截断。设备型号、Android 版本未提供。

- 原确认面板虽有 navigationBarsPadding，但 Dialog 未显式以 edge-to-edge 接收安全区，且内容列没有滚动/高度约束，操作行排在正文后。改为 decorFitsSystemWindows=false，顶部/左右及底部使用 safeDrawing inset；正文占受限剩余高度并滚动，拒绝/允许行固定在滚动区外。
- 保留确认内容、真实截图、60 秒倒计时、禁止外部/返回关闭、一次授权和回调语义。大字体按钮可换行并保持至少 48dp 点击区，只对相关面板显式启用，多余页面按钮保持原行为。
- 同类修复：配对「继续设置/暂时跳过」、语音权限说明、更新说明、诊断面板改为受限滚动正文与独立操作区；诊断面板直接展开，避免默认半展开将底部操作藏到屏幕外。浏览器/模型用途/通用操作/账号登录长列表支持滚动，保留原有导航区 padding。
- 已检查标准 Material AlertDialog（包括继续下载等确认）；仍由其系统安全区/按钮槽处理。先前含「完成后继续／消耗额度」的弹窗仍未在仓库中匹配，不猜测修改其卡顿逻辑。本段不涉及依赖升级、网络/密钥/确认规则改动。
- 新增 ConfirmSheetLayoutTest 三个 Compose 回归场景：320×360 + 36dp 手势区；320×400 + 2 倍字体；360×280 横屏 + 2.5 倍字体 + 48dp 导航区。检查两个按钮完整位于安全区、48dp 点击区、正文滚动后操作行不移动，以及两个回调可点击。

验证结果：

- 应用 ConfirmationCopyTest、ApprovalStoreTest、DiagnosticsTest：**27 项通过**；核心 ConfirmationGateTest、VamosConfirmationTest：**18 项通过**；合计 **45 项通过，0 失败/错误/跳过**。
- `assembleDebug`、`assembleDebugAndroidTest`、`lintDebug` 集中检查 **BUILD SUCCESSFUL（7m57s）**；最后诊断面板展开设置的增量 `assembleDebug` 再次 **BUILD SUCCESSFUL（2m20s）**。
- Lint：**0 错误、79 警告、6 信息**，与上轮数量一致；未新增 baseline 或关闭检查。
- 新增三项 ConfirmSheetLayoutTest 已编译，但 **未执行**：`adb devices -l` 无连接设备；未启动模拟器。实际手势/三键导航、横屏和大字体复验见 DEVICE_ACCEPTANCE.md，没有宣称真机已通过。
- debug 签名和包身份验证通过：com.yongpeng.vamos / 1.0.0 / code 33；大小 **69,153,951 字节**，SHA-256 `0a611add2f567c99d33794e7be71b8ab208613c60634249c4cd2df6ab626a8e6`。本次只生成 debug，之前的 release 产物不包含本段修复。
- 当前 debug 副本：`work/vamos-1.0.0-debug.apk`；本段原始验证记录为忽略的 `work/sheet-safe-area-*`，APK/日志不提交仓库。

## 2026-11-02 输入法与性能专项（A/C）

本次只处理最新请求 A/C；B「完成后继续／消耗额度」确认框按用户指示跳过，等待截图。没有修改确认流程、升级依赖或大规模重构。

- A：语音层单击后在协程中 requestFocus，等一帧后请求 Compose 键盘并通过 InputMethodManager.showSoftInput 发起系统请求；输入栏只在 IME 可见时合并 IME inset，附件面板高度缓存/扣减也忽略不可见的假 inset。保留输入栏实测高度驱动消息列表底部留白与跟随最新消息逻辑。
- C1/2：release 与继承它的 perf 开启 R8/资源收缩，添加 AIDL/Binder、helper 反射入口、JS bridge、ML Kit/Firebase registrar、EdDSA 保留规则，移除 Log.d/Log.i；对 EdDSA 的可选 OpenJDK X509Key 分支仅精确 dontwarn（已核对字节码，Vamos 使用原始 EdDSAPublicKey 路径）；仅保留 arm64-v8a / armeabi-v7a。
- C3：默认关闭明文；只为 localhost、127.0.0.1、::1、.local 开例外。Android XML 不支持网段匹配，数字 LAN IPv4 通过 `-PvamosLanHttpHosts=<真实RFC1918地址列表>` 构建白名单；未提供地址时不编造。既有 COS 原始清单与 APK HTTPS 校验、拒绝 TLS 降级保持开启。
- C4/5/6：截图先读取 bounds，按显示宽度计算 2 的幂 inSampleSize 后 IO 解码；许可全文统一在 IO 加载；Recorder.stop 改为 suspend，AudioRecord 停止/释放和等待都在 Dispatchers.IO，删除 runBlocking。
- C7：扫描显式 token/apiKey/accessToken/authorization/secret 日志，未发现直接打印凭据值；去掉网页 URL 调试输出，账号日志仅记录固定失败说明，不输出异常或原始文本。没有提交实际密钥或地址。
- C8/9：按最新用户指示提前开放 V1 新手示例卡（按设计稿两列六张 VmExampleTaskCard，仅填草稿，不自动发送）；同步中英繁文案和测试。已移除 PDF/Python 脚本处理无界面入口；保留系统 PdfRenderer 的文件预览，它不运行 Python/脚本、不提供 PDF 提取能力。
- BETA 保持 true；COS URL、公钥、法律文本继续占位。release 仅使用显式 `-PvamosDebugSignRelease=true` 的 debug 签名验证；默认 release 仍用本地正式签名配置或未签名，不静默回退到 debug。

本次集中验证完成（未启动模拟器）：

- 相关 JVM 回归：应用 37 项 + 核心 10 项，合计 **47 项通过，0 失败/错误/跳过**。新增 IME/采样 2 项与录音异步停止 1 项回归。
- `lintDebug`、`lintRelease`、`assembleDebug`、`assembleRelease`、`assembleDebugAndroidTest` 均通过：集中命令 BUILD SUCCESSFUL（11m48s）；日志返回值规则补齐后单独 assembleRelease 再次通过（3m43s）。release 明确使用 debug 签名验证 R8，不是正式发布包。
- Lint debug/release：各 **0 错误、79 警告、6 信息**，数量与基线一致。未新增 baseline 或关闭错误检查。
- 扩大运行整个 EnglishCopyTest 时有 **1 项现存失败**：settingsAndSetup 期望英文 WelcomeCopy.TITLE，现有实现固定返回中文。该引导界面不在本次范围，未修改或将失败改成通过；最终相关测试只运行其中 chatPage。首次新增测试 API 写法编译错误已修正。
- 实际触摸语音层并检查系统 IME 可见的设备测试已编译，**未执行**，真实键盘、输入栏/消息遮挡与 R8 后配对/OCR 待真机验收。

| APK | 字节 | MiB | 比本次修改前 debug |
|---|---:|---:|---:|
| 修改前 debug（上一阶段交付包） | 93,051,111 | 88.74 | 基线 |
| 修改后 debug | 65,086,296 | 62.07 | 减少 30.05% |
| 修改后 release（R8/debug 签名） | 30,419,927 | 29.01 | 减少 67.31% |

没有旧 release 包，因此 release 行是相对旧 debug 的跨构建类型比较，不能归因于 R8 单一因素。

- debug SHA-256：`c0780730fa3d797bf17702e83d7dc190a1345fa1bd30d3edde3aa90a2f00ac67`。
- release SHA-256：`a042df9bb6812d5b83e15a4c24df8792e3df5c076b9594366c01e670ef90c84b`。

当前产物核查全部通过：

- `tools/verify_v1_apk.py` 对 debug/release 均通过；包名 com.yongpeng.vamos、1.0.0 / code 33。根 LICENSE SHA 与历史基线一致。
- 两包 apksigner 验证通过，均为同一 Android Debug 证书；DEX 内 HelperMain、IVamosShell、IVamosShell$Stub 入口保留。
- 最终 release DEX 的 Log.d/Log.i 方法引用为 **0**。规则包含 `return 0`，覆盖 Kotlin 将日志返回值作为表达式结果的情况。
- 许可/OCR/原有 assets 的路径与 CRC 保留；release 仅额外增加 Android 构建生成的 assets/dexopt/baseline.prof、baseline.profm。
- 默认拒绝明文的配置已在两包编译 XML 中核对。LAN 白名单生成器：合成的三个 RFC1918 测试地址通过，公网测试 IP 被拒；这些 fixture 仅写入忽略的 generated/，没有进入交付 APK，也不表示提供了真实 LAN 配置。
- APK 副本：`work/vamos-1.0.0-debug.apk`、`work/vamos-1.0.0-release-debug-signed.apk`；均不提交或发布。审计/构建原始记录位于忽略的 work/input-perf-*。

本次针对性单测范围：InputPerformanceTest、ProviderSpeechTest、LicensesTest、UpdateCheckerTest、WorkspaceContractTest、ChatCopyTest、EnglishCopyTest.chatPage、VamosMilestoneTest，以及核心 SignedUpdateTest。没有声称完整上游测试套件通过。

当前 native 清单（debug/release 相同）：

```text
lib/arm64-v8a/libandroidx.graphics.path.so
lib/arm64-v8a/libmlkit_google_ocr_pipeline.so
lib/armeabi-v7a/libandroidx.graphics.path.so
lib/armeabi-v7a/libmlkit_google_ocr_pipeline.so
```

## M3 收尾补充

- 下载截断按网络故障保留部分文件并续传；206 响应严格核对完整起止/总大小；暂停后不继续网络重试。
- 签名缓存时间与检查尝试时间分开，失败不会延长缓存；过期缓存从当前状态与重启状态中移除；强制更新不能被忽略；先写入可信缓存再开放下载入口。
- 更新相关文案统一为 COS；校验失败日志只记录固定说明，不记录清单、URL 或密钥。
- 发布辅助脚本拒绝空文件及覆盖 APK、私钥、变更说明等输入。三个 Python 回归测试只生成临时测试密钥，退出即删除。
- 本段 `SignedUpdateTest` 10 项、`UpdateCheckerTest` 3 项及发布脚本 3 项，合计 **16 项通过，0 失败**。未启动模拟器；M4 后集中生成最终 debug APK 和运行 Lint。

## 开发收尾状态

按最新指示，TASK 定义的 M1–M4 开发项与真机验收分开管理：源码/本地验证收尾完成；真机项不阻塞开发，全部列入 [DEVICE_ACCEPTANCE.md](DEVICE_ACCEPTANCE.md)。没有额外定义 M5；按 2026-11-02 最新用户指示，仅将新手示例任务卡提前到 V1，其余 V1.1/V2 保持原范围。正式签名和发布仍由 Dodo 本地执行。

M4 本段还隐藏了残留的执行记录导出入口（默认关闭，代码保留供 V1.1），移除旧沙箱自检/提示并将通用状态资源改名。清理 Lint 标记且在主代码/测试/XML 中均无引用的资源，新增 `tools/verify_v1_apk.py`，可复现检查已知 Linux/运行器、Compress/XZ 和统计/崩溃 SDK 类排除项及 native/assets/许可清单。保留 Android 系统 helper 操作所需权限，不删除仍被保留能力使用的权限。

## 完成情况

| 里程碑 | 源码完成项 | 验收边界 |
|---|---|---|
| M1 | 品牌、包名/namespace/AIDL/目录迁移为 Vamos / com.yongpeng.vamos；V 图标；保留 LICENSE/NOTICE，修改声明与许可页 | 编译/资源扫描通过；真实并存安装与启动验收未完成 |
| M2 确认与停止 | 五类确认逐项持久化，支付永久开启；自主/远程模式仍受规则约束；目标/内容/金额绑定、单次授权；通知/胶囊/音量组合停止；关闭控制时暂停看门狗并阻止共用连接重新获取，保留配对身份 | 安全回归结果见下；≤1 秒停止和释放虚拟屏需要真机测量 |
| M2 界面 | 日夜 token、Noto Serif SC 标题子集、玻璃/24dp 模糊与 Android 11 回退；引导/法律占位/权限、模型配置、配对清单/PIN/跳过说明、任务卡/时间线/真实预览、确认截图/60 秒倒计时/超时重启、更新状态、设置、空状态和骨架 | 未获得有效应用截图；不能宣称与 57 屏逐屏像素验收通过 |
| M3 | 删除 GitHub Releases 更新；COS 清单原始字节 Ed25519 验签、双公钥、缓存与节流、HTTPS/ETag/Range 续传、SHA/大小校验、强更、安装权限与 FileProvider 安装、离线签名脚本 | 正式地址与公钥未提供；线上 COS 和系统安装六场景仍需设备验收 |
| M4 | 删除 Linux/PRoot/talloc/Alpine、native 运行器、脚本执行工具/桥接/准备服务；保留安全 Kotlin 工作区、附件/上传下载/导出；清理 Compress/XZ、许可及 README；CLI/MCP UI 默认关闭 | APK 清单及针对性回归见下；未引入独立崩溃/统计 SDK |

主要提交：`5a05ee0` M3 收尾；`532a4b3` 五类确认；`393f605` COS 更新；`db53ed2` Linux 移除；`8737ff4` 看门狗暂停、PIN、预览等界面改动。后续修复包含后台确认超时回显、完整 60 秒计时、配对等待 10 秒提供人工确认。

## M2 设计覆盖与限制

| 设计组 | 当前实现 |
|---|---|
| 01 引导 | 欢迎、法律阅读/勾选、六类权限说明；三份法律文件明确为待提供占位，未编造正式条款 |
| 02 模型 | DeepSeek deepseek-flash 默认推荐、原生看图 caps、看图默认用主模型；通义 qwen3.7-plus 备选；Key 后四位/显示切换；其余厂商与 ChatGPT/xAI/OpenRouter 登录保留 |
| 03 配对 | 六步真实状态清单、当前/完成/失败/连接态、六位 PIN；10 秒后人工确认；常驻跳过及二次说明；国产 ROM 图文延期 |
| 04 对话/执行 | VmDrawer、任务卡、语义色时间线、132×240dp 实际帧预览，可展开；停止中反馈；通知、胶囊和音量停止入口 |
| 05 确认 | 104×180dp 真实帧（无帧明确提示）、五类规则、不可从遮罩/返回关闭、60 秒环、最后 10 秒加粗、超时拒绝并停止；重新确认重新发起任务，不复用旧授权 |
| 06 更新 | 普通/强制提示、移动流量确认、百分比/8dp 进度条/速度、暂停续传、校验/错误、未知来源授权返回安装、安装中反馈 |
| 07 其他 | 浏览器、定时、记忆/技能、自检、设置/确认规则/关于/许可、胶囊使用统一主题；一键关闭控制保留配对信息 |
| 08 通用 | 空状态、骨架、未配置/未配对提示及已有错误反馈；仍需逐屏检查长文本、字号、触控区和各状态布局 |

新增截图夹具 `VmDesignScreenshotsTest` 仅用于生产 Compose 组件的日夜渲染，不能代替实际任务、支付或下载成功的证明。当前没有输出有效应用截图。逐屏位置、动画、日夜对照和无障碍触控验收仍未完成，因此 M2 不标记为最终验收完成。

## 环境与测试

- Linux 云环境；JDK 17、Gradle 8.11.1、Android SDK 36 / build-tools 34.0.0；minSdk 30 / targetSdk 35。
- 无真机。曾尝试 Android 11 x86_64 软件模拟器；无 KVM，框架启动缓慢，安装时出现 system_server 重启/DeadSystemException 和 Broken pipe。未将其误报为 Vamos 崩溃或安装成功。按最新要求已停止模拟器。
- 为节省资源，使用单 worker、进程内 Kotlin 编译及缓存；稳定源码后统一检查，不反复运行模拟器。
- 常规构建命令仍为 `./gradlew assembleDebug`；云环境额外设置 JAVA_HOME、ANDROID_HOME、GRADLE_USER_HOME 和系统 CA trustStore，TLS 校验保持开启。

上一阶段（2026-11-01）统一命令包含 `assembleDebug :app:lintDebug :core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest`，附下列针对性测试过滤器及 `--offline`，**BUILD SUCCESSFUL**（最终检查 10m7s；本轮未运行模拟器）。

- debug APK 编译：通过。
- Lint：**0 错误、79 警告、6 信息**。之前三个缩进错误未再出现；未使用全局 baseline 或关闭错误检查。警告尚未全部清理，主要类型：ApplySharedPref 17, StringFormatCount 10, ModifierParameter 7, PluralsCandidate 7；UnusedResources 已清零。
- 核心：**96 项通过，0 失败/错误/跳过**。
- 应用：**50 项通过，0 失败/错误/跳过**，JVM/应用合计 **146 项**；另有发布脚本 **3 项**，总计 **149 项通过**。
- 设备测试 APK：编译通过，**未执行设备测试**，截图夹具未运行。
- `tools/verify_v1_apk.py` 实际 APK 审计通过；删除已知 Linux/运行器/禁止 SDK 排除项，许可清单齐全。
- `git diff --check`、APK `apksigner verify`、`aapt dump badging`：通过。

核心测试：

| 套件 | 通过项数 |
|---|---:|
| ToolDispatcherSandboxTest | 9 |
| BuiltinShellEngineTest | 34 |
| VamosConfirmationTest | 3 |
| ConfirmationGateTest | 15 |
| SignedUpdateTest | 10 |
| ApprovalPolicyTest | 2 |
| WorkspaceFileGateTest | 12 |
| SelfCheckRunnerTest | 11 |

应用测试：

| 套件 | 通过项数 |
|---|---:|
| VamosMilestoneTest | 2 |
| DiagnosticsTest | 14 |
| ReleasesTest | 1 |
| AppVersionTest | 4 |
| ApprovalStoreTest | 7 |
| AttachmentsNamingTest | 3 |
| LicensesTest | 1 |
| VamosLinksTest | 4 |
| WorkspaceContractTest | 6 |
| ApkIntegrityTest | 5 |
| UpdateCheckerTest | 3 |

早期 M3 签名脚本使用临时目录生成测试密钥完成冒烟验证，临时私钥已删除；没有将测试私钥作为 App 默认配置。没有声称完整上游测试集、真实模型 API/OAuth 或真机任务测试全部通过。

## 更新六场景

| 场景 | 已有证据 | 仍需验收 |
|---|---|---|
| 正常更新 | 签名清单、versionCode 判新、下载 hash/大小单测 | COS 公网到安装器全链路 |
| 签名错误 | 篡改/错误签名/缺公钥拒绝，验签先于 JSON 解析 | 设备提示与联网场景 |
| SHA 错误 | 校验失败删除文件，不进入安装态；同版本两次失败停止重试 | 设备界面与重试操作 |
| 断点续传 | 强 ETag + Range/If-Range、200 重启、错误 206 拒绝、暂停续传单测 | 网络切换/后台进程终止 |
| 强制更新 | versionCode/force/minSupported 判定单测；不可返回/遮罩关闭，执行中延后弹窗 | 设备交互 |
| 无安装权限 | 授权检查、设置页与返回后安装入口已接通 | 系统授权页、安装成功/取消、包名/证书冲突的设备流程 |

## 上一阶段 APK 与合规（2026-11-01 历史基线）

- 当前 APK：`app/build/outputs/apk/debug/app-debug.apk`；交付副本 `work/vamos-1.0.0-debug.apk`（忽略文件，不提交二进制）。
- 大小：**93,051,111 字节**；SHA-256：`9ddfd4987181f42188038cc161b88ec065012567abc3b62fb15f95379a8725aa`。
- 比移除 Linux 前同环境 debug APK 的 103,249,501 字节下降 **9.88%**。这是移除前后比较；上游正式测试包未提供，不声称已实测上游发布包体积。
- 实测无 proot/talloc/rootfs/vamosrun/可执行脚本资源。剩余 native 为 AndroidX 图形与现有本地 ML Kit OCR，各组件许可已列明。

- applicationId / FileProvider：`com.yongpeng.vamos` / `com.yongpeng.vamos.fileprovider`；versionName `1.0.0`，versionCode `33`。
- 根 LICENSE 与基线 e17d308 SHA-256 一致：`cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`；NOTICE 原文保留并追加 `Modified by 永鹏 / Vamos, 2026`。
- `rg -i mika app/src/main/res`：0 命中。上游署名和 Apache 全文保留在开源许可材料；修改文件清单见 MODIFIED_FILES.md。
- EdDSA-Java：CC0-1.0；Noto Serif SC：SIL OFL 1.1，保留全文，UI 字形子集总计约 1.3MB；其他字形由 Android 回退字体补足。现有 Google ML Kit OCR 许可也已列明。
- 当前云环境没有可写的聊天附件输出目录；APK 保留在构建目录及忽略的 work/ 下，未作为二进制提交或发布到 Releases。

### 2026-11-01 基线 APK 内 native / assets 清单

```text
lib/arm64-v8a/libandroidx.graphics.path.so
lib/arm64-v8a/libmlkit_google_ocr_pipeline.so
lib/armeabi-v7a/libandroidx.graphics.path.so
lib/armeabi-v7a/libmlkit_google_ocr_pipeline.so
lib/x86/libandroidx.graphics.path.so
lib/x86/libmlkit_google_ocr_pipeline.so
lib/x86_64/libandroidx.graphics.path.so
lib/x86_64/libmlkit_google_ocr_pipeline.so
assets/licenses/Apache-2.0.txt
assets/licenses/CC0-1.0.txt
assets/licenses/NOTICE.txt
assets/licenses/OFL-Noto-Serif.txt
assets/licenses/THIRD_PARTY_NOTICES.md
assets/mlkit-google-ocr-models/aksara/aksara_page_layout_analysis_rpn_gcn.binarypb
assets/mlkit-google-ocr-models/aksara/aksara_page_layout_analysis_ti_rpn_gcn.binarypb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Hani_ctc/optical/assets.extra/LabelMap.pb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Hani_ctc/optical/conv_model.fb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Hani_ctc/optical/lstm_model.fb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Hani_ctc_cpu.binarypb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Latn_ctc/optical/assets.extra/LabelMap.pb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Latn_ctc/optical/conv_model.fb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Latn_ctc/optical/lstm_model.fb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/Latn_ctc_cpu.binarypb
assets/mlkit-google-ocr-models/gocr/gocr_models/line_recognition_legacy_mobile/tflite_langid.tflite
assets/mlkit-google-ocr-models/gocr/layout/line_clustering_custom_ops/model.tflite
assets/mlkit-google-ocr-models/gocr/layout/line_splitting_custom_ops/model.tflite
assets/mlkit-google-ocr-models/taser/detector/region_proposal_text_detector_tflite_vertical_mbv2_v1.bincfg
assets/mlkit-google-ocr-models/taser/detector/rpn_text_detector_mobile_space_to_depth_quantized_mbv2_v1.tflite
assets/mlkit-google-ocr-models/taser/rpn_text_detection_tflite_mobile_mbv2.binarypb
assets/mlkit-google-ocr-models/taser/segmenter/tflite_script_detector_0.3.bincfg
assets/mlkit-google-ocr-models/taser/segmenter/tflite_script_detector_0.3.conv_model
assets/mlkit-google-ocr-models/taser/segmenter/tflite_script_detector_0.3.lstm_model
assets/mlkit-google-ocr-models/taser/taser_script_identification_tflite_mobile.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrchinese_and_latin_mbv2_aksara_layout_gcn_mobile_engine.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrchinese_and_latin_mbv2_aksara_layout_gcn_mobile_engine_ti.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrchinese_and_latin_mbv2_aksara_layout_gcn_mobile_recognizer.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrchinese_and_latin_mbv2_aksara_layout_gcn_mobile_runner.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrchinese_and_latin_mbv2_aksara_layout_gcn_mobile_runner_ti.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile_engine.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile_engine_ti.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile_recognizer.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile_runner.binarypb
assets/mlkit-google-ocr-models/taser_tflite_gocrlatin_mbv2_scriptid_aksara_layout_gcn_mobile_runner_ti.binarypb
assets/selfcheck/page.html
assets/web/page.js
res/font/vamos_serif_medium.ttf
res/font/vamos_serif_semibold.ttf
```

## 占位、未完成与延期

1. 正式 COS 桶地址、公钥、三份正式法律文本由产品/运维提供；保持占位，未配置更新时 fail closed。没有索取或提交正式私钥、keystore、API Key、token、COS SecretId/SecretKey。
2. 逐屏日夜截图、视觉对照、真机配对/后台操作/停止时延，以及真实模型/OAuth 和更新安装六场景仍需完成。源码覆盖不等于设备验收通过。
3. 正式签名与发布由 Dodo 本地完成；本次不构建正式签名包，不上架、不发布、不合并 PR。
4. V1.1 延期：国产 ROM 图文、执行记录导出、开放 CLI/MCP。V2：免确认白名单、模板市场、崩溃上报、沙箱。
5. 随 Linux 一并移除的 PDF/Python 脚本提取不可用；普通附件和文件工具保留，不能仅凭二进制文件名声称已读取内容。
