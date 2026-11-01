package com.xkx.happylook.ui.viewer

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xkx.happylook.core.FileAccess
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.core.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/** 压缩包中的一个条目。目录条目单独标记，界面上显示成文件夹。 */
data class ZipEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
)

private sealed interface ArchiveState {
    data object Loading : ArchiveState
    data class Ready(val entries: List<ZipEntry>) : ArchiveState
    data class Failed(val message: String) : ArchiveState
}

/**
 * 压缩包浏览器。
 *
 * 只支持 zip 系（含 jar/apk/cbz），7z 和 rar 需要额外解压库暂未内置。
 * 条目列表按层级平铺，靠缩进表达嵌套，比一层层点进去更适合手机屏幕。
 */
@Composable
fun ArchiveScreen(target: ViewerTarget) {
    val context = LocalContext.current
    val uri = remember(target.uri) { Uri.parse(target.uri) }

    val state by produceState<ArchiveState>(ArchiveState.Loading, target.uri) {
        value = ArchiveState.Loading
        value = withContext(Dispatchers.IO) { readEntries(context, uri) }
    }

    when (val s = state) {
        is ArchiveState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
        }
        is ArchiveState.Failed -> ViewerMessage(s.message, null)
        is ArchiveState.Ready -> ArchiveList(target = target, entries = s.entries)
    }
}

@Composable
private fun ArchiveList(target: ViewerTarget, entries: List<ZipEntry>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 92.dp, bottom = 48.dp),
    ) {
        item {
            Text(
                text = "共 ${entries.count { !it.isDirectory }} 个文件",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
        }
        items(entries, key = { it.path }) { entry ->
            ZipRow(entry)
        }
    }
}

@Composable
private fun ZipRow(entry: ZipEntry) {
    val depth = entry.path.count { it == '/' }.coerceAtMost(4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp + (depth * 14).dp, end = 20.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Rounded.Folder
            else Icons.AutoMirrored.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.padding(2.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            if (!entry.isDirectory && entry.size > 0) {
                Text(
                    text = formatSize(entry.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** 读出全部条目。只读中央目录区的名字与大小，不解压内容，几百个文件也是毫秒级。 */
private fun readEntries(context: Context, uri: Uri): ArchiveState {
    return runCatching {
        val entries = mutableListOf<ZipEntry>()
        FileAccess.openBuffered(context, uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var guard = 0
                while (guard < 20000) {
                    val e = zip.nextEntry ?: break
                    guard++
                    val path = e.name
                    if (path.isNullOrBlank()) continue
                    entries.add(
                        ZipEntry(
                            path = path,
                            name = path.trimEnd('/').substringAfterLast('/'),
                            size = e.size.takeIf { it > 0 } ?: 0L,
                            isDirectory = path.endsWith("/"),
                        ),
                    )
                    zip.closeEntry()
                }
            }
        }
        if (entries.isEmpty()) {
            ArchiveState.Failed("压缩包是空的，或者不是 zip 格式")
        } else {
            ArchiveState.Ready(entries.sortedWith(compareBy({ !it.isDirectory }, { it.path })))
        }
    }.getOrElse { ArchiveState.Failed("无法读取这个压缩包\n${it.message ?: ""}") }
}
