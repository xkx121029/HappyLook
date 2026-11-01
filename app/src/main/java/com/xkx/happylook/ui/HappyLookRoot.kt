package com.xkx.happylook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.ui.home.HomeScreen
import com.xkx.happylook.ui.viewer.ViewerScreen

/** 当前显示的页面。 */
private sealed interface Route {
    data object Home : Route
    data class File(val target: ViewerTarget) : Route
}

@Composable
fun HappyLookRoot(
    incomingTargets: List<ViewerTarget>,
    onIncomingConsumed: () -> Unit,
) {
    var route by remember { mutableStateOf<Route>(Route.Home) }

    LaunchedEffect(incomingTargets) {
        val first = incomingTargets.firstOrNull() ?: return@LaunchedEffect
        route = Route.File(first)
        onIncomingConsumed()
    }

    // 系统返回键在查看器里退回首页，而不是直接退出应用
    BackHandler(enabled = route is Route.File) { route = Route.Home }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            // 从下往上推进再淡出，符合阅读器「翻过一页」的感觉；
            // 位移只取屏高的 1/12，够被感知但不喧哗。
            (slideInVertically(tween(240)) { it / 12 } + fadeIn(tween(200)))
                .togetherWith(slideOutVertically(tween(180)) { -it / 16 } + fadeOut(tween(140)))
        },
        label = "route",
    ) { current ->
        when (current) {
            is Route.Home -> HomeScreen(
                onOpenFile = { target -> route = Route.File(target) },
            )
            is Route.File -> ViewerScreen(
                target = current.target,
                onBack = { route = Route.Home },
            )
        }
    }
}
