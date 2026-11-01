package com.xkx.happylook.ui.viewer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.xkx.happylook.core.FileKind
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.web.SandboxWebView

/**
 * 按类别分发到具体查看器。
 *
 * 分工原则：能用系统原生能力做好的（图片、音视频、纯文本）走原生，
 * 需要排版引擎的（PDF、Office、Markdown、EPUB、STL）走内置沙箱 WebView。
 */
@Composable
fun ViewerScreen(
    target: ViewerTarget,
    onBack: () -> Unit,
) {
    val uri = remember(target.uri) { Uri.parse(target.uri) }

    ViewerScaffold(target = target, onBack = onBack) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (target.kind) {
                FileKind.TEXT -> TextScreen(target)
                FileKind.CODE -> TextScreen(target, monospace = true, withLineNumbers = true)
                FileKind.IMAGE -> ImageScreen(target)
                FileKind.VIDEO, FileKind.AUDIO -> MediaScreen(target)

                FileKind.MARKDOWN -> SandboxWebView(
                    uri = uri,
                    mimeType = "text/markdown",
                    htmlPath = "markdown.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.PDF -> SandboxWebView(
                    uri = uri,
                    mimeType = "application/pdf",
                    htmlPath = "pdf.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.DOCX, FileKind.ODT, FileKind.RTF -> SandboxWebView(
                    uri = uri,
                    mimeType = target.mimeType,
                    htmlPath = "docx.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.XLSX, FileKind.LEGACY_XLS -> SandboxWebView(
                    uri = uri,
                    mimeType = target.mimeType,
                    htmlPath = "xlsx.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.PPTX -> SandboxWebView(
                    uri = uri,
                    mimeType = target.mimeType,
                    htmlPath = "pptx.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.ANIMATED_IMAGE -> SandboxWebView(
                    uri = uri,
                    mimeType = target.mimeType ?: "image/gif",
                    htmlPath = "image.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.EPUB -> SandboxWebView(
                    uri = uri,
                    mimeType = "application/epub+zip",
                    htmlPath = "epub.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.STL -> SandboxWebView(
                    uri = uri,
                    mimeType = "model/stl",
                    htmlPath = "stl.html",
                    modifier = Modifier.fillMaxSize(),
                )
                FileKind.ZIP -> ArchiveScreen(target)

                FileKind.LEGACY_DOC -> ViewerMessage(
                    title = "暂不支持 Word 97-2003 格式",
                    detail = "请用 Word 或 WPS 另存为 .docx 后再打开",
                )
                FileKind.FONT -> ViewerMessage(
                    title = "暂不支持字体预览",
                    detail = "字体文件无法直接显示内容",
                )
                FileKind.UNKNOWN -> ViewerMessage(
                    title = "无法识别这个文件",
                    detail = "没有扩展名，内容也不像常见的文件格式",
                )
            }
        }
    }
}
