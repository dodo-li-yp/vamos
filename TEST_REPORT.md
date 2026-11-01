# Vamos 测试报告

## 续作进展（以下进展优先于后面的首轮基线）

### M2 确认规则与停止路径

- 已接入支付、消息/邮件/评论、删除、账号/系统设置、对外发布五类规则，四类可持久化切换，关闭需二次提示，支付锁定。正常与自主任务始终保留确认关卡。
- 执行层取消自主模式无条件放行；支付核对金额，消息核对对象/原文，一次性授权绑定 App、对象、动作、有效步数；超时拒绝并停止，继续需新授权。
- 更新模型工具参数与提示词，消除“只有花钱才确认”的旧约定。停止直接取消运行协程，避免等待 inbox 消费。
- `assembleDebug` 通过；确认规则矩阵、原有目标/内容/金额绑定、持久化与自主模式测试通过（core 20 项，app 10 项，以测试 XML 为准）。
- 仍待：设备注入停止时延、日夜截图验收；COS 更新与 M4 下一段实现。

---

日期：2026-11-01。范围：M1 源码迁移与 M2 部分实现；本次不是 V1.0 正式发布验收。

## 环境与构建

- Linux 云构建环境，JDK 17，Gradle 8.11.1，Android SDK 36 / build-tools 34.0.0，minSdk 30 / targetSdk 35。
- 无 Android 真机或模拟器；`adb devices` 列表为空。没有进行安装、启动、配对、真实模型 API/OAuth、截图或任务注入的设备验收。
- 本次仅 debug 签名；没有索取、生成或提交 release keystore、Ed25519 私钥、API Key、token 或 COS 密钥。
- 构建使用 `JAVA_HOME=/workspace/jdk-17 ANDROID_HOME=/workspace/android-sdk GRADLE_USER_HOME=/workspace/.gradle`。
- M1：`./gradlew assembleDebug --offline` **通过**，保存里程碑 APK。
- M2：`./gradlew assembleDebug :app:testDebugUnitTest --tests '*VamosMilestoneTest' --offline` **通过**。
- 新增测试：**2 项通过、0 失败**；验证 DeepSeek/通义模型名、看图默认值、厂商保留、双音量键顺序/重复/释放重置。
- `git diff --check` **通过**。
- 最终 `./gradlew assembleDebug :app:lintDebug :app:testDebugUnitTest --tests '*VamosMilestoneTest' --offline` **通过**。
- Lint：**0 错误，92 警告**。报告中的警告未全部清理，未建立全局 baseline，未关闭错误检查。
- core 针对性回归：`ConfirmationGateTest` 15 项、`PhoneSetupFlowTest` 28 项、`KnowledgeIndexTest` 6 项，共 **49 项通过、0 失败**；与新增测试合计 **51 项通过**。
- Lint 首次为 33 错误；修复 Android 12 API 保护、广播注册、受限键盘回调、BOM 字面量等问题，将 19 处已确认赋值的 producer 改为显式 remembered state + keyed effect，并保留取消与资源释放逻辑。
- WebView 旧版本特性常量保留一处有原因说明的、方法级 `RestrictedApi` 抑制；未使用全局抑制。完整 HTML/XML 报告保留在 `app/build/reports/lint-results-debug.html` 与 `app/build/reports/lint-results-debug.xml`。
- 云环境自带 JDK 的 CA 缺失通过 `-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts` 解决；TLS 校验保持开启。依赖缓存补齐后最终检查使用 offline 模式。

## APK

- 最终 debug APK 已生成：`app/build/outputs/apk/debug/app-debug.apk`；目标交付名为 `vamos-1.0.0-debug.apk`，不提交二进制。
- M1 里程碑 APK 暂存于 `work/vamos-m1-debug.apk`。
- 此云环境不存在聊天指定的 `/codex/browser/projectless/.../output` 目录，且无权限创建 `/codex`，也未提供 sudo。因此未能生成聊天可下载的 APK/报告附件；APK 和完整 Lint 报告保留在上述构建目录，源码和本报告通过 draft PR 交付。
- applicationId：`com.yongpeng.vamos`，versionName：`1.0.0`，versionCode：**33**（上游为 32）。
- 大小：103,249,501 字节。
- SHA-256：`15ff53f3282d04d70924c04f0f66680ca802a3f65db3e59b6623010dfd50cbfe`。
- `aapt dump badging` 验证包名与版本；`apksigner verify` **通过**。
- 与上游包名不同，具备并存的包身份；实际并存安装**未测**。

## M1 验收

| 项目 | 结果 |
|---|---|
| Gradle applicationId/namespace、app/core 源码、测试、AIDL 目录与引用迁移 | 已实现，构建通过 |
| 品牌、通知、UA、提示词、launcher V 标志 | 已替换；设备启动画面待测 |
| LICENSE 原样保留 | 与基线 SHA-256 比对一致 |
| NOTICE 原文保留并追加修改声明 | 已实现，`Modified by 永鹏 / Vamos, 2026` |
| 修改的源文件头部标注 | 已添加 `Modified by Vamos (2026)` |
| 关于 › 开源许可 | 显示 Apache-2.0 全文、NOTICE 上游署名/链接和第三方列表/说明 |
| FileProvider authority | 声明 `com.yongpeng.vamos.fileprovider`，仅开放 `cache/update/`；更新安装流程接入留待 M3 |
| res 中品牌残留 | `rg -i mika app/src/main/res`：**0 命中**（含资源文件名已迁移） |
| 安装、与上游并存、启动无旧品牌 | 无设备，未测 |

