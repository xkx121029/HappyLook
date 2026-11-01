package com.xkx.happylook.web

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLDecoder

/**
 * 给 WebView 回答一切请求。
 *
 * 设计要点：页面只从 https://happylook.local/ 加载，所有资源都由这里从 assets
 * 或者当前文件的 content:// 流直接喂回去。除此之外的请求一律 404，
 * 这样即使渲染库里有意外发起的外链也出不去（本应用也没有 INTERNET 权限，双保险）。
 */
class ViewerAssetRouter(
    private val context: Context,
    private val fileUri: Uri?,
    private val fileMime: String?,
) {

    fun respond(url: String): WebResourceResponse? {
        val path = runCatching {
            Uri.parse(url).path?.removePrefix("/") ?: return denied()
        }.getOrNull() ?: return denied()

        return when {
            // 页面与脚本：assets/viewer 下的已知文件，形如 viewer/pdf.html、viewer/lib/pdf.min.mjs
            path.startsWith("viewer/") && path != FILE_STREAM_PATH ->
                asset(path.removePrefix("viewer/"))
            // 当前正在查看的文件本体。页面以 /viewer/ 为基准，所以两种形态都要认
            path == FILE_STREAM_PATH || path == FILE_STREAM_PATH.removePrefix("viewer/") ->
                fileStream()
            else -> denied()
        }
    }

    private fun asset(relative: String): WebResourceResponse? {
        if (relative.contains("..")) return denied()
        return runCatching {
            val stream = context.assets.open("viewer/$relative")
            WebResourceResponse(mimeFor(relative), charsetFor(relative), 200, "OK", headers(), stream)
        }.getOrElse { denied() }
    }

    private fun fileStream(): WebResourceResponse? {
        val uri = fileUri ?: return denied()
        val stream: InputStream = runCatching {
            context.contentResolver.openInputStream(uri)
        }.getOrNull() ?: return denied()
        return WebResourceResponse(
            fileMime ?: "application/octet-stream",
            null,
            200,
            "OK",
            headers().apply { put("Accept-Ranges", "none") },
            stream,
        )
    }

    private fun denied(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "utf-8",
        404,
        "Not Found",
        headers(),
        ByteArrayInputStream(ByteArray(0)),
    )

    private fun headers(): MutableMap<String, String> = mutableMapOf(
        // 资源全部本地，长缓存没有意义；关掉缓存避免 WebView 记住旧版本。
        "Cache-Control" to "no-store",
        "X-Content-Type-Options" to "nosniff",
    )

    private fun mimeFor(path: String): String = when (path.substringAfterLast('.', "")) {
        "html" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "wasm" -> "application/wasm"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }

    private fun charsetFor(path: String): String? = when (path.substringAfterLast('.', "")) {
        "html", "js", "mjs", "css", "json", "svg" -> "utf-8"
        else -> null
    }

    companion object {
        const val ORIGIN = "https://happylook.local"

        /** 页面用 new URL('file', location.href) 拼出来，实际路径落在 /viewer/file。 */
        private const val FILE_STREAM_PATH = "viewer/file"

        fun decode(seg: String): String = runCatching { URLDecoder.decode(seg, "UTF-8") }.getOrDefault(seg)
    }
}
