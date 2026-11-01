package com.xkx.happylook.core

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.InputStream

object FileKinds {

    private val imageExt = setOf(
        "jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "avif", "ico", "tif", "tiff", "jfif",
    )
    private val animatedExt = setOf("gif", "apng")
    private val videoExt = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "3gp", "3g2", "avi", "flv", "ts", "m2ts", "ogv", "wmv",
    )
    private val audioExt = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "amr", "wma", "ape", "mid", "midi",
    )
    private val codeExt = setOf(
        "kt", "kts", "java", "py", "js", "mjs", "cjs", "ts", "tsx", "jsx", "c", "h", "cpp", "hpp",
        "cc", "cs", "go", "rs", "rb", "php", "swift", "m", "mm", "scala", "lua", "pl", "r", "jl",
        "dart", "sh", "bash", "zsh", "ps1", "bat", "cmd", "sql", "gradle", "groovy", "cmake",
        "html", "htm", "xhtml", "css", "scss", "sass", "less", "vue", "svelte", "toml", "ini",
        "cfg", "conf", "properties", "xml", "json", "json5", "yaml", "yml", "proto", "graphql",
        "dockerfile", "makefile", "mk", "asm", "s", "vb", "pas", "f90", "tex", "sol", "zig", "nim",
    )
    private val textExt = setOf(
        "txt", "log", "csv", "tsv", "srt", "vtt", "ass", "ssa", "nfo", "m3u", "m3u8", "diff", "patch",
        "readme", "license", "gitignore", "env", "srt2",
    )

    /** 扩展名到类别的映射。未知扩展名返回 null，交给后续 MIME 与嗅探决定。 */
    fun byExtension(ext: String): FileKind? {
        val e = ext.lowercase()
        return when {
            e == "md" || e == "markdown" || e == "mdown" || e == "mkd" -> FileKind.MARKDOWN
            e == "pdf" -> FileKind.PDF
            e == "docx" || e == "docm" || e == "dotx" || e == "dotm" -> FileKind.DOCX
            e == "doc" || e == "dot" -> FileKind.LEGACY_DOC
            e == "odt" || e == "ott" -> FileKind.ODT
            e == "rtf" -> FileKind.RTF
            e == "xlsx" || e == "xlsm" || e == "xltx" || e == "xlsb" -> FileKind.XLSX
            e == "xls" || e == "xlt" -> FileKind.LEGACY_XLS
            e == "pptx" || e == "pptm" || e == "ppsx" || e == "potx" -> FileKind.PPTX
            e == "zip" || e == "jar" || e == "apk" || e == "cbz" || e == "cb7" || e == "7z" ||
                e == "rar" || e == "tar" || e == "gz" || e == "tgz" || e == "xz" || e == "bz2" -> FileKind.ZIP
            e == "epub" -> FileKind.EPUB
            e == "stl" -> FileKind.STL
            e == "ttf" || e == "otf" || e == "woff" || e == "woff2" -> FileKind.FONT
            e in animatedExt -> FileKind.ANIMATED_IMAGE
            e in imageExt -> FileKind.IMAGE
            e in videoExt -> FileKind.VIDEO
            e in audioExt -> FileKind.AUDIO
            e in codeExt -> FileKind.CODE
            e in textExt -> FileKind.TEXT
            else -> null
        }
    }

    fun byMime(mime: String?): FileKind? {
        val m = mime?.lowercase()?.substringBefore(';')?.trim() ?: return null
        return when {
            m.startsWith("image/gif") || m == "image/apng" -> FileKind.ANIMATED_IMAGE
            m.startsWith("image/") -> FileKind.IMAGE
            m.startsWith("video/") -> FileKind.VIDEO
            m.startsWith("audio/") -> FileKind.AUDIO
            m == "application/pdf" -> FileKind.PDF
            m == "text/markdown" -> FileKind.MARKDOWN
            m.contains("wordprocessingml") || m == "application/vnd.ms-word.document.macroenabled.12" -> FileKind.DOCX
            m == "application/msword" -> FileKind.LEGACY_DOC
            m.contains("spreadsheetml") || m == "application/vnd.ms-excel" -> FileKind.XLSX
            m.contains("presentationml") || m == "application/vnd.ms-powerpoint" -> FileKind.PPTX
            m == "application/vnd.oasis.opendocument.text" -> FileKind.ODT
            m == "application/rtf" -> FileKind.RTF
            m == "application/epub+zip" -> FileKind.EPUB
            m == "model/stl" || m == "application/sla" -> FileKind.STL
            m.startsWith("font/") || m == "application/font-woff" -> FileKind.FONT
            m == "application/zip" || m == "application/x-zip-compressed" -> FileKind.ZIP
            m.startsWith("text/") -> FileKind.TEXT
            m == "application/json" || m == "application/xml" -> FileKind.CODE
            else -> null
        }
    }

    /**
     * 最终判定。返回 kind 与规范化后的扩展名。
     * 顺序：扩展名 → MIME → 首字节嗅探（文本 / zip / pdf / png…）。
     */
    fun resolve(
        displayName: String,
        mimeType: String?,
        sniff: (() -> InputStream?)? = null,
    ): Pair<FileKind, String> {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        byExtension(ext)?.let { return it to ext }
        byMime(mimeType)?.let { return it to ext }
        // 没有可用线索时读首字节。魔数判断比 MIME 更可靠，尤其是国产 App 的分享。
        sniff?.invoke()?.use { input ->
            val head = ByteArray(64)
            val read = input.read(head).coerceAtLeast(0)
            if (read >= 4) {
                head.copyOf(read).let { b ->
                    magicKind(b)?.let { return it to ext }
                }
            }
            if (read > 0 && looksLikeText(head, read)) return FileKind.TEXT to ext
        }
        return FileKind.UNKNOWN to ext
    }

    private fun magicKind(b: ByteArray): FileKind? {
        fun at(i: Int) = b[i].toInt() and 0xFF
        return when {
            at(0) == 0x25 && at(1) == 0x50 && at(2) == 0x44 && at(3) == 0x46 -> FileKind.PDF
            at(0) == 0x50 && at(1) == 0x4B && (at(2) == 0x03 || at(2) == 0x05 || at(2) == 0x07) -> FileKind.ZIP
            at(0) == 0xFF && at(1) == 0xD8 -> FileKind.IMAGE
            at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> FileKind.IMAGE
            at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 -> FileKind.ANIMATED_IMAGE
            at(0) == 0x52 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x46 &&
                b.size >= 12 && b[8] == 'W'.code.toByte() -> FileKind.AUDIO
            at(0) == 0x1A && at(1) == 0x45 && at(2) == 0xDF && at(3) == 0xA3 -> FileKind.VIDEO
            at(0) == 0xD0 && at(1) == 0xCF && at(2) == 0x11 && at(3) == 0xE0 -> FileKind.LEGACY_DOC
            else -> null
        }
    }

    /** 粗判是否文本：不含 NUL，且控制字符占比很低。 */
    fun looksLikeText(bytes: ByteArray, len: Int): Boolean {
        if (len == 0) return false
        var control = 0
        for (i in 0 until len) {
            val v = bytes[i].toInt() and 0xFF
            if (v == 0) return false
            if (v < 0x09 || (v in 0x0E..0x1F)) control++
        }
        return control * 100 / len < 3
    }
}

/** 从 content URI 读取元信息。渠道不可靠时给出合理兜底值。 */
object UriMeta {

    data class Meta(val displayName: String, val sizeBytes: Long?)

    fun read(resolver: ContentResolver, uri: Uri): Meta {
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "未命名文件"
        }
        if (size == null || size!! <= 0L) {
            // 部分渠道不给 SIZE，退回按描述符长度探测。
            runCatching {
                resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    val len = afd.length
                    if (len > 0) size = len
                }
            }
        }
        return Meta(name!!, size)
    }

    fun guessMime(resolver: ContentResolver, uri: Uri, name: String): String? {
        val fromResolver = runCatching { resolver.getType(uri) }.getOrNull()
        if (!fromResolver.isNullOrBlank() && fromResolver != "application/octet-stream") return fromResolver
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: fromResolver
    }
}
