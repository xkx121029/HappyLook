# 悦览 HappyLook

一个**不接受任何权限、不联网、只在本机解析**的 Android 文件查看器。

系统里点「打开方式」选它，或者在别的应用里把文件分享给它，就能直接看清内容 —— 不需要给存储权限，文件也不会离开这台手机。

## 为什么单独做一个

现成的查看器要么只认一种格式，要么一上来就要「访问所有文件」的权限。悦览反过来：

- **零权限**：`AndroidManifest.xml` 里没有任何 `uses-permission` 声明，连 INTERNET 都没有。
- **零上传**：没有网络能力，渲染库全部离线内置在 APK 里。
- **只读**：文件通过系统授权的一次性 URI 进来，只读不写，不落副本。

## 支持的格式

| 类别 | 格式 | 渲染方式 |
| --- | --- | --- |
| 文本 | txt / log / ini / csv | 原生 LazyColumn，自动编码识别（BOM → UTF-8 → GBK → Latin-1） |
| 代码 | kt / java / py / js / ts / c / cpp / go / rs / json / xml / yaml … | 原生 + 等宽字体 + 行号 |
| Markdown | md / markdown | marked + 白名单净化 |
| PDF | pdf | pdf.js（内置，IntersectionObserver 懒渲染） |
| 文档 | docx / odt / rtf | docx-preview（内置） |
| 表格 | xlsx / xls | SheetJS（内置，行号列 + 冻结表头） |
| 演示 | pptx | 自研 OOXML 解析（图层定位、主题色板、文本框换行） |
| 电子书 | epub | JSZip 解包 + 章节流式排版，带目录与阅读进度 |
| 图片 | png / jpg / webp / bmp … | 原生，尺寸预读 + 降采样，支持双指缩放 |
| 动图 | gif / 动画 webp | `<img>` 原生动图播放 |
| 音视频 | mp4 / mkv / mp3 / flac … | Media3 ExoPlayer |
| 压缩包 | zip / jar / apk / cbz | 直接读中央目录，平铺展示层级 |
| 3D | stl | 自研 WebGL 渲染器，朗伯着色 + 边缘光 |

`.doc`、字体文件等暂不支持，界面里会明确说明原因，而不是给个空白。

## 安全模型

排版引擎都在 WebView 里跑，但页面并不是从网络加载的：

1. 自建虚拟源 `https://happylook.local`，`shouldInterceptRequest` 拦截所有请求。
2. 除 `assets/viewer/**` 与当前文件流之外，一律返回 404。
3. DOM Storage、数据库、文件访问、多窗口全部关闭。
4. 页面内不允许任何跳转，外链出不去。

叠加「没有 INTERNET 权限」这一层，即使渲染库里有意外发起的请求也无法离开设备。

## 视觉

Material 3 的形状与字阶，配色取自纸与墨：纸白 `#FBFAF7`、墨黑 `#1C1B19`、朱红 `#B23A2E`、苔绿 `#4A5D45`，暗色对齐 `#14130F`。动效用弹簧而不是线性缓动，翻页位移只取屏高的 1/12。

## 构建

```bash
# 需要 Android SDK（compileSdk 36）与 JDK 17+
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

技术栈：Kotlin 2.2 + Jetpack Compose + Material 3，AGP 8.13.2，minSdk 26。

内置渲染库（`app/src/main/assets/viewer/lib`，合计约 2.8 MB）：

- pdf.js legacy build（`pdf.min.mjs` / `pdf.worker.min.mjs`）
- docx-preview + JSZip + SheetJS + marked

## 已知限制

- 加密 PDF 暂不支持；超大表格超过 3000 行 / 200 列会截断并提示。
- 压缩包目前只浏览条目，不支持解压或直接打开包内文件。
- `.ppt`（97-2003 二进制格式）与 `.doc` 未实现。

## 许可证

MIT，见 [LICENSE](LICENSE)。
