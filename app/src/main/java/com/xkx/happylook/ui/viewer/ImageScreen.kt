package com.xkx.happylook.ui.viewer

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xkx.happylook.core.ViewerTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片查看器。
 *
 * 大图先按目标尺寸降采样再解码，避免一张 4000 万像素的照片直接把内存吃爆；
 * 原始尺寸明显大于屏幕时保留足够解码余量，放大时依然清晰。
 */
@Composable
fun ImageScreen(target: ViewerTarget) {
    val context = LocalContext.current
    val uri = remember(target.uri) { android.net.Uri.parse(target.uri) }

    val result by produceState<Result<ImageBitmap>?>(null, target.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                val w = bounds.outWidth
                val h = bounds.outHeight
                if (w <= 0 || h <= 0) error("图片尺寸异常")

                // 目标边长：屏幕最长边 * 2，兼顾清晰与内存
                val targetEdge = 2048
                var sample = 1
                while (w / (sample * 2) >= targetEdge && h / (sample * 2) >= targetEdge) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: error("无法解码图片")
                bitmap.asImageBitmap()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            result == null -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
            result!!.isFailure -> ViewerMessage(
                title = "无法显示这张图片",
                detail = "格式可能不受系统支持，比如部分 HEIC 或 AVIF 文件",
            )
            else -> ZoomableImage(result!!.getOrThrow())
        }
    }
}

/** 双指缩放 + 拖动 + 双击复位。缩放范围限制在 1x–6x，避免拖丢。 */
@Composable
private fun ZoomableImage(image: ImageBitmap) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 6f)
                    scale = next
                    offset = if (next <= 1.001f) {
                        Offset.Zero
                    } else {
                        // 限制平移范围，保证图片不会被拖出可视区
                        val maxX = (size.width * (next - 1)) / 2
                        val maxY = (size.height * (next - 1)) / 2
                        Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.001f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
