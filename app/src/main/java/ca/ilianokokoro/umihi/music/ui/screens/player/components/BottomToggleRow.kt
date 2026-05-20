package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.ui.components.ToggleSegmentButton

@Composable
fun BottomToggleRow(
    modifier: Modifier,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavoriteProvider: () -> Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    activeColorMain: Color = MaterialTheme.colorScheme.primary,
    activeColorSecondary: Color = MaterialTheme.colorScheme.secondary,
    activeColorTertiary: Color = MaterialTheme.colorScheme.tertiary,
    onActiveColorMain: Color = MaterialTheme.colorScheme.onPrimary,
    onActiveColorSecondary: Color = MaterialTheme.colorScheme.onSecondary,
    onActiveColorTertiary: Color = MaterialTheme.colorScheme.onTertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    val isFavorite = isFavoriteProvider()
    val rowCorners = 24.dp
    val buttonCorners = 12.dp

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(rowCorners)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(rowCorners))
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commonModifier = Modifier.weight(1f)

            ToggleSegmentButton(
                modifier = commonModifier,
                active = isShuffleEnabled,
                activeColor = activeColorMain,
                activeCornerRadius = buttonCorners,
                activeContentColor = onActiveColorMain,
                inactiveColor = Color.Transparent,
                inactiveContentColor = inactiveContentColor,
                onClick = onShuffleToggle,
                imageVector = Icons.Rounded.Shuffle,
                contentDesc = "Shuffle"
            )
            val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                else -> Icons.Rounded.Repeat
            }
            ToggleSegmentButton(
                modifier = commonModifier,
                active = repeatActive,
                activeColor = activeColorSecondary,
                activeCornerRadius = buttonCorners,
                activeContentColor = onActiveColorSecondary,
                inactiveColor = Color.Transparent,
                inactiveContentColor = inactiveContentColor,
                onClick = onRepeatToggle,
                imageVector = repeatIcon,
                contentDesc = "Repeat"
            )
            ToggleSegmentButton(
                modifier = commonModifier,
                active = isFavorite,
                activeColor = activeColorTertiary,
                activeCornerRadius = buttonCorners,
                activeContentColor = onActiveColorTertiary,
                inactiveColor = Color.Transparent,
                inactiveContentColor = inactiveContentColor,
                onClick = onFavoriteToggle,
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDesc = "Favorite"
            )
        }
    }
}
