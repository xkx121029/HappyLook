package com.xkx.happylook.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xkx.happylook.core.FileKind
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.core.label
import com.xkx.happylook.ui.theme.Motion

/**
 * 查看器外壳：内容全屏铺满，顶部浮一根信息条。
 *
 * 顶部条默认收起成一行标题，点一下才展开看大小/来源。
 * 阅读类界面里，常驻的工具栏是在跟内容抢空间。
 */
@Composable
fun ViewerScaffold(
    target: ViewerTarget,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        content()

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(Motion.spatial()) { -it } + fadeIn(Motion.effects()),
            exit = slideOutVertically(Motion.spatial()) { -it } + fadeOut(Motion.effects()),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .widthIn(max = 720.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                onClick = { expanded = !expanded },
            ) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                // 让文字区也能整块点开，比只点小箭头好按
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.MiddleEllipsis,
                            )
                            Text(
                                text = buildString {
                                    append(target.kind.label())
                                    val size = com.xkx.happylook.core.formatSize(target.sizeBytes)
                                    if (size.isNotEmpty()) {
                                        append(" · ")
                                        append(size)
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Box(Modifier.widthIn(min = 8.dp))
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = slideInVertically(Motion.spatial()) { -it / 3 } + fadeIn(Motion.effects()),
                        exit = slideOutVertically(Motion.spatial()) { -it / 3 } + fadeOut(Motion.effects()),
                    ) {
                        Column(
                            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            DetailRow("格式", target.mimeType ?: target.kind.label())
                            DetailRow("大小", com.xkx.happylook.core.formatSize(target.sizeBytes))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 内容区底部留白，避免内容被系统手势条盖住。 */
val BottomContentPadding: Modifier
    @Composable get() = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
