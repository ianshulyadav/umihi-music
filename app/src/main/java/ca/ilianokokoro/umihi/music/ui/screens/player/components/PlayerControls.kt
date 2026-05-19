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
    modifier: Modifier = Modifier
) {
    val player by PlayerManager.controllerState.collectAsState()
    val repeatMode = ComposeHelper.rememberRepeatMode(player)
    val isShuffleEnabled = player?.shuffleModeEnabled ?: false

    var isFavorite by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        // Toggle Buttons Row (Shuffle, Repeat, Favorite, Connect Device, Lyrics, Queue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle/Repeat/Favorite Toggle Bar
            BottomToggleRow(
                modifier = Modifier.weight(1f),
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
                onFavoriteToggle = { isFavorite = !isFavorite }
            )

            // Right-aligned actions row (Connect Device, Lyrics, Queue)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connect Device Button
                val context = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.material3.IconButton(
                    onClick = {
                        val intent = android.content.Intent().apply {
                            action = "com.android.settings.PLAYBACK_MEDIA_OUTPUT"
                            putExtra("package_name", context.packageName)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intentFallback = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intentFallback)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Cast,
                        contentDescription = "Connect Device",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Lyrics Button
                androidx.compose.material3.IconButton(
                    onClick = onToggleLyrics,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Queue Button (Styled slightly upper/taller as per request or aligned nicely)
                androidx.compose.material3.IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier
                        .size(44.dp)
                        .offset(y = (-6).dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Open Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}