package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datastore.UserSettings

data class LiquidGlassStyle(
    val backgroundColor: Color,
    val contentColor: Color,
    val subtitleColor: Color,
    val borderColor: Color,
    val borderStroke: BorderStroke?,
    val inputBackground: Color,
    val isLight: Boolean
)

@Composable
fun rememberLiquidGlassStyle(
    userSettings: UserSettings
): LiquidGlassStyle {
    val isEnabled = userSettings.glassmorphismEnabled
    val isLight = userSettings.wallpaperTone == "light"
    val alpha = userSettings.glassAlpha.coerceIn(0.15f, 0.95f)

    val bg = when {
        !isEnabled -> MaterialTheme.colorScheme.surface
        isLight -> Color(0xFFFFFFFF).copy(alpha = alpha.coerceAtLeast(0.40f))
        else -> Color(0xFF0F1117).copy(alpha = alpha)
    }

    val content = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurface
        isLight -> Color(0xFF0F172A)
        else -> Color(0xFFF8FAFC)
    }

    val subtitle = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isLight -> Color(0xFF475569)
        else -> Color(0xFF94A3B8)
    }

    val borderColor = when {
        !isEnabled -> Color.Transparent
        isLight -> Color(0xFF0F172A).copy(alpha = 0.12f)
        else -> Color(0xFFFFFFFF).copy(alpha = 0.14f)
    }

    val borderStroke = if (isEnabled) {
        BorderStroke(1.dp, borderColor)
    } else {
        null
    }

    val inputBg = when {
        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isLight -> Color(0x1F000000)
        else -> Color(0x2EFFFFFF)
    }

    return LiquidGlassStyle(
        backgroundColor = bg,
        contentColor = content,
        subtitleColor = subtitle,
        borderColor = borderColor,
        borderStroke = borderStroke,
        inputBackground = inputBg,
        isLight = isLight
    )
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    userSettings: UserSettings,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val style = rememberLiquidGlassStyle(userSettings)

    Box(
        modifier = modifier
            .clip(shape)
            .background(style.backgroundColor)
            .then(
                if (style.borderStroke != null) {
                    Modifier.border(style.borderStroke, shape)
                } else Modifier
            )
    ) {
        content()
    }
}

@Composable
fun LiquidGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    userSettings: UserSettings,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.5.sp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val style = rememberLiquidGlassStyle(userSettings)
    val isEnabled = userSettings.glassmorphismEnabled

    val pillShape = RoundedCornerShape(18.dp)
    val pillBg = if (isEnabled) {
        if (style.isLight) Color(0x28000000) else Color(0x35FFFFFF)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val pillBorder = if (isEnabled) {
        BorderStroke(1.dp, style.borderColor)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }

    val pillTextColor = if (isEnabled) style.contentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(pillShape)
            .background(pillBg)
            .border(pillBorder, pillShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Box(modifier = Modifier.padding(end = 4.dp))
            }
            Text(
                text = text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                color = pillTextColor
            )
        }
    }
}
