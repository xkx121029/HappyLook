package com.xkx.happylook.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.font.FontFamily

/**
 * 动效规范。Material 3 Expressive 的核心是「有重量的位移」，
 * 这里用同一组弹簧参数覆盖所有入场/转场，避免每个界面各调一套手感。
 */
object Motion {
    /** 元素在屏幕内移动时用，带一点点回弹，不夸张。 */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** 透明度、颜色这类不应该有物理位移的变化。 */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** 用于需要一点活泼感的元素，比如空态插图。 */
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessLow,
    )
}

/** 文本与代码统一用等宽字体，保证列对齐。 */
val CodeFontFamily: FontFamily = FontFamily.Monospace

/** 正文阅读用的字体族，跟随系统无衬线字体。 */
val ReadingFontFamily: FontFamily = FontFamily.SansSerif
