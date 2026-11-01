package com.xkx.happylook.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xkx.happylook.core.TargetFactory
import com.xkx.happylook.core.ViewerTarget

/**
 * 首页。
 *
 * 这个应用的主入口不是首页，而是系统分享与「打开方式」。
 * 所以首页只做一件事：在用户主动打开应用时，把文件选进来。
 * 不放最近文件、不放推荐、不放任何需要额外权限的东西。
 */
@Composable
fun HomeScreen(onOpenFile: (ViewerTarget) -> Unit) {
    val context = LocalContext.current

    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // 授权持久化，避免个别提供方在本次会话结束后再次询问
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        TargetFactory.fromUri(context, uri)?.let(onOpenFile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.height(56.dp))

        Text(
            text = "悦览",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "在其它应用里分享文件，或长按文件选「打开方式」，\n就能在这里直接看清楚它。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        PickCard { pickDocument.launch(arrayOf("*/*")) }

        Spacer(Modifier.weight(1f))

        Column(
            Modifier.padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "不申请任何权限，不含网络功能",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            Text(
                text = "文件只在你亲手选中时才被读取，全部解析都在本机完成。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

/** 主操作卡片：虚线描边，暗示「把文件放进来的地方」。 */
@Composable
private fun PickCard(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "dash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box {
            val stroke = MaterialTheme.colorScheme.outlineVariant
            val inset = 9.dp
            val radius = 18.dp
            Canvas(Modifier.fillMaxWidth().height(132.dp)) {
                drawRoundRect(
                    color = stroke,
                    topLeft = Offset(inset.toPx(), inset.toPx()),
                    size = Size(
                        width = size.width - inset.toPx() * 2,
                        height = size.height - inset.toPx() * 2,
                    ),
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            // 虚线段长与间隙，整体平移形成走马灯效果
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            phase.dp.toPx(),
                        ),
                    ),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "选一个文件",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "PDF · Word · Excel · 图片 · 音视频 · 压缩包",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
