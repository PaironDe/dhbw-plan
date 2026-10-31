package de.soeren.dhbwplan.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.soeren.dhbwplan.R

object Neon {
    val Bg = Color(0xFF0B0614)
    val Surface = Color(0xFF170C2A)
    val Surface2 = Color(0xFF120A21)
    val Magenta = Color(0xFFFF2BD6)
    val Cyan = Color(0xFF00F0FF)
    val Yellow = Color(0xFFFCEE0A)
    val Border = Color(0xFF4A2F7A)
    val Text = Color(0xFFEDE6FF)
    val Muted = Color(0xFF9C8BC4)
    val Soft = Color(0xFFB9A9E0)
}

val Orbitron = FontFamily(
    Font(R.font.orbitron, FontWeight.Medium),
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.Black),
)

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

fun chamfer(size: Dp = 18.dp) = CutCornerShape(topEnd = size, bottomStart = size)

fun glow(color: Color) = TextStyle(shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = 24f))

@Composable
fun NeonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Magenta,
            secondary = Neon.Cyan,
            background = Neon.Bg,
            surface = Neon.Surface,
            onBackground = Neon.Text,
            onSurface = Neon.Text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Neon.Text, content = content)
    }
}
