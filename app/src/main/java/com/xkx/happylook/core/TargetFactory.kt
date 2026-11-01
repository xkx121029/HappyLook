package com.xkx.happylook.core

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 把各种来源的 Intent 统一成一组 [ViewerTarget]。
 *
 * 覆盖三种入口：
 * 1. ACTION_VIEW —— 文件管理器里「用悦览打开」
 * 2. ACTION_SEND / ACTION_SEND_MULTIPLE —— 其他应用分享过来
 * 3. 应用内自己选的 SAF 文档
 */
object TargetFactory {

    fun fromIntent(context: Context, intent: Intent?): List<ViewerTarget> {
        if (intent == null) return emptyList()
        val uris = collectUris(intent)
        if (uris.isEmpty()) return emptyList()

        // 单文件时尝试拿来源包名，多文件时无从归属，统一留空。
        val sourcePackage = if (uris.size == 1) {
            runCatching { (context as? android.app.Activity)?.referrer?.host }.getOrNull()
        } else {
            null
        }

        return uris.mapNotNull { uri -> fromUri(context, uri, sourcePackage) }
    }

    private fun collectUris(intent: Intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> listOfNotNull(
                @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri,
            )
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.toList() ?: emptyList()
            }
            else -> listOfNotNull(intent.data)
        }
    }

    fun fromUri(context: Context, uri: Uri, sourcePackage: String? = null): ViewerTarget? {
        val resolver = context.contentResolver
        // 目标不可读时尽早失败，避免后续每个环节都要处理异常。
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        val meta = runCatching { UriMeta.read(resolver, uri) }.getOrNull() ?: return null
        val resolvedMime = UriMeta.guessMime(resolver, uri, meta.displayName)
        val (kind, _) = FileKinds.resolve(
            displayName = meta.displayName,
            mimeType = resolvedMime,
            sniff = { runCatching { resolver.openInputStream(uri) }.getOrNull() },
        )
        return ViewerTarget(
            uri = uri.toString(),
            displayName = meta.displayName,
            mimeType = resolvedMime ?: mime,
            sizeBytes = meta.sizeBytes,
            kind = kind,
            sourcePackage = sourcePackage,
        )
    }
}
