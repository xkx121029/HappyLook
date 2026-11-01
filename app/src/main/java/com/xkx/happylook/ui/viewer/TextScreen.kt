package com.xkx.happylook.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xkx.happylook.core.FileAccess
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.core.formatSize
import com.xkx.happylook.ui.theme.CodeFontFamily
import com.xkx.happylook.ui.theme.ReadingFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val READ_LIMIT = 4 * 1024 * 1024

private data class TextState(
    val lines: List<String> = emptyList(),
    val charset: String = "UTF-8",
    val truncated: Boolean = false,
    val totalBytes: Long = 0,
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * 纯文本 / 代码查看器。
 *
 * 用 LazyColumn 按行虚拟化，几万行的日志也不会卡。
 * 编码判断顺序是 UTF-8 严格校验 → GBK → Latin-1，
 * 国内的文件很多是 GBK，硬按 UTF-8 读会整篇乱码。
 */
@Composable
fun TextScreen(
    target: ViewerTarget,
    monospace: Boolean = false,
    withLineNumbers: Boolean = false,
) {
    val context = LocalContext.current
    val uri = remember(target.uri) { android.net.Uri.parse(target.uri) }

    val state by produceState(TextState(), target.uri) {
        value = TextState(loading = true)
        value = withContext(Dispatchers.IO) {
            val bytes = FileAccess.readHead(context, uri, READ_LIMIT)
                ?: return@withContext TextState(loading = false, error = "读取文件失败")
            if (bytes.isEmpty()) {
                return@withContext TextState(loading = false, lines = listOf(""), charset = "UTF-8")
            }
            val total = target.sizeBytes ?: bytes.size.toLong()
            val truncated = bytes.size >= READ_LIMIT && total > READ_LIMIT
            // 截断点可能落在多字节字符中间，退到最近的换行处再解码，避免末尾一个乱码字
            val usable = if (truncated) {
                val cut = bytes.lastIndexOf('\n'.code.toByte()).let { if (it > 0) it else bytes.size }
                bytes.copyOf(cut)
            } else {
                bytes
            }
            val decoded = FileAccess.decodeText(usable, truncated, total)
            TextState(
                lines = decoded.text.split('\n'),
                charset = decoded.charset.name(),
                truncated = truncated,
                totalBytes = total,
                loading = false,
            )
        }
    }

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
        }

        state.error != null -> ViewerMessage(state.error!!, null)

        else -> TextContent(state, monospace, withLineNumbers)
    }
}

@Composable
private fun TextContent(state: TextState, monospace: Boolean, withLineNumbers: Boolean) {
    val gutterWidth = remember(state.lines.size) {
        when {
            !withLineNumbers -> 0.dp
            state.lines.size < 100 -> 34.dp
            state.lines.size < 1000 -> 44.dp
            state.lines.size < 10000 -> 54.dp
            else -> 64.dp
        }
    }
    val hScroll = rememberScrollState()
    val family: FontFamily = if (monospace) CodeFontFamily else ReadingFontFamily
    val fontSize = if (monospace) 13.sp else 15.sp
    val lineHeight = if (monospace) 21.sp else 26.sp

    Column(Modifier.fillMaxSize()) {
        // 顶部给浮动的信息条让出空间
        Box(Modifier.padding(top = 96.dp))

        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .horizontalScroll(hScroll),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 24.dp,
                    bottom = 96.dp,
                ),
            ) {
                itemsIndexed(state.lines) { index, line ->
                    Row {
                        if (withLineNumbers) {
                            Text(
                                text = (index + 1).toString(),
                                fontFamily = CodeFontFamily,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(gutterWidth - 10.dp),
                            )
                            Box(Modifier.width(10.dp))
                        }
                        Text(
                            text = line.ifEmpty { " " },
                            fontFamily = family,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (state.truncated) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "内容较长，只显示前 4 MB",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "文件共 ${formatSize(state.totalBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}
