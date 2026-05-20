package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.core.helpers.ComposeHelper
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.toTimeString
import ca.ilianokokoro.umihi.music.ui.components.WavySliderExpressive
import ca.ilianokokoro.umihi.music.ui.screens.player.PlaybackProgress
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun PlayerControls(
    song: ca.ilianokokoro.umihi.music.models.Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: PlaybackProgress,
    onSeekPlayer: () -> Unit,
    onUpdateSeekBarHeldState: (isHeld: Boolean) -> Unit,
    onSeek: (location: Float) -> Unit,
    onOpenQueue: () -> Unit,
    onToggleLyrics: () -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    audioFormat: String? = null,
    audioSize: String? = null,
    audioBitrate: String? = null,
    modifier: Modifier = Modifier,
    progressAlpha: Float = 1f,
    progressOffsetY: Float = 0f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: Float = 0f,
    secondaryAlpha: Float = 1f,
    secondaryOffsetY: Float = 0f
) {
    val player by PlayerManager.controllerState.collectAsState()
    val repeatMode = ComposeHelper.rememberRepeatMode(player)
    val isShuffleEnabled = player?.shuffleModeEnabled ?: false

    // Color theme logic
    val isDark = ca.ilianokokoro.umihi.music.ui.theme.LocalPixelPlayDarkTheme.current
    val albumColorSchemePair = ca.ilianokokoro.umihi.music.ui.theme.LocalAlbumColorScheme.current
    val activeScheme = albumColorSchemePair?.let { if (isDark) it.dark else it.light }

    val colorPrevNext = when {
        activeScheme != null -> {
            if (isDark) {
                activeScheme.surfaceContainerHigh
            } else {
                activeScheme.primary
            }
        }
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val tintPrevNextIcon = when {
        activeScheme != null -> {
            if (isDark) {
                activeScheme.onSurface
            } else {
                activeScheme.onPrimary
            }
        }
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val colorPlayPause = when {
        activeScheme != null -> {
            activeScheme.primaryContainer
        }
        else -> MaterialTheme.colorScheme.primary
    }

    val tintPlayPauseIcon = when {
        activeScheme != null -> {
            activeScheme.onPrimaryContainer
        }
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val activeColorMain = activeScheme?.surfaceVariant ?: MaterialTheme.colorScheme.surfaceVariant
    val onActiveColorMain = activeScheme?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant

    val activeColorSecondary = activeScheme?.secondaryContainer ?: MaterialTheme.colorScheme.secondaryContainer
    val onActiveColorSecondary = activeScheme?.onSecondaryContainer ?: MaterialTheme.colorScheme.onSecondaryContainer

    val activeColorTertiary = activeScheme?.tertiaryContainer ?: MaterialTheme.colorScheme.tertiaryContainer
    val onActiveColorTertiary = activeScheme?.onTertiaryContainer ?: MaterialTheme.colorScheme.onTertiaryContainer

    val rowContainerColor = activeScheme?.surface ?: MaterialTheme.colorScheme.surface
    val inactiveContentColor = (activeScheme?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Metadata Title/Artist & Lyrics row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = secondaryAlpha
                    translationY = secondaryOffsetY
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = song?.title ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = song?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = GoogleSansRounded,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.basicMarquee()
                )
            }

            // Lyrics button
            androidx.compose.material3.IconButton(
                onClick = onToggleLyrics,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = "Lyrics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Progress bar container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progressAlpha
                    translationY = progressOffsetY
                }
        ) {
            // Expressive Wavy Slider
            WavySliderExpressive(
                value = progress.position,
                valueRange = 0f..progress.duration.coerceAtLeast(1f),
                isPlaying = isPlaying && !isLoading,
                onValueChange = { newValue ->
                    onUpdateSeekBarHeldState(true)
                    onSeek(newValue)
                },
                onValueChangeFinished = {
                    onSeekPlayer()
                    onUpdateSeekBarHeldState(false)
                },
                activeTrackColor = colorPrevNext,
                inactiveTrackColor = colorPrevNext.copy(alpha = 0.15f),
                thumbColor = colorPrevNext,
                modifier = Modifier.padding(top = 10.dp)
            )

            // Time labels + central format badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = progress.position.toTimeString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quality Badge (e.g. FLAC / Opus / M4A + Size)
                val format = audioFormat ?: "Opus"
                val size = audioSize ?: ""
                val badgeText = if (size.isNotEmpty()) "$format • $size" else format

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Text(
                    text = progress.duration.toTimeString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Playback Controls (Previous / Play-Pause / Next)
        AnimatedPlaybackControls(
            isPlayingProvider = { isPlaying },
            onPrevious = { PlayerManager.currentController?.seekToPrevious() },
            onPlayPause = {
                if (isPlaying) {
                    PlayerManager.currentController?.pause()
                } else {
                    PlayerManager.currentController?.play()
                }
            },
            onNext = { PlayerManager.currentController?.seekToNext() },
            height = 80.dp,
            playPauseCornerPlaying = 24.dp,
            playPauseCornerPaused = 24.dp,
            colorPreviousButton = colorPrevNext,
            colorNextButton = colorPrevNext,
            tintPreviousIcon = tintPrevNextIcon,
            tintNextIcon = tintPrevNextIcon,
            colorPlayPause = colorPlayPause,
            tintPlayPauseIcon = tintPlayPauseIcon,
            playPauseIconSize = 32.dp,
            iconSize = 26.dp,
            pressAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            modifier = Modifier
                .padding(vertical = 12.dp)
                .graphicsLayer {
                    alpha = controlsAlpha
                    translationY = controlsOffsetY
                }
        )

        // Centered Shuffle/Repeat/Favorite Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = secondaryAlpha
                    translationY = secondaryOffsetY
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomToggleRow(
                modifier = Modifier.fillMaxWidth(0.9f),
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                isFavoriteProvider = { isFavorite },
                activeColorMain = activeColorMain,
                onActiveColorMain = onActiveColorMain,
                activeColorSecondary = activeColorSecondary,
                onActiveColorSecondary = onActiveColorSecondary,
                activeColorTertiary = activeColorTertiary,
                onActiveColorTertiary = onActiveColorTertiary,
                containerColor = rowContainerColor,
                inactiveContentColor = inactiveContentColor,
                onShuffleToggle = {
                    PlayerManager.currentController?.let {
                        it.shuffleModeEnabled = !it.shuffleModeEnabled
                    }
                },
                onRepeatToggle = {
                    PlayerManager.currentController?.let {
                        it.repeatMode = when (it.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                },
                onFavoriteToggle = onFavoriteToggle
            )
        }
    }
}