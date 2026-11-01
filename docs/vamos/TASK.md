# WorkBuddy 开发任务：Vamos（Android 手机 AI 智能体）V1.0

## 0. 目标
基于开源项目 Mika（Apache-2.0）二次开发，品牌化为 **Vamos**。设计稿 v0.1 已由老板批准，可以开始构建 APK。
- 代码仓库（已 fork，main 分支）：https://github.com/dodo-li-yp/vamos
  - 上游：https://github.com/wangzan101/Mika
- 本包内文件：`vamos-PRD.md`（PRD v1.0 定稿，需求唯一依据）、`vamos-design-v0.1.zip`（57 屏总览图 + html 源稿 + 生成脚本）、`design-spec.md`（设计规范/标注/组件命名）、本文件。
- 参考：上游测试包 Mika-v0.1.22（找 Dodo 要），仅用于对照原功能行为。

## 1. 固定决策（不得更改）
| 项 | 值 |
|---|---|
| applicationId / 包名 | `com.yongpeng.vamos`（namespace、源码 package 目录、Manifest、FileProvider authority `com.yongpeng.vamos.fileprovider` 一并改） |
| App 名称 | Vamos |
| 默认推荐模型 | DeepSeek `deepseek-flash`（V4.1-Flash，原生看图+工具调用）：主模型选它，看图模型默认开启并设为「用主模型」，DeepSeek 预设的 caps 补上看图能力；备选推荐 通义 `qwen3.7-plus`（复杂 App 失败多时切换）；首次启动推荐 + 一键测试连通性；其余所有厂商与 ChatGPT / xAI / OpenRouter 账号登录全部保留 |
| 在线更新 | 腾讯云 COS 广州桶托管 APK + `version.json`；Ed25519 验签；APK SHA-256 校验；FileProvider + 系统安装器；删除原 GitHub Releases 更新代码 |
| 发布渠道 | 不上架应用商店，仅 COS 直装，App 内更新始终开启 |
| Linux 沙箱 | V1 整体移除（PRoot / talloc / Alpine / Python / Node / shell 运行时，GPL 合规原因） |
| 电脑端调用 CLI / MCP | 代码保留，V1 UI 中隐藏入口 |
| 崩溃上报 | V1 不做，不引入任何崩溃/统计 SDK |
| 夜间按钮文字色 onAccent | 日间 #FFFFFF；夜间 #1A1C1B（设计规范 §7 第 1 点已确认） |
| 版本号 | 每次发版 versionCode +1（只增不减）；versionName 语义化，首版 1.0.0 |

## 2. V1 范围说明（PRD §3 与 §9 不一致的处理）
PRD §3 的优先级表与 §9 版本规划有冲突（如国产 ROM 配对、示例任务卡在 §3 为 P1，但 §9 归入 V1.1；崩溃上报 §3 列为 P2 可选，§9 归入 V2.0）。**统一按较窄且一致的解读：V1.0 范围 = §9 中 V1.0 行**，即：
- 品牌替换与合规；§3.1 保留功能中的 P0/P1（沙箱已删、CLI/MCP 隐藏）；COS 在线更新；签名发布流程；扩展后的敏感操作确认（§5）；停止开关（1 秒内生效）；新 UI。
- **例外**：DeepSeek 默认推荐由老板明确决策，放入 V1.0。
- **推迟到 V1.1 及以后，V1 不要做**：国产 ROM 配对图文、执行记录导出、新手示例任务卡、CLI/MCP 开放；V2.0：免确认白名单、任务模板市场、崩溃上报、沙箱。
- 设计稿里如果出现上述推迟功能的页面，V1 不实现或隐藏入口，在测试报告中列出。

## 3. 里程碑
每个里程碑单独提交（commit 信息写清楚 M 编号），完成后出一个 debug APK 并简要自测。

### M1 品牌替换 + 包名迁移
1. 包名改为 `com.yongpeng.vamos`（Gradle applicationId、namespace、目录、import、Manifest、ProGuard 规则、AIDL/JNI 签名如涉及）。
2. 全部用户可见文本、图标、启动页、通知、悬浮胶囊、浏览器 UA、系统提示词中的自称 → Vamos。UI/品牌中**不得出现 "Mika" 或原作者名字**（`grep -ri mika` 对 res/ 与字符串资源应为 0，代码内部类名可按需保留但建议改）。
3. 合规：保留根目录 `LICENSE`、`NOTICE`；在 NOTICE 追加修改声明（"Modified by 永鹏 / Vamos, 2026"）；被修改的源文件头部加 `Modified by Vamos (2026)` 注释；新增「关于 › 开源许可」页面，**唯一允许**展示上游项目名 Mika、原作者署名、仓库链接与 Apache-2.0 全文，以及第三方库许可列表。
4. 验收：可安装、可与上游版本并存（不同包名），启动无 Mika 字样。

