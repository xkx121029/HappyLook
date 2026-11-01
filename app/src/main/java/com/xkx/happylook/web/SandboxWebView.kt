package com.xkx.happylook.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 沙箱 WebView 容器。
 *
 * 安全约束：
 * - JavaScript 打开，但 DOM Storage / 数据库关闭，页面没有持久化痕迹
 * - 文件访问全部关闭，页面只能用 content provider 直给的流
 * - 禁止多窗口、禁止地理位置、禁止混入非 https 的内容
 * - shouldInterceptRequest 兜住所有请求，只回答本地资源
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxWebView(
    uri: Uri,
    mimeType: String?,
    htmlPath: String,
    modifier: Modifier = Modifier,
    onReady: ((WebView) -> Unit)? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val darkTheme = isNightMode(context)
    val router = remember(uri, mimeType) { ViewerAssetRouter(context, uri, mimeType) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(if (darkTheme) Color.parseColor("#14130F") else Color.parseColor("#FBFAF7"))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = false
                databaseEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = false
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                // 禁止把 file:// 当作可访问来源，页面只有 https://happylook.local 这一个源
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                textZoom = 100
            }
            isLongClickable = false
            isHapticFeedbackEnabled = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = router.respond(request.url.toString())

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = true // 页面内不允许任何跳转

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError,
                ) {
                    android.util.Log.w(
                        "HappyLook",
                        "资源加载失败 ${request.url} : ${error.description}",
                    )
                }

                override fun onPageFinished(view: WebView, url: String) {
                    onReady?.invoke(view)
                }
            }
            // 页面里的报错要能在 logcat 里看到，否则渲染失败只能靠猜
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage): Boolean {
                    android.util.Log.d(
                        "HappyLook",
                        "页面 ${msg.messageLevel()} ${msg.sourceId()}:${msg.lineNumber()} ${msg.message()}",
                    )
                    return true
                }
            }
            loadUrl("${ViewerAssetRouter.ORIGIN}/viewer/$htmlPath")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

/** 读取系统暗色模式，页面配色需要跟着变。 */
private fun isNightMode(context: Context): Boolean {
    val mode = context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
}
