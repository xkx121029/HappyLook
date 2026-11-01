package com.xkx.happylook.ui.viewer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.xkx.happylook.core.ViewerTarget

/**
 * 音视频查看器。
 *
 * 用 Media3（ExoPlayer），格式覆盖比系统 MediaPlayer 宽：
 * MKV、FLV、TS 这些系统播放器经常不认的容器它都能解。
 * 播放器实例随组合创建与释放，退出即停止，不留后台占用。
 */
@Composable
fun MediaScreen(target: ViewerTarget) {
    val context = LocalContext.current
    val uri = remember(target.uri) { Uri.parse(target.uri) }

    val player = remember(target.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (target.kind == com.xkx.happylook.core.FileKind.AUDIO) {
                MaterialTheme.colorScheme.background
            } else {
                android.graphics.Color.BLACK.let { androidx.compose.ui.graphics.Color(it) }
            }),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // 顶部留出空间，避免控制条压住文件信息
                    setPadding(0, dpToPx(72), 0, 0)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            modifier = Modifier.fillMaxSize().padding(top = 0.dp),
        )
    }
}

private fun androidx.media3.ui.PlayerView.dpToPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).toInt()