## M2 实现与设计对照

已完整阅读 TASK.md、PRD、design-spec.md，查看日夜总览及 01–08 的所有分组图。下表为源码覆盖情况，**不是截图验收通过**。

| 设计组 | 本次实现 | 仍需完成 |
|---|---|---|
| 01 引导 | V 标志、两行标题、示例说明、条款阅读/勾选、权限说明、日夜主题 | 三份正式法律文档由产品/法务提供；引导顺序及逐屏布局微调、设备验证 |
| 02 模型 | DeepSeek 推荐边框、`deepseek-flash`、看图 caps、默认用主模型、通义 `qwen3.7-plus`、Key 后四位/显示切换、其他厂商列表、保留账号登录 | 连通性真实 API 验证；测试/保存仍沿用已有合并流程；视觉与交互逐屏验收 |
| 03 配对 | 保留真实状态驱动的向导，统一主题、字号、卡片和按钮 | 设计六步布局、六位 PIN 格、自动打勾动效等逐屏精修；所有配对状态设备验收 |
| 04 对话/执行 | 共享卡片/气泡主题，停止按钮，时间线语义色，后台预览青碧呼吸描边，隐藏 V1.1 示例任务卡 | 按稿重排任务卡、小窗/时间线、抽屉、完整通用状态；日夜设备截图 |
| 05 确认 | 无遮罩/返回关闭的底部确认面板、朱砂允许按钮、60 秒环形倒计时；服务端本地任务超时也改为 60 秒 | **五类逐项开关及执行层完整强制规则尚未实现**；截图预览、超时后重新确认等状态；不能宣称敏感确认增强完成 |
| 06 更新 | 普通新版提示改为居中圆角对话框，沿用已有下载控件 | COS、Ed25519、断点续传/强更及所有新状态留待 M3；旧 GitHub 更新实现仍在 |
| 07 其他 | 设置/记忆/定时/自检等复用新主题；隐藏电脑端入口；关于移除上游作者社交/联系入口；许可页署名 | 各页按稿重排、一键关闭手机控制入口、部分胶囊布局与视觉、完整许可全文整理 |
| 08 通用 | 现有空/加载/失败状态接入新主题，V1 示例入口已隐藏 | 骨架、断网、未配置/未配对提示等逐屏统一 |

共有 token 已按设计值实现，夜间 onAccent 为 #1A1C1B；标题暂用系统 Serif 回退，未捆绑 Noto Serif CJK。卡片实现色层/描边，完整背景模糊尚未全部接入；所有触控区/色彩用途仍需逐屏审计。

### 停止开关

- App 内按钮、现有通知「停止」、胶囊执行中长按、双音量键入口均连接真实取消路径。
- 双音量键在前台 Activity 可用；全局监听依赖用户启用无障碍且系统允许过滤按键。
- 手势纯逻辑单测通过；**没有实测 ≤1 秒中断注入/释放虚拟屏**，不能标为该时延验收通过。
- 本次没有完成五类敏感确认设置/策略。上游自主模式行为仍需整改，不能按 PRD 的支付锁定规则验收。

## 日夜截图验收

没有可用设备，未生成应用截图。设计图仅作参照，不当作应用运行结果。01–08 组的日间/夜间逐屏对照、字号放大、Android 11 降级与减少动画均**未测**。

## M3 更新六场景

| 场景 | 结果 |
|---|---|
| 正常 COS 更新 | 未实现/未测 |
| Ed25519 签名错误 | 未实现/未测 |
| SHA 错误 | 未按新协议实现/未测 |
| 断点续传 | 未实现/未测 |
| 强制更新 | 未实现/未测 |
| 无安装权限 | 未按新协议设备验收 |

## APK 内 native/runtime 清单与 M4

以下为本次实际 APK 的清单。**仍含 PRoot、Alpine rootfs；M4 未完成，不能声称无 GPL/LGPL 组件。** 未做上游同环境 APK 体积基线对比。

```text
lib/arm64-v8a/libandroidx.graphics.path.so
lib/arm64-v8a/libmlkit_google_ocr_pipeline.so
lib/arm64-v8a/libproot.so
lib/arm64-v8a/libproot_loader.so
lib/arm64-v8a/libvamosrun.so
lib/armeabi-v7a/libandroidx.graphics.path.so
lib/armeabi-v7a/libmlkit_google_ocr_pipeline.so
lib/x86/libandroidx.graphics.path.so
lib/x86/libmlkit_google_ocr_pipeline.so
lib/x86_64/libandroidx.graphics.path.so
lib/x86_64/libmlkit_google_ocr_pipeline.so
assets/runtime/packages.json
assets/runtime/rootfs.tar.xz
```

沙箱设置入口已隐藏，运行时代码/二进制并未整体移除。保留原有许可材料。README 的 Vamos 产品说明/构建说明、沙箱工具与依赖清理、GPL 资源移除均待 M4。

## 推迟功能

按 TASK.md：国产 ROM 图文适配、执行记录导出、新手示例任务卡、CLI/MCP 开放为 V1.1+；免确认白名单、模板市场、崩溃上报、沙箱重新引入为 V2+。本次没有新增这些功能或 SDK。

## 修改文件

见 `MODIFIED_FILES.md`，按基线 e17d308 列出迁移/修改/新增文件。每个里程碑单独提交，M2 为部分实现提交。
