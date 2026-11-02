---
name: documents
display: 处理 PDF 文档
description: 读 PDF 的文字、按页取、看文档信息,拆分合并后把文件交给用户
aliases: ["读 PDF", "PDF 转文字", "合并 PDF"]
terms: ["PDF", "文档", "提取文字", "按页", "合并", "拆分", "扫描件"]
---
## 入口

先看文档在哪:
- 网上的公开链接:用 `web_navigate` 打开,内置浏览器能直接显示的,读页面就够了。
- 用户发来的附件:在 `/workspace/inbox/`。
- 网页上下载的文件:在 `/workspace/downloads/`,可以用 `web_downloads` 查下载记录。

## 步骤

V1 不提供 shell 或可执行脚本。文本文件使用 read_file / search_files 查看；PDF 等二进制文件不能仅凭文件名声称已读取。需要外部转换时请用户提供可读文本。
