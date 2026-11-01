package com.xkx.happylook.core

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 统一的内容读取入口。
 * 所有读取都走 content:// 流，不假设文件在磁盘上的真实路径——
 * 分区存储和第三方文件提供方都不保证路径可访问。
 */
object FileAccess {

    fun open(context: Context, uri: Uri): InputStream? =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()

    fun openBuffered(context: Context, uri: Uri): InputStream? =
        runCatching { context.contentResolver.openInputStream(uri)?.buffered() }.getOrNull()

    /** 读取前 [limit] 字节；超长文件只读开头，避免一次性把内存吃满。 */
    fun readHead(context: Context, uri: Uri, limit: Int = 2 * 1024 * 1024): ByteArray? {
        return open(context, uri)?.use { input ->
            val buffer = ByteArray(limit)
            var total = 0
            while (total < limit) {
                val read = input.read(buffer, total, limit - total)
                if (read <= 0) break
                total += read
            }
            if (total == 0) null else buffer.copyOf(total)
        }
    }

    /** 完整读取，仅用于小文件（office 解析前先判大小）。 */
    fun readAll(context: Context, uri: Uri, maxBytes: Long = 64L * 1024 * 1024): ByteArray? {
        return open(context, uri)?.use { it.readBytes() }?.takeIf { it.size <= maxBytes }
    }

    data class TextContent(
        val text: String,
        val charset: Charset,
        val truncated: Boolean,
        val totalBytes: Long,
    )

    /**
     * 解码文本。顺序：BOM → 严格 UTF-8 → GBK → Latin-1 兜底。
     * 国产文件中 GBK 编码的日志和 CSV 很常见，光靠 UTF-8 会把整篇读成乱码。
     */
    fun decodeText(bytes: ByteArray, truncated: Boolean, totalBytes: Long): TextContent {
        bomCharset(bytes)?.let { cs ->
            val start = bomLength(bytes)
            return TextContent(String(bytes, start, bytes.size - start, cs), cs, truncated, totalBytes)
        }
        // 严格解码：遇到非法字节序列会抛异常，正好用来判断是否为合法 UTF-8。
        val strictUtf8 = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull()
        if (strictUtf8 != null) {
            return TextContent(strictUtf8, Charsets.UTF_8, truncated, totalBytes)
        }
        val gbk = runCatching { String(bytes, Charset.forName("GBK")) }.getOrNull()
        if (gbk != null && gbk.none { it == '\uFFFD' }) {
            return TextContent(gbk, Charset.forName("GBK"), truncated, totalBytes)
        }
        return TextContent(String(bytes, Charsets.ISO_8859_1), Charsets.ISO_8859_1, truncated, totalBytes)
    }

    private fun bomCharset(bytes: ByteArray): Charset? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) return Charsets.UTF_8
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return Charsets.UTF_16LE
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return Charsets.UTF_16BE
        return null
    }

    private fun bomLength(bytes: ByteArray): Int = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() -> 3
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> 2
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> 2
        else -> 0
    }

    /** 只读图片头部拿到宽高，用于大图分块与预占位，不整张解码。 */
    fun imageSize(context: Context, uri: Uri): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(context, uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth > 0 && opts.outHeight > 0) return opts.outWidth to opts.outHeight
        return null
    }
}
