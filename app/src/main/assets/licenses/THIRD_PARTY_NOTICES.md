# Third-party notices

## scrcpy

- Project: https://github.com/Genymobile/scrcpy
- License: Apache-2.0
- Usage: 以 shell 身份取得系统上下文(ActivityThread / 伪装包名的 Context)的做法仿照 scrcpy。
  源文件头部保留出处:
  - `app/src/main/java/com/wangzan101/mika/shell/ShellContext.kt`

## Kotlin 标准库

- Project: https://kotlinlang.org/
- Artifacts: `org.jetbrains.kotlin:kotlin-stdlib:2.1.20`、`kotlin-stdlib-common:2.1.20`、`kotlin-stdlib-jdk7:1.9.10`、
  `kotlin-stdlib-jdk8:1.9.10`;`org.jetbrains:annotations:23.0.0`
- License: Apache-2.0(依据各自 POM)
- Usage: 以库的形式依赖,打进 APK。

## kotlinx.coroutines

- Project: https://github.com/Kotlin/kotlinx.coroutines
- Artifacts: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0`、`kotlinx-coroutines-core-jvm:1.9.0`、`kotlinx-coroutines-android:1.9.0`
- License: Apache-2.0(依据各自 POM)
- Usage: 以库的形式依赖,打进 APK。

## kotlinx.serialization

- Project: https://github.com/Kotlin/kotlinx.serialization
- Artifacts: `org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3`、`kotlinx-serialization-json:1.7.3`(及其 `-jvm` 变体)
- License: Apache-2.0(依据各自 POM)
- Usage: 以库的形式依赖,打进 APK。

## OkHttp

- Project: https://square.github.io/okhttp/
- Artifact: `com.squareup.okhttp3:okhttp:4.12.0`
- License: Apache-2.0(依据 POM)
- Usage: 以库的形式依赖,用于访问模型接口。

## Okio

- Project: https://github.com/square/okio/
- Artifacts: `com.squareup.okio:okio:3.6.0`、`okio-jvm:3.6.0`
- License: Apache-2.0(依据各自 POM)
- Usage: OkHttp 的依赖。

## AndroidX(含 Jetpack Compose)

- Project: https://developer.android.com/jetpack/androidx
- License: Apache-2.0(依据各自 POM;`androidx.compose:compose-bom:2024.12.01` 只是版本清单,不含代码)
- Artifacts(release 运行时类路径里的全部 AndroidX 组件):
  - Compose 1.7.6:`compose.runtime:runtime`、`runtime-saveable`;`compose.ui:ui`、`ui-geometry`、`ui-graphics`、`ui-text`、
    `ui-unit`、`ui-util`;`compose.foundation:foundation`、`foundation-layout`;`compose.animation:animation`、`animation-core`;
    `compose.material:material-icons-core`、`material-ripple`(以上各含 `-android` 变体)
  - `compose.material3:material3:1.3.1`
  - `activity:activity`、`activity-ktx`、`activity-compose`:1.9.3
  - `lifecycle:*`(common、common-java8、livedata-core、process、runtime、runtime-compose、runtime-ktx、viewmodel、
    viewmodel-ktx、viewmodel-savedstate):2.8.3
  - `core:core`、`core-ktx`:1.13.1;`annotation:annotation`:1.8.1;`annotation-experimental`:1.4.1;
    `arch.core:core-common`、`core-runtime`:2.2.0;`collection:collection`、`collection-ktx`:1.4.4;
    `savedstate:savedstate`、`savedstate-ktx`:1.2.1;`emoji2:emoji2`:1.3.0;`profileinstaller`:1.3.1;
    `startup:startup-runtime`:1.1.1;`concurrent:concurrent-futures`:1.1.0;`autofill`:1.0.0;
    `customview:customview-poolingcontainer`:1.0.0;`graphics:graphics-path`:1.0.1;`interpolator`:1.0.0;
    `tracing`:1.0.0;`versionedparcelable`:1.1.1;`webkit:webkit`:1.12.1(内置浏览器)
- Usage: 界面与系统组件库,打进 APK。

## Guava ListenableFuture(由 AndroidX 库引入)

- Project: https://github.com/google/guava
- Artifact: `com.google.guava:listenablefuture:1.0` — Apache-2.0(依 Maven POM)
- Usage: 作为库依赖使用,未修改其代码。

## commonmark-java

- Project: https://github.com/commonmark/commonmark-java
- Artifacts: `org.commonmark:commonmark:0.30.1`、`commonmark-ext-gfm-tables:0.30.1`、`commonmark-ext-gfm-strikethrough:0.30.1`
- License: BSD-2-Clause
- Usage: 以库的形式依赖,未修改其代码;把对话里回答的 Markdown 解析成语法树,再由 Mika 自己的界面代码显示
  (`app/src/main/java/com/wangzan101/mika/ui/MarkdownModel.kt`)。
- License text:

```
Copyright (c) 2015, Atlassian Pty Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

## EdDSA-Java

- Project: https://github.com/str4d/ed25519-java
- Artifact: `net.i2p.crypto:eddsa:0.3.0`
- License: CC0-1.0 (public domain dedication)
- Usage: Ed25519 verification of the original COS update manifest bytes.

## Google ML Kit Text Recognition

- Artifacts: `com.google.mlkit:text-recognition:16.0.1` and `text-recognition-chinese:16.0.1` with their Google SDK dependencies.
- License: Google APIs Terms of Service / ML Kit terms (not GPL).
- Terms: https://developers.google.com/ml-kit/terms
- Usage: existing on-device OCR; bundled Chinese/Latin recognition models and native OCR pipeline. Not introduced as an analytics or crash-reporting SDK.

## Noto Serif SC

- Source: https://github.com/google/fonts/tree/main/ofl/notoserifsc
- License: SIL Open Font License 1.1; full text: OFL-Noto-Serif.txt.
- Usage: UI glyph subset at weights 500 and 600, generated with tools/fonts/subset_noto.py; other glyphs fall back to Android fonts. Copyright 2012 Google Inc. All Rights Reserved.
