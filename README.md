# Vamos

Vamos 是 Android 手机 AI 智能体：用自然语言发起任务，在后台屏操作 App，支持内置浏览器、定时任务、记忆与技能、多模型及账号登录。敏感操作按支付、消息/邮件、删除、系统设置、对外发布分类确认；支付确认始终开启。随时可用界面、通知或胶囊停止任务。

V1 正在开发，尚未完成真机与逐屏视觉验收。正式 COS 地址、公钥和法律文本仍待提供；不能把 debug 包当作正式发布版本。

## 构建

需要 JDK 17、Android SDK（compileSdk 36 / build-tools 34.0.0）和网络依赖缓存。Android 11 可安装，手机自动操作的完整能力需要 Android 12 及以上；按应用向导启用所需权限。

```sh
./gradlew assembleDebug
./gradlew :app:lintDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`，applicationId `com.yongpeng.vamos`，versionName `1.0.0`，versionCode `33`。调试签名用于开发验证，与上游应用包名不同。

## 更新与正式签名

更新只读腾讯云 COS 的公开 HTTPS URL；不需要在 App 中放 COS SecretId/SecretKey。清单在解析前校验 Ed25519 原始字节签名，APK 下载后校验大小、SHA-256，安装前复核包名、版本和证书。未配置正式公钥时拒绝更新。

详见 [发布辅助说明](tools/release/README.md)。私钥、keystore、API Key 和 token 不得提交。release 签名只由 Dodo 在本地完成；Gradle 从 `keystore.properties` 或 `VAMOS_STORE_FILE`、`VAMOS_STORE_PASSWORD`、`VAMOS_KEY_ALIAS`、`VAMOS_KEY_PASSWORD` 环境变量读取，缺失时保留未签名 release，debug 构建不受影响。每次正式发版 versionCode 必须递增。

## V1 范围

Linux/PRoot/Alpine/Python/Node 运行时与脚本执行工具已移除。附件、私有工作区文件读写/搜索/导出和网页上传仍由 Kotlin 文件接口提供。电脑 CLI/MCP 代码保留，入口默认隐藏（ENABLE_COMPUTER_ACCESS=false）；执行记录导出入口默认关闭（RunTimelineScreen 的 RECORD_EXPORT_ENABLED=false），实现保留供 V1.1；不包含统计或崩溃上报 SDK。

按最新需求，新手示例卡已提前到 V1（仅填入输入框，用户确认后发送）。国产 ROM 图文、执行记录导出和开放 CLI/MCP 延后到 V1.1；白名单、任务模板市场、崩溃上报和沙箱不属于 V1。

## 开源许可与致谢

本项目基于 [Mika](https://github.com/wangzan101/Mika) 二次开发，遵循 Apache-2.0。保留根目录 [LICENSE](LICENSE)、[NOTICE](NOTICE) 和上游署名，修改声明为 `Modified by 永鹏 / Vamos, 2026`；第三方许可见 [licenses](licenses/THIRD_PARTY_NOTICES.md) 和应用「关于 › 开源许可」。

开发约束见 [TASK.md](docs/vamos/TASK.md)，实际完成情况、未测边界与 APK 清单见 [TEST_REPORT.md](TEST_REPORT.md)。

## 开发交付检查

TASK.md 定义的 M1–M4 开发项已收尾；正式配置与真机验收分开管理。完整记录见 [TEST_REPORT.md](TEST_REPORT.md)，逐项执行清单见 [DEVICE_ACCEPTANCE.md](DEVICE_ACCEPTANCE.md)。缺失的 COS 地址、公钥和法律文本保留占位。

不安装 APK 即可复查已知运行时/SDK 排除项、许可和 native/assets 清单：

```sh
python3 tools/verify_v1_apk.py app/build/outputs/apk/debug/app-debug.apk \
  --aapt "$ANDROID_HOME/build-tools/34.0.0/aapt" --output /tmp/vamos-apk-audit.json
python3 -m unittest discover -s tools/release -p 'test_*.py'
```

审计脚本检查 APK 实际内容和已知禁止项，不能代替第三方依赖的完整法律审查。设备测试包编译通过也不代表真机验收通过；本分支保持 draft，不自动签名发布。

### 本地性能与网络验证

release/perf 启用 R8 与资源收缩，仅打包 arm64-v8a、armeabi-v7a。验证 release 可显式传
`-PvamosDebugSignRelease=true`，只用于本地测试；不传时仍读取本地正式签名配置或生成未签名包。
BETA 保持 true，不用于正式发布。

默认拒绝公网明文 HTTP；localhost、127.0.0.1、::1 与 `.local` 可使用 HTTP。
Android 网络安全 XML 不支持 CIDR：数字局域网 IP 必须在构建时通过
`-PvamosLanHttpHosts=<逗号分隔的实际局域网IPv4地址>` 显式白名单配置（仅接受 RFC1918）。
没有提供真实 LAN 地址时不臆造白名单；HTTPS 不受此限制。COS 清单和 APK 始终要求 HTTPS。
