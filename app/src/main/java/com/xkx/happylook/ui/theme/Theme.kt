package com.xkx.happylook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 配色取自纸与墨：底是纸白，字是墨黑，唯一的强调色是一点朱红。
 * 没有渐变、没有玻璃、没有发光——查看器的界面应该退到文件后面。
 */
private val InkBlack = Color(0xFF1C1B19)
private val PaperWhite = Color(0xFFFBFAF7)
private val PaperDim = Color(0xFFEFEDE6)
private val Vermilion = Color(0xFFB23A2E)
private val Moss = Color(0xFF4A5D45)

private val LightColors = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color(0xFFFFFBF8),
    primaryContainer = Color(0xFFF6DBD5),
    onPrimaryContainer = Color(0xFF3D0903),
    secondary = Moss,
    onSecondary = Color(0xFFFFFBF8),
    secondaryContainer = Color(0xFFDCE6D6),
    onSecondaryContainer = Color(0xFF17240F),
    tertiary = Color(0xFF3F5A6B),
    onTertiary = Color(0xFFFFFBF8),
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = PaperDim,
    onSurfaceVariant = Color(0xFF4E4B45),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F5F0),
    surfaceContainer = Color(0xFFF2EFE8),
    surfaceContainerHigh = Color(0xFFECE9E1),
    surfaceContainerHighest = Color(0xFFE6E2D9),
    outline = Color(0xFF7D7970),
    outlineVariant = Color(0xFFCFCABF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFBF8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE98C7C),
    onPrimary = Color(0xFF4A0D06),
    primaryContainer = Color(0xFF6B1B12),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFB4C4AA),
    onSecondary = Color(0xFF21331A),
    secondaryContainer = Color(0xFF35472D),
    onSecondaryContainer = Color(0xFFD0E1C6),
    tertiary = Color(0xFFA9C5D6),
    onTertiary = Color(0xFF12313F),
    background = Color(0xFF14130F),
    onBackground = Color(0xFFE7E3D8),
    surface = Color(0xFF14130F),
    onSurface = Color(0xFFE7E3D8),
    surfaceVariant = Color(0xFF33302A),
    onSurfaceVariant = Color(0xFFC7C1B5),
    surfaceContainerLowest = Color(0xFF0D0C09),
    surfaceContainerLow = Color(0xFF1B1A16),
    surfaceContainer = Color(0xFF201F1A),
    surfaceContainerHigh = Color(0xFF2A2822),
    surfaceContainerHighest = Color(0xFF35332B),
    outline = Color(0xFF948E82),
    outlineVariant = Color(0xFF48453C),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

/** 大圆角的纸感形状，沿用 Expressive 的尺度但去掉过分的胶囊化。 */
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
)

/**
 * 字阶。标题收紧字距、加大字重，正文放宽行距——
 * 阅读器里唯一值得花心思的就是字与留白。
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun HappyLookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
