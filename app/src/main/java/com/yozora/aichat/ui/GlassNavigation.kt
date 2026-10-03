package com.yozora.aichat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class DockIcon { Discover, Chats, Create, Settings }
internal data class DockDestination(val label: String, val icon: DockIcon)

@Composable
internal fun GlassNavigationDock(
    destinations: List<DockDestination>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    accent: Color,
    isLight: Boolean,
    createAnchor: Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val glass = if (isLight) Color.White else Color(0xFF1B2031)
    val reflection = if (isLight) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.20f)
    val edge = if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.25f)
    val restingTint = if (isLight) Color(0xFF656577) else Color(0xFFB4B3CB)
    val selectedTint = if (isLight) lerp(accent, Color(0xFF172333), 0.6f) else accent

    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(reflection, glass.copy(alpha = 0.86f), glass.copy(alpha = 0.72f))), shape)
                .border(1.dp, Brush.linearGradient(listOf(edge, edge.copy(alpha = 0.10f), edge.copy(alpha = 0.55f))), shape)
                .padding(6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                val strength by animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "dockSelection")
                val tint by animateColorAsState(if (selected) selectedTint else restingTint, tween(220), label = "dockTint")
                val itemShape = RoundedCornerShape(22.dp)
                Column(
                    modifier = Modifier.weight(1f).height(64.dp)
                        .then(if (destination.icon == DockIcon.Create) createAnchor else Modifier)
                        .background(Brush.verticalGradient(listOf(
                            accent.copy(alpha = (if (isLight) 0.14f else 0.22f) * strength),
                            accent.copy(alpha = 0.04f * strength)
                        )), itemShape)
                        .border(0.75.dp, Color.White.copy(alpha = (if (isLight) 0.8f else 0.16f) * strength), itemShape)
                        .selectable(selected = selected, role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            onClick = { onSelected(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DockGlyph(destination.icon, tint, selected)
                    Spacer(Modifier.height(5.dp))
                    Text(destination.label, color = tint, style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DockGlyph(icon: DockIcon, tint: Color, selected: Boolean) {
    Canvas(Modifier.size(26.dp)) {
        val stroke = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            when (icon) {
                DockIcon.Discover -> {
                    drawCircle(tint.copy(alpha = if (selected) 0.12f else 0.04f), 9.2f, Offset(11f, 12.5f))
                    drawArc(tint, 300f, 298f, false, Offset(1.8f, 3.3f), Size(18.4f, 18.4f), style = stroke)
                    val needle = Path().apply {
                        moveTo(14.8f, 8.7f); lineTo(12.5f, 14f); lineTo(7.2f, 16.3f)
                        lineTo(9.5f, 11f); close()
                    }
                    drawPath(needle, tint.copy(alpha = 0.18f))
                    drawPath(needle, tint, style = stroke)
                    drawLine(tint, Offset(19.3f, 1f), Offset(19.3f, 6f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(16.8f, 3.5f), Offset(21.8f, 3.5f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                }
                DockIcon.Chats -> {
                    val back = Path().apply {
                        moveTo(9f, 3f); lineTo(19f, 3f); quadraticTo(22f, 3f, 22f, 6f)
                        lineTo(22f, 12f); quadraticTo(22f, 15f, 19f, 15f)
                        lineTo(19f, 18f); lineTo(16f, 15f)
                    }
                    drawPath(back, tint.copy(alpha = 0.62f), style = stroke)
                    val front = Path().apply {
                        moveTo(5f, 7f); lineTo(13f, 7f); quadraticTo(16f, 7f, 16f, 10f)
                        lineTo(16f, 15f); quadraticTo(16f, 18f, 13f, 18f)
                        lineTo(8f, 18f); lineTo(4f, 21f); lineTo(4f, 18f)
                        quadraticTo(1f, 18f, 1f, 15f); lineTo(1f, 10f)
                        quadraticTo(1f, 7f, 5f, 7f); close()
                    }
                    drawPath(front, tint.copy(alpha = if (selected) 0.14f else 0.04f))
                    drawPath(front, tint, style = stroke)
                    listOf(5f, 8.5f, 12f).forEach { drawCircle(tint, 0.8f, Offset(it, 12.5f)) }
                }
                DockIcon.Create -> {
                    val nib = Path().apply {
                        moveTo(4f, 20f); lineTo(7f, 11f); lineTo(15f, 3f)
                        quadraticTo(17f, 1f, 19f, 3f); lineTo(21f, 5f)
                        quadraticTo(23f, 7f, 21f, 9f); lineTo(13f, 17f); close()
                    }
                    drawPath(nib, tint.copy(alpha = if (selected) 0.14f else 0.04f))
                    drawPath(nib, tint, style = stroke)
                    drawLine(tint, Offset(4f, 20f), Offset(14f, 10f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(13.5f, 4.5f), Offset(19.5f, 10.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(18.5f, 16f), Offset(18.5f, 22f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                    drawLine(tint, Offset(15.5f, 19f), Offset(21.5f, 19f), strokeWidth = 1.6f, cap = StrokeCap.Round)
                }
                DockIcon.Settings -> {
                    listOf(5f to 7f, 12f to 16f, 19f to 9f).forEach { (x, y) ->
                        drawLine(tint.copy(alpha = 0.68f), Offset(x, 2f), Offset(x, y - 3f), strokeWidth = 1.7f, cap = StrokeCap.Round)
                        drawLine(tint.copy(alpha = 0.68f), Offset(x, y + 3f), Offset(x, 22f), strokeWidth = 1.7f, cap = StrokeCap.Round)
                        drawRoundRect(tint.copy(alpha = if (selected) 0.18f else 0.06f), Offset(x - 2.8f, y - 2.8f), Size(5.6f, 5.6f), CornerRadius(1.8f))
                        drawRoundRect(tint, Offset(x - 2.8f, y - 2.8f), Size(5.6f, 5.6f), CornerRadius(1.8f), style = stroke)
                    }
                }
            }
        }
    }
}
