package ca.ilianokokoro.umihi.music.ui.components.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    currentSong: Song,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val hapticFeedback = LocalHapticFeedback.current
    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }

    // Dynamic Artwork Colors
    val albumColorSchemePair = ca.ilianokokoro.umihi.music.ui.theme.LocalAlbumColorScheme.current
    val isDark = ca.ilianokokoro.umihi.music.ui.theme.LocalPixelPlayDarkTheme.current
    val activeScheme = albumColorSchemePair?.let { if (isDark) it.dark else it.light }

    val containerColor = activeScheme?.primaryContainer?.copy(alpha = 0.95f) 
        ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    val onContainerColor = activeScheme?.onPrimaryContainer 
        ?: MaterialTheme.colorScheme.onPrimaryContainer
    val primaryColor = activeScheme?.primary 
        ?: MaterialTheme.colorScheme.primary
    val onPrimaryColor = activeScheme?.onPrimary 
        ?: MaterialTheme.colorScheme.onPrimary

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "player_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    boundsTransform = { _, _ ->
                        tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    },
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                )
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(36.dp))
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            shape = RoundedCornerShape(36.dp), // Modern stadium shape
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Album Art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                SmartImage(
                    model = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                    contentDescription = "Cover for ${currentSong.title}",
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        strokeWidth = 2.dp,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoading) "Loading audio…" else currentSong.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = onContainerColor,
                    maxLines = 1,
                    modifier = Modifier
                        .basicMarquee()
                )
                Text(
                    text = if (isLoading) "Preparing playback…" else currentSong.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    ),
                    color = onContainerColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Controls Row (Previous, Play/Pause, Next)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(onPrimaryColor)
                        .clickable(
                            interactionSource = previousInteraction,
                            indication = null,
                            enabled = !isLoading
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .clickable(
                            interactionSource = playPauseInteraction,
                            indication = null,
                            enabled = !isLoading
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isPlaying && !isLoading,
                        label = "playPause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = onPrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Next Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(onPrimaryColor)
                        .clickable(
                            interactionSource = nextInteraction,
                            indication = null,
                            enabled = !isLoading
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
}

