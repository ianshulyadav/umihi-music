package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.SquareImage
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerSheetState
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerState
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SheetCollapsedHeight = 72.dp
private val SheetCollapsedHorizontalPadding = 16.dp
private val SheetCollapsedCornerRadius = 28.dp
private val SheetExpandedCornerRadius = 0.dp

@Composable
fun UnifiedPlayerSheet(
    playerViewModel: PlayerViewModel,
    showMiniPlayer: Boolean,
    bottomBarVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)
    val isExpanded = uiState.sheetState == PlayerSheetState.EXPANDED
    val visible = showMiniPlayer || isExpanded

    if (!visible || currentSong == null) {
        return
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val bottomInset = 0.dp
    val bottomPadding = if (bottomBarVisible) 60.dp else 0.dp
    val collapsedHeightPx = with(density) { (SheetCollapsedHeight + bottomInset + 16.dp).toPx() }
    val collapsedOffsetY = with(density) { (screenHeightDp - collapsedHeightPx.toDp() - bottomPadding).toPx() }.coerceAtLeast(0f)

    val sheetOffset = remember { Animatable(collapsedOffsetY) }
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(collapsedOffsetY, isExpanded) {
        sheetOffset.animateTo(
            targetValue = if (isExpanded) 0f else collapsedOffsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    BackHandler(enabled = isExpanded) {
        playerViewModel.collapsePlayerSheet()
    }

    val translationY = sheetOffset.value.coerceIn(0f, collapsedOffsetY)
    val expansionFraction = if (collapsedOffsetY > 0f) {
        1f - (translationY / collapsedOffsetY).coerceIn(0f, 1f)
    } else {
        1f
    }
    val miniAlpha = (1f - expansionFraction * 2f).coerceIn(0f, 1f)
    val fullAlpha = ((expansionFraction - 0.5f) * 2f).coerceIn(0f, 1f)
    val sheetCornerRadius = lerp(SheetCollapsedCornerRadius, SheetExpandedCornerRadius, expansionFraction)
    val sheetHorizontalPadding = lerp(SheetCollapsedHorizontalPadding, 0.dp, expansionFraction)

    val player = PlayerManager.currentController
    val isPlaying = player?.isPlaying == true
    val isLoading = player?.playbackState == androidx.media3.common.Player.STATE_BUFFERING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(screenHeightDp)
            .offset { IntOffset(0, translationY.roundToInt()) }
            .padding(horizontal = sheetHorizontalPadding)
            .graphicsLayer {
                shadowElevation = 24f
                shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
                clip = true
            }
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(currentSong, collapsedOffsetY) {
                detectVerticalDragGestures(
                    onDragStart = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (sheetOffset.value + dragAmount).coerceIn(0f, collapsedOffsetY)
                        coroutineScope.launch {
                            sheetOffset.snapTo(newOffset)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            val shouldExpand = sheetOffset.value < collapsedOffsetY * 0.4f
                            sheetOffset.animateTo(
                                targetValue = if (shouldExpand) 0f else collapsedOffsetY,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            if (!shouldExpand) {
                                playerViewModel.collapsePlayerSheet()
                            } else {
                                playerViewModel.expandPlayerSheet()
                            }
                        }
                    }
                )
            }
    ) {
        Surface(
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = bottomPadding, top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f))
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    MiniPlayerHeader(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        alpha = miniAlpha,
                        onExpand = playerViewModel::expandPlayerSheet,
                        onPlayPause = {
                            if (isPlaying) player?.pause() else player?.play()
                        },
                        onSkipPrevious = { player?.seekToPrevious() },
                        onSkipNext = { player?.seekToNext() },
                        onDismissPlaylist = playerViewModel::dismissPlaylist
                    )

                    FullPlayerContent(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        progress = uiState.playbackProgress,
                        uiState = uiState,
                        alpha = fullAlpha,
                        onCollapse = playerViewModel::collapsePlayerSheet,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerHeader(
    currentSong: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    alpha: Float,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onDismissPlaylist: () -> Unit
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val offsetAnimatable = remember { Animatable(0f) }
    val dismissHandler = rememberMiniPlayerDismissGestureHandler(
        scope = rememberCoroutineScope(),
        density = density,
        hapticFeedback = LocalHapticFeedback.current,
        offsetAnimatable = offsetAnimatable,
        screenWidthPx = screenWidthPx,
        onDismissPlaylistAndShowUndo = onDismissPlaylist
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(SheetCollapsedHeight)
            .graphicsLayer {
                this.alpha = alpha
                this.translationX = offsetAnimatable.value
            }
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f))
            .miniPlayerDismissHorizontalGesture(true, dismissHandler)
            .clickable { onExpand() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                SquareImage(
                    uri = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1
                )
                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1
                )
            }

            IconButton(onClick = onSkipPrevious, enabled = !isLoading) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onPlayPause, enabled = !isLoading) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onSkipNext, enabled = !isLoading) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun FullPlayerContent(
    currentSong: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: ca.ilianokokoro.umihi.music.ui.screens.player.PlaybackProgress,
    uiState: PlayerState,
    alpha: Float,
    onCollapse: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Now playing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse player"
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SquareImage(
                uri = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                cornerRadius = 32.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PlayerControls(
            song = currentSong,
            isPlaying = isPlaying,
            isLoading = isLoading,
            progress = progress,
            onSeekPlayer = playerViewModel::seekPlayer,
            onUpdateSeekBarHeldState = playerViewModel::updateSeekBarHeldState,
            onSeek = playerViewModel::seek,
            onOpenQueue = { playerViewModel.setQueueVisibility(true) },
            onToggleLyrics = { playerViewModel.toggleLyricsVisibility(true) },
            isFavorite = uiState.isFavorite,
            onFavoriteToggle = playerViewModel::toggleFavorite,
            modifier = Modifier.fillMaxWidth(),
            progressAlpha = alpha,
            progressOffsetY = 0f,
            controlsAlpha = alpha,
            controlsOffsetY = 0f,
            secondaryAlpha = alpha,
            secondaryOffsetY = 0f
        )

        if (uiState.isQueueModalShown) {
            QueueBottomSheet(
                changeVisibility = { playerViewModel.setQueueVisibility(it) },
                currentSong = currentSong,
                songs = uiState.queue,
                playerViewModel = playerViewModel
            )
        }

        if (uiState.showLyrics) {
            LyricsSheet(
                onClose = { playerViewModel.toggleLyricsVisibility(false) },
                playerViewModel = playerViewModel
            )
        }
    }
}
