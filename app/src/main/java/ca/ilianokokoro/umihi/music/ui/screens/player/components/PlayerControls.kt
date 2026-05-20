package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun PlayerControls(
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
    modifier: Modifier = Modifier
) {
    val player by PlayerManager.controllerState.collectAsState()
    val repeatMode = ComposeHelper.rememberRepeatMode(player)
    val isShuffleEnabled = player?.shuffleModeEnabled ?: false

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lyrics Button above the progress seekbar on the very left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(
                onClick = onToggleLyrics,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = "Lyrics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expressive Wavy Slider from PixelPlayer
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
            modifier = Modifier.padding(top = 10.dp)
        )

        // Time labels
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
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
            Text(
                text = progress.duration.toTimeString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Animated Playback Controls from PixelPlayer (Slightly smaller, 70.dp height)
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
            height = 70.dp,
            playPauseIconSize = 26.dp,
            iconSize = 22.dp,
            pressAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            modifier = Modifier.padding(vertical = 10.dp)
        )

        // Centered Shuffle/Repeat/Favorite Toggle Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomToggleRow(
                modifier = Modifier.fillMaxWidth(0.85f),
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                isFavoriteProvider = { isFavorite },
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