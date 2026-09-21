package com.newbrowser.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * One tile in a Menu / Quick Tools icon grid: a circular icon over a caption.
 * [onClick] is last so call sites can use trailing-lambda syntax and skip [tint].
 */
data class GridAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

/** The 4-column icon-grid layout used by the Menu and Quick Tools bottom sheets. */
@Composable
fun ActionGrid(actions: List<GridAction>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(actions) { action -> ActionTile(action) }
    }
}

/**
 * Same look as [ActionGrid], but non-lazy - for a bounded action list that sits inside a
 * container that's already scrolling (like the new-tab page), where a lazy grid would crash
 * with "vertically scrollable component measured with infinity height constraints".
 */
@Composable
fun ActionFlowGrid(actions: List<GridAction>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        actions.forEach { action ->
            Box(modifier = Modifier.width(72.dp)) {
                ActionTile(action)
            }
        }
    }
}

@Composable
private fun ActionTile(action: GridAction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = action.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = action.tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Corner radius shared by the new Omni-style pill toolbar, bottom sheets, and grouped cards. */
val OmniCardShape = RoundedCornerShape(20.dp)
val OmniPillShape = RoundedCornerShape(28.dp)

/** A rounded card grouping related Settings rows, replacing flat dividers. */
@Composable
fun GroupedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = OmniCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

/** A single labeled row with a leading icon, used inside a [GroupedCard]. */
@Composable
fun CardRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

/**
 * A single rounded-card row for a list screen (Bookmarks, History, Downloads, ...), replacing
 * the old flat Row-plus-HorizontalDivider list style so these screens match the grouped-card
 * look used everywhere else (Settings, Safe Locker, the Menu/Quick Tools sheets).
 */
@Composable
fun ListRowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = OmniCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