### M2 按设计稿实现 UI
1. 严格按 `design-spec.md`：颜色 token `VmColors.*`（不新增色相）、字号、圆角间距、组件命名 `Vm` + PascalCase、日/夜两套。
2. 青碧只用于 AI 执行中；朱砂只用于确认/停止/错误。
3. 覆盖 57 屏总览中 V1 范围内的页面：引导、模型配置（DeepSeek 默认）、配对向导、对话与任务执行、敏感操作确认、更新、各设置页、通用状态（空/加载/错误/无网络）。
4. 敏感操作确认扩展为：支付、发消息/邮件、删除、修改系统设置、对外发布，按类别逐项开关（PRD §5）。
5. 停止开关：常驻通知「停止」、胶囊长按停止、音量键组合紧急停止，均 ≤1 秒生效。
6. 验收：与设计图逐屏对照截图（日/夜各一套），放入测试报告。

### M3 在线更新（替换 GitHub 更新）
1. 删除原 GitHub Releases 检查更新的代码与相关网络权限/字符串。
2. 读取 COS 上 `version.json`（URL 做成 BuildConfig 常量，Dodo 提供最终地址；开发期用占位 `https://<bucket>.cos.ap-guangzhou.myqcloud.com/vamos/version.json`）。字段按 PRD §4a（至少：versionCode、versionName、apkUrl、sha256、size、forceUpdate、minSupportedVersionCode、changelog、signature）。
3. **Ed25519 验签**：App 内置公钥（Dodo 提供，开发期用你自己生成的测试密钥对，测试私钥不得提交进仓库）；签名不通过一律拒绝更新。
4. 下载支持断点续传；下载完成校验 SHA-256，不一致删除文件并报错。
5. 通过 FileProvider + `ACTION_VIEW`/`PackageInstaller` 调起系统安装器；声明 `REQUEST_INSTALL_PACKAGES`，未授权时引导到「允许安装未知应用」设置页。
6. 支持强制更新；versionCode 不高于当前时不提示。
7. 附带一个发布辅助脚本（如 `tools/release/make_version_json.py`）：输入 APK 和私钥路径，输出带签名的 version.json（私钥路径由运行者传入，脚本不含任何密钥）。
8. 验收：用测试密钥覆盖 正常更新 / 签名错误 / SHA 错误 / 断点续传 / 强制更新 / 无安装权限 六种情况。

### M4 清理与沙箱移除
1. 删除 Linux 沙箱全部代码、native 库、PRoot/talloc/Alpine 资源、相关工具定义与提示词；确认 APK 内无 GPL 组件（列出 `lib/` 与 assets 清单）。
2. 隐藏 CLI/MCP 入口（代码保留，加开关默认关闭）。
3. 移除崩溃上报/统计 SDK（若有）；移除未用权限与资源。
4. 更新 README（Vamos 简介 + 上游致谢 + 构建说明）。
5. 验收：`./gradlew assembleDebug` 通过、lint 无新增错误、APK 体积较上游下降情况记录在报告中。

## 4. 签名与发布
- **你只用 debug 签名构建**。release keystore 与 Ed25519 私钥由 Dodo 保管，**不会提供给你，也不得索取后写入仓库**。
- release 签名在 Dodo 本地完成：`./gradlew assembleRelease` 时通过环境变量/本地 `keystore.properties`（已在 .gitignore）读取。请你在 `app/build.gradle(.kts)` 中实现这套读取逻辑：变量缺失时 release 构建回退为未签名或报清晰错误，不能失败在 debug 构建上。
- 需要正式签名包时，停下来告诉 Dodo。

## 5. 禁止事项
- 不在 UI/品牌中使用 "Mika" 或原作者名（开源许可页除外）；不删 LICENSE/NOTICE。
- 不提交任何密钥、keystore、token、COS SecretId/SecretKey（App 端只读公开 URL，不需要 COS 密钥）。
- 不做 V1 范围外功能（§2）；不重新引入沙箱或任何 GPL 依赖；不加崩溃上报/统计。
- 不删除任何模型厂商或账号登录方式。
- 不改动设计 token 色值；不擅自上架/发布；不向 dodo-li-yp/vamos 以外的仓库推送；不 force-push main（用分支 + PR，或直接按里程碑提交到 main 前先说明）。

## 6. 交付物
1. 源码：推送到 https://github.com/dodo-li-yp/vamos（建议分支 `v1-dev`，每个里程碑一个 PR 或 tag）。
2. debug APK：`vamos-1.0.0-debug.apk`（注明 versionCode）。
3. 测试报告 `TEST_REPORT.md`：环境（设备/Android 版本）、每个里程碑验收项结果、日/夜截图对照、更新六场景结果、APK 内库清单（证明无 GPL）、`grep -ri mika` 结果、已知问题、推迟到 V1.1 的项目列表。
4. 修改文件清单（供 NOTICE 核对）。

## 7. 有疑问时
需求以 PRD v1.0 为准，视觉以 design-spec.md + 总览图为准；两者冲突或不清楚时，列出问题停下来问 Dodo，不要自行猜测扩大范围。
