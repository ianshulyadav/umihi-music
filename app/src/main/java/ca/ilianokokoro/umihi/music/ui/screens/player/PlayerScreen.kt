@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package ca.ilianokokoro.umihi.music.ui.screens.player

import android.app.Application
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import ca.ilianokokoro.umihi.music.ui.screens.player.components.PlayerControls
import ca.ilianokokoro.umihi.music.ui.screens.player.components.QueueBottomSheet
import ca.ilianokokoro.umihi.music.ui.screens.player.components.LyricsSheet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.automirrored.rounded.QueueMusic

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    application: Application,
    playerViewModel: PlayerViewModel = viewModel(
        factory =
            PlayerViewModel.Factory(application = application)
    )
) {
    val uiState = playerViewModel.uiState.collectAsStateWithLifecycle().value
    val orientation = LocalConfiguration.current.orientation
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)

    // Close the screen in resumed with an empty queue
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.queue.isEmpty() && currentSong == null) {
                onBack()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(
            modifier = modifier
                .padding(8.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {},
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount < -15) {
                                playerViewModel.setQueueVisibility(true)
                            } else if (dragAmount > 15) {
                                playerViewModel.setQueueVisibility(false)
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopPlayerHeader(
                onBack = onBack,
                onOpenQueue = { playerViewModel.setQueueVisibility(true) }
            )

            Thumbnail(
                href = currentSong?.thumbnailHref.toString(),
                isPlaying = uiState.isPlaying,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SongInfo(currentSong)
                PlayerControls(
                    isPlaying = uiState.isPlaying,
                    isLoading = uiState.isLoading,
                    progress = uiState.playbackProgress,
                    onSeek = playerViewModel::seek,
                    onSeekPlayer = playerViewModel::seekPlayer,
                    onUpdateSeekBarHeldState = playerViewModel::updateSeekBarHeldState,
                    onOpenQueue = {
                        playerViewModel.setQueueVisibility(true)
                    },
                    onToggleLyrics = {
                        playerViewModel.toggleLyricsVisibility(true)
                    },
                    isFavorite = uiState.isFavorite,
                    onFavoriteToggle = playerViewModel::toggleFavorite
                )
            }
        }

    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopPlayerHeader(
                onBack = onBack,
                onOpenQueue = { playerViewModel.setQueueVisibility(true) }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {},
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -15) {
                                    playerViewModel.setQueueVisibility(true)
                                } else if (dragAmount > 15) {
                                    playerViewModel.setQueueVisibility(false)
                                }
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Thumbnail(
                    href = currentSong?.thumbnailHref.toString(),
                    isPlaying = uiState.isPlaying,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    SongInfo(currentSong)
                    PlayerControls(
                        isPlaying = uiState.isPlaying,
                        isLoading = uiState.isLoading,
                        progress = uiState.playbackProgress,
                        onSeek = playerViewModel::seek,
                        onSeekPlayer = playerViewModel::seekPlayer,
                        onUpdateSeekBarHeldState = playerViewModel::updateSeekBarHeldState,
                        onOpenQueue = {
                            playerViewModel.setQueueVisibility(true)
                        },
                        onToggleLyrics = {
                            playerViewModel.toggleLyricsVisibility(true)
                        },
                        isFavorite = uiState.isFavorite,
                        onFavoriteToggle = playerViewModel::toggleFavorite
                    )
                }
            }
        }
    }


    // Queue
    if (uiState.isQueueModalShown) {
        QueueBottomSheet(
            changeVisibility = { playerViewModel.setQueueVisibility(it) },
            currentSong = uiState.queue.getOrNull(uiState.currentIndex),
            songs = uiState.queue,
            playerViewModel = playerViewModel
        )
    }

    // Lyrics
    if (uiState.showLyrics) {
        LyricsSheet(
            onClose = { playerViewModel.toggleLyricsVisibility(false) },
            playerViewModel = playerViewModel
        )
    }
}

@Composable
fun Thumbnail(
    href: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(maxWidth, maxHeight)

        androidx.compose.animation.AnimatedContent(
            targetState = href,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(Constants.Player.IMAGE_TRANSITION_DELAY)
                ).togetherWith(
                    fadeOut(
                        animationSpec = tween(Constants.Player.IMAGE_TRANSITION_DELAY)
                    )
                )
            }
        ) { targetState ->
            val cornerRadius by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isPlaying) 32.dp else 16.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "albumCornerRadius"
            )
            SquareImage(
                uri = targetState,
                cornerRadius = cornerRadius,
                modifier = Modifier.size(size)
            )
        }
    }
}


@Composable
fun SongInfo(song: Song?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = song?.title ?: "",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = song?.artist ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
fun TopPlayerHeader(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back / Dropdown Arrow button
        androidx.compose.material3.IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f), CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Go Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }

        // Title
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        // Actions Row (Bluetooth/Cast & Queue)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cast/Bluetooth (Connect device) Button
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f), CircleShape)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Cast,
                    contentDescription = "Connect Device",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Queue Button
            androidx.compose.material3.IconButton(
                onClick = onOpenQueue,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f), CircleShape)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Open Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}