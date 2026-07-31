package ru.ritual.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import kotlin.math.roundToInt
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.ui.theme.Ink

@Composable
fun ChecklistCard(
    checklist: Checklist,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasActions = onEdit != null || onDelete != null
    val revealWidth = 116.dp
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    var offsetX by remember(checklist.id) { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val dragState = rememberDraggableState { delta ->
        if (hasActions) offsetX = (offsetX + delta).coerceIn(-revealPx, 0f)
    }
    val settle: (Float) -> Unit = { velocity ->
        val target = when {
            velocity > 850f -> 0f
            velocity < -850f -> -revealPx
            offsetX < -revealPx * .34f -> -revealPx
            else -> 0f
        }
        scope.launch {
            Animatable(offsetX).animateTo(target, animationSpec = spring(stiffness = 650f)) {
                offsetX = value
            }
        }
    }

    Box(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(revealWidth),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                    offsetX = 0f
                    onEdit?.invoke()
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Edit, "Редактировать", tint = Ink)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFFFD9D7)).clickable {
                    offsetX = 0f
                    onDelete?.invoke()
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.DeleteOutline, "Удалить", tint = Color(0xFF9C2424))
            }
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .background(MaterialTheme.colorScheme.surface)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = hasActions,
                onDragStopped = { velocity -> settle(velocity) },
            )
            .clickable {
                if (offsetX < -1f) settle(0f) else onClick()
            }
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(88.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(checklist.accent),
        ) {
            Text(
                text = checklist.emoji,
                style = MaterialTheme.typography.headlineLarge,
                color = Ink,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                checklist.category.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = .78f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .align(Alignment.BottomStart),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    checklist.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (checklist.isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "В избранном",
                        tint = checklist.accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                checklist.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f),
                maxLines = if (checklist.tags.isEmpty()) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (checklist.tags.isNotEmpty()) {
                Text(
                    checklist.tags.take(3).joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "${checklist.durationMinutes} мин · ${checklist.steps.size} шагов",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .52f),
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Открыть", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
    }
}
