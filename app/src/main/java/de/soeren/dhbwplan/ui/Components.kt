package de.soeren.dhbwplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.soeren.dhbwplan.ui.theme.Mono
import de.soeren.dhbwplan.ui.theme.Neon
import de.soeren.dhbwplan.ui.theme.Orbitron
import de.soeren.dhbwplan.ui.theme.chamfer

/** Feine Scanlines über dem ganzen Bildschirm. */
fun Modifier.scanlines(): Modifier = this.drawWithContent {
    drawContent()
    val step = 3.dp.toPx()
    val line = Color.White.copy(alpha = 0.025f)
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

/** Karte mit abgeschrägten Ecken und leuchtendem 1-dp-Rand. */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    border: Color = Neon.Border,
    fill: Color = Neon.Surface2,
    cut: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = chamfer(cut)
    Column(
        modifier = modifier
            .clip(shape)
            .background(border)
            .padding(1.dp)
            .clip(shape)
            .background(fill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    accent: Color = Neon.Magenta,
) {
    val shape = chamfer(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (filled) accent else Neon.Border)
            .padding(if (filled) 0.dp else 1.dp)
            .clip(shape)
            .background(if (filled) accent else Neon.Bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = Orbitron,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = if (filled) Neon.Bg else Neon.Text,
            maxLines = 1,
        )
    }
}

@Composable
fun Tag(text: String, background: Color) {
    Text(
        text = text,
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = Neon.Bg,
        modifier = Modifier
            .background(background)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
fun NeonCheckbox(checked: Boolean) {
    val shape = chamfer(6.dp)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) Neon.Magenta else Neon.Border)
            .padding(if (checked) 0.dp else 2.dp)
            .clip(shape)
            .background(if (checked) Neon.Magenta else Neon.Bg),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", color = Neon.Bg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
