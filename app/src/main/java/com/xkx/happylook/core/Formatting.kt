package com.xkx.happylook.core

import android.content.Context
import android.net.Uri
import java.util.Locale

/** 人类可读的大小，1024 进制，保留一位小数但去掉多余的 .0。 */
fun formatSize(bytes: Long?): String {
    if (bytes == null || bytes < 0) return "大小未知"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val text = if (value >= 100) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)
    return "${text.removeSuffix(".0")} ${units[unit]}"
}

/** 扩展名，用于标题下的那行灰字。没有扩展名时返回空串而不是「未知」。 */
fun extensionOf(name: String): String {
    val idx = name.lastIndexOf('.')
    if (idx <= 0 || idx == name.length - 1) return ""
    return name.substring(idx + 1).lowercase()
}

/** 类别到中文标签，用于列表和顶栏的说明文字。 */
fun FileKind.label(): String = when (this) {
    FileKind.TEXT -> "文本"
    FileKind.MARKDOWN -> "Markdown"
    FileKind.CODE -> "代码"
    FileKind.PDF -> "PDF"
    FileKind.DOCX, FileKind.LEGACY_DOC, FileKind.ODT, FileKind.RTF -> "文档"
    FileKind.XLSX, FileKind.LEGACY_XLS -> "表格"
    FileKind.PPTX -> "演示"
    FileKind.IMAGE -> "图片"
    FileKind.ANIMATED_IMAGE -> "动图"
    FileKind.VIDEO -> "视频"
    FileKind.AUDIO -> "音频"
    FileKind.ZIP -> "压缩包"
    FileKind.EPUB -> "电子书"
    FileKind.STL -> "3D 模型"
    FileKind.FONT -> "字体"
    FileKind.UNKNOWN -> "未知"
}

/** 该类别是否由内置网页渲染层处理。决定走 WebView 还是原生视图。 */
fun FileKind.usesWebRenderer(): Boolean = when (this) {
    FileKind.PDF, FileKind.DOCX, FileKind.LEGACY_DOC, FileKind.ODT, FileKind.RTF,
    FileKind.XLSX, FileKind.LEGACY_XLS, FileKind.PPTX, FileKind.MARKDOWN,
    FileKind.EPUB, FileKind.STL, FileKind.ANIMATED_IMAGE,
    -> true
    else -> false
}

/** 安全解析，畸形字符串返回 null 而不是抛异常。 */
fun parseUriSafe(raw: String): Uri? = runCatching { Uri.parse(raw) }.getOrNull()

/** 读取来源应用的名称，拿不到就返回 null，界面上不显示这一行。 */
fun sourceAppLabel(context: Context, packageName: String?): String? {
    if (packageName.isNullOrBlank()) return null
    return runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()
}
