# Recorded Codes

Below are the code files provided for reference and recording.

## File 1: `fullplayercontent.kt`

> [!WARNING]
> This file was truncated during transmission (`<truncated 60811 bytes>`). Only the first 505 lines are recorded below. Please provide the remaining parts to complete the file.

```kotlin
package com.theveloper.pixelplay.presentation.components.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import com.theveloper.pixelplay.data.model.Lyrics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LoadingIndicator
// import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults // Removed
// import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // Removed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Artist
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.AlbumArtQuality
import com.theveloper.pixelplay.data.preferences.CarouselStyle
import com.theveloper.pixelplay.data.preferences.FullPlayerLoadingTweaks
import com.theveloper.pixelplay.presentation.components.AlbumCarouselSection
import com.theveloper.pixelplay.presentation.components.AutoScrollingTextOnDemand
import com.theveloper.pixelplay.presentation.components.LocalMaterialTheme
import com.theveloper.pixelplay.presentation.components.LyricsSheet
import com.theveloper.pixelplay.presentation.components.scoped.rememberSmoothProgress
import com.theveloper.pixelplay.presentation.components.subcomps.FetchLyricsDialog
import com.theveloper.pixelplay.presentation.viewmodel.LyricsSearchUiState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerSheetState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import com.theveloper.pixelplay.utils.AudioMetaUtils.mimeTypeToFormat
import com.theveloper.pixelplay.utils.LyricsImportFailureReason
import com.theveloper.pixelplay.utils.LyricsImportSecurity
import com.theveloper.pixelplay.utils.LyricsImportValidationResult
import com.theveloper.pixelplay.utils.ValidatedLyricsImport
import com.theveloper.pixelplay.utils.formatDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToLong
import com.theveloper.pixelplay.presentation.components.WavySliderExpressive
import com.theveloper.pixelplay.presentation.components.ToggleSegmentButton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

private const val PREVIOUS_TRACK_RESTART_THRESHOLD_MS = 10_000L
private const val SKIP_COMMAND_GUARD_MS = 96L

private enum class SkipDirection { PREVIOUS, NEXT }

private suspend fun validateLyricsImport(
    context: Context,
    uri: Uri
): LyricsImportValidationResult = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver

    var fileName = ""
    var fileSize: Long? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            fileName = if (nameIndex != -1) cursor.getString(nameIndex) else ""
            fileSize = if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                cursor.getLong(sizeIndex)
            } else {
                null
            }
        }
    }

    contentResolver.openInputStream(uri)?.use { inputStream ->
        LyricsImportSecurity.validateImportedLyricsFile(
            fileName = fileName,
            mimeType = contentResolver.getType(uri),
            inputStream = inputStream,
            reportedSizeBytes = fileSize
        )
    } ?: LyricsImportValidationResult.Invalid(LyricsImportFailureReason.EMPTY_CONTENT)
}

@androidx.annotation.OptIn(UnstableApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullPlayerContent(
    currentSong: Song?,
    currentPlaybackQueue: ImmutableList<Song>,
    currentQueueSourceName: String,
    currentMediaItemIndex: Int = -1,
    isShuffleEnabled: Boolean,
    shuffleTransitionInProgress: Boolean,
    repeatMode: Int,
    allowRealtimeUpdates: Boolean = true,
    expansionFractionProvider: () -> Float,
    currentSheetState: PlayerSheetState,
    carouselStyle: String,
    loadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean = false,
    playerViewModel: PlayerViewModel, // For stable state like totalDuration and lyrics
    // State Providers
    currentPositionProvider: () -> Long,
    isPlayingProvider: () -> Boolean,
    playWhenReadyProvider: () -> Boolean,
    isFavoriteProvider: () -> Boolean,
    repeatModeProvider: () -> Int,
    isShuffleEnabledProvider: () -> Boolean,
    totalDurationProvider: () -> Long,
    lyricsProvider: () -> Lyrics? = { null }, 
    // State
    isCastConnecting: Boolean = false,
    // Event Handlers
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit,
    onShowCastClicked: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var retainedSong by remember { mutableStateOf(currentSong) }
    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) {
            retainedSong = currentSong
        }
    }

    val song = currentSong ?: retainedSong ?: return // Keep the player visible while transitioning
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showArtistPicker by rememberSaveable { mutableStateOf(false) }
    
    val lyricsSearchUiState by playerViewModel.lyricsSearchUiState.collectAsStateWithLifecycle()

    // Single subscription — replaces 11 independent collectAsStateWithLifecycle calls.
    // distinctUntilChanged in the ViewModel ensures this only emits when something
    // actually changed, batching multiple rapid updates into one recomposition.
    val fullPlayerSlice by playerViewModel.fullPlayerSlice.collectAsStateWithLifecycle()
    val currentSongArtists = fullPlayerSlice.currentSongArtists
    val lyricsSyncOffset = fullPlayerSlice.lyricsSyncOffset
    val albumArtQuality = fullPlayerSlice.albumArtQuality
    val playbackAudioMetadata = fullPlayerSlice.audioMetadata
    val showPlayerFileInfo = fullPlayerSlice.showPlayerFileInfo
    val immersiveLyricsEnabled = fullPlayerSlice.immersiveLyricsEnabled
    val immersiveLyricsTimeout = fullPlayerSlice.immersiveLyricsTimeout
    val isImmersiveTemporarilyDisabled = fullPlayerSlice.isImmersiveTemporarilyDisabled
    val isRemotePlaybackActive = fullPlayerSlice.isRemotePlaybackActive
    val selectedRouteName = fullPlayerSlice.selectedRouteName
    val isBluetoothEnabled = fullPlayerSlice.isBluetoothEnabled
    val bluetoothName = fullPlayerSlice.bluetoothName
    val navigationBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val queueGestureBottomExclusion = maxOf(20.dp, navigationBarBottomInset + 8.dp)
    val queueGestureBottomExclusionPx = with(LocalDensity.current) {
        queueGestureBottomExclusion.toPx()
    }

    var showFetchLyricsDialog by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val fileImportScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                fileImportScope.launch {
                    try {
                        val validation = validateLyricsImport(context, it)
                        val validatedImport: ValidatedLyricsImport = when (validation) {
                            is LyricsImportValidationResult.Valid -> validation.value
                            is LyricsImportValidationResult.Invalid -> {
                                playerViewModel.sendToast(
                                    LyricsImportSecurity.messageFor(validation.reason)
                                )
                                return@launch
                            }
                        }

                        val currentSongId = currentSong?.id?.toLongOrNull()
                        if (currentSongId == null) {
                            playerViewModel.sendToast("No song selected for lyrics import.")
                            return@launch
                        }

                        playerViewModel.importLyricsFromFile(currentSongId, validatedImport)
                        showFetchLyricsDialog = false
                        showLyricsSheet = true
                    } catch (e: Exception) {
                        Timber.e(e, "Error reading imported lyrics file")
                        playerViewModel.sendToast("Error reading file.")
                    }
                }
            }
        }
    )

    // totalDurationValue is derived from stablePlayerState, so it's fine.
    // OPTIMIZATION: Use passed provider instead of collecting flow
    val totalDurationValue = totalDurationProvider()

    val playerOnBaseColor = LocalMaterialTheme.current.onPrimaryContainer
    val playerAccentColor = LocalMaterialTheme.current.primary
    val playerOnAccentColor = LocalMaterialTheme.current.onPrimary
    val transportPlayPauseColors = expressivePlayPauseButtonColors(LocalMaterialTheme.current)
    val transportSkipColors = expressiveSkipButtonColors(LocalMaterialTheme.current)
    val transportSkipButtonColors = TransportButtonColors(
        container = playerAccentColor,
        content = playerOnAccentColor
    )
    val progressActiveColor = playerOnBaseColor

    val placeholderColor = playerOnBaseColor.copy(alpha = 0.1f)
    val placeholderOnColor = playerOnBaseColor.copy(alpha = 0.2f)

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE


    // Lógica para el botón de Lyrics en el reproductor expandido
    val onLyricsClick = {
        val lyrics = lyricsProvider()
        if (lyrics?.synced.isNullOrEmpty() && lyrics?.plain.isNullOrEmpty()) {
            // Si no hay letra, mostramos el diálogo para buscar
            showFetchLyricsDialog = true
        } else {
            // Si hay letra, mostramos el sheet directamente
            showLyricsSheet = true
        }
    }

    if (showFetchLyricsDialog) {
        MaterialTheme(
            colorScheme = LocalMaterialTheme.current,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            FetchLyricsDialog(
                uiState = lyricsSearchUiState,
                currentSong = song, // Use 'song' which is derived from args/retained
                onConfirm = { forcePick ->
                    // El usuario confirma, iniciamos la búsqueda
                    playerViewModel.fetchLyricsForCurrentSong(forcePick)
                },
                onPickResult = { result ->
                    playerViewModel.acceptLyricsSearchResultForCurrentSong(result)
                },
                onManualSearch = { title, artist ->
                    playerViewModel.searchLyricsManually(title, artist)
                },
                onDismiss = {
                    // El usuario cancela o cierra el diálogo
                    showFetchLyricsDialog = false
                    playerViewModel.resetLyricsSearchState()
                },
                onImport = {
                    filePickerLauncher.launch(com.theveloper.pixelplay.utils.LyricsImportSecurity.pickerMimeTypes())
                }
            )
        }
    }

    // Observador para reaccionar al resultado de la búsqueda de letras
    LaunchedEffect(lyricsSearchUiState) {
        when (val state = lyricsSearchUiState) {
            is LyricsSearchUiState.Success -> {
                if (showFetchLyricsDialog) {
                    showFetchLyricsDialog = false
                    showLyricsSheet = true
                    playerViewModel.resetLyricsSearchState()
                }
            }
            is LyricsSearchUiState.Error -> {
            }
            else -> Unit
        }
    }

    val onAlbumSongSelected: (Song) -> Unit = { newSong ->
        playerViewModel.showAndPlaySong(
            song = newSong,
            contextSongs = currentPlaybackQueue,
            queueName = currentQueueSourceName
        )
    }

    val onSongMetadataQueueClick = {
        showSongInfoBottomSheet = true
        onShowQueueClicked()
    }

    val onSongMetadataArtistClick = {
        val resolvedArtistId = currentSongArtists.firstOrNull { it.id != 0L && it.id != -1L }?.id ?: song.artistId
        if (currentSongArtists.size > 1) {
            showArtistPicker = true
        } else {
            playerViewModel.triggerArtistNavigationFromPlayer(resolvedArtistId)
        }
    }

    var pendingCarouselIndex by remember { mutableStateOf<Int?>(null) }
    val currentQueueIndex = remember(song.id, currentMediaItemIndex, currentPlaybackQueue) {
        resolveQueueIndex(
            queue = currentPlaybackQueue,
            songId = song.id,
            currentMediaItemIndex = currentMediaItemIndex
        )
    }
    val skipRequests = remember {
        MutableSharedFlow<SkipDirection>(
            extraBufferCapacity = 16
        )
    }
    val latestQueue by rememberUpdatedState(currentPlaybackQueue)
    val latestSongId by rememberUpdatedState(song.id)
    val latestCurrentQueueIndex by rememberUpdatedState(currentQueueIndex)
    val latestRepeatMode by rememberUpdatedState(repeatMode)
    val latestIsRemotePlaybackActive by rememberUpdatedState(isRemotePlaybackActive)
    val latestCurrentPositionProvider by rememberUpdatedState(currentPositionProvider)
    val latestOnNext by rememberUpdatedState(onNext)
    val latestOnPrevious by rememberUpdatedState(onPrevious)

    LaunchedEffect(currentQueueIndex, pendingCarouselIndex) {
        if (pendingCarouselIndex == currentQueueIndex) {
            pendingCarouselIndex = null
        }
    }

    LaunchedEffect(pendingCarouselIndex, currentQueueIndex) {
        val targetIndex = pendingCarouselIndex ?: return@LaunchedEffect
        kotlinx.coroutines.delay(900)
        if (pendingCarouselIndex == targetIndex && currentQueueIndex != targetIndex) {
            pendingCarouselIndex = null
        }
    }

    LaunchedEffect(skipRequests) {
        skipRequests.collect { direction ->
            when (direction) {
                SkipDirection.NEXT -> latestOnNext()
                SkipDirection.PREVIOUS -> latestOnPrevious()
            }

            kotlinx.coroutines.delay(SKIP_COMMAND_GUARD_MS)
        }
    }

    fun predictSkipCarouselIndex(direction: SkipDirection): Int? {
        val queueSnapshot = latestQueue
        val baseIndex = pendingCarouselIndex
            ?: latestCurrentQueueIndex
            ?: queueSnapshot.indexOfFirst { it.id == latestSongId }.takeIf { it >= 0 }

        return when (direction) {
            SkipDirection.NEXT -> predictSkipNextCarouselIndex(
                currentIndex = baseIndex,
                queue = queueSnapshot,
                repeatMode = latestRepeatMode,
                isRemotePlaybackActive = latestIsRemotePlaybackActive
            )
            SkipDirection.PREVIOUS -> predictSkipPreviousCarouselIndex(
                currentIndex = baseIndex,
                queue = queueSnapshot,
                currentPositionMs = latestCurrentPositionProvider(),
                repeatMode = latestRepeatMode,
                isRemotePlaybackActive = latestIsRemotePlaybackActive
            )
        }
    }

    fun requestSkip(direction: SkipDirection) {
        val predictedTargetIndex = predictSkipCarouselIndex(direction)
        if (skipRequests.tryEmit(direction) && predictedTargetIndex != null) {
            pendingCarouselIndex = predictedTargetIndex
        }
    }

    val onNextWithOptimisticCarousel = {
        requestSkip(SkipDirection.NEXT)
        Unit
    }

    val onPreviousWithOptimisticCarousel = {
        requestSkip(SkipDirection.PREVIOUS)
        Unit
    }

    val albumCoverSection: @Composable (Modifier) -> Unit = { modifier ->
        FullPlayerAlbumCoverSection(
            song = song,
            currentPlaybackQueue = currentPlaybackQueue,
            currentMediaItemIndex = currentQueueIndex ?: currentMediaItemIndex,
            carouselStyle = carouselStyle,
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            isPlayingProvider = isPlayingProvider,
            playWhenReadyProvider = playWhenReadyProvider,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            albumArtQuality = albumArtQuality,
            requestedScrollIndex = pendingCarouselIndex,
            onSongSelected = onAlbumSongSelected,
            onAlbumClick = { albumSong ->
                playerViewModel.triggerAlbumNavigationFromPlayer(albumSong.albumId)
            },
            modifier = modifier
        )
    }

    val playerProgressSection: @Composable () -> Unit = {
        FullPlayerProgressSection(
            song = song,
            playbackMetadataMediaId = playbackAudioMetadata.mediaId,
            playbackMetadataMimeType = playbackAudioMetadata.mimeType,
            playbackMetadataBitrate = playbackAudioMetadata.bitrate,
            playbackMetadataSampleRate = playbackAudioMetadata.sampleRate,
            currentPositionProvider = currentPositionProvider,
            totalDurationValue = totalDurationValue,
            showPlayerFileInfo = showPlayerFileInfo,
            onSeek = onSeek,
            expansionFractionProvider = expansionFractionProvider,
            isPlayingProvider = isPlayingProvider,
            currentSheetState = currentSheetState,
            progressActiveColor = progressActiveColor,
            playerOnBaseColor = playerOnBaseColor,
            allowRealtimeUpdates = allowRealtimeUpdates,
            isSheetDragGestureActive = isSheetDragGestureActive,
            loadingTweaks = loadingTweaks
        )
    }

    val controlsSection: @Composable () -> Unit = {
        FullPlayerControlsSection(
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isPlayingProvider = isPlayingProvider,
            onPrevious = onPreviousWithOptimisticCarousel,
            onPlayPause = onPlayPause,
            onNext = onNextWithOptimisticCarousel,
            transportPlayPauseColors = transportPlayPauseColors,
            transportSkipColors = transportSkipButtonColors,
            isShuffleEnabledProvider = isShuffleEnabledProvider,
            shuffleTransitionInProgress = shuffleTransitionInProgress,
            repeatModeProvider = repeatModeProvider,
            isFavoriteProvider = isFavoriteProvider,
            onShuffleToggle = onShuffleToggle,
            onRepeatToggle = onRepeatToggle,
            onFavoriteToggle = onFavoriteToggle
        )
    }

    val portraitSongMetadataSection: @Composable () -> Unit = {
        FullPlayerSongMetadataSection(
            song = song,
            currentSongArtists = currentSongArtists,
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isLandscape = false,
            onLyricsClick = onLyricsClick,
            playerOnBaseColor = playerOnBaseColor,
            playerViewModel = playerViewModel,
            gradientEdgeColor = LocalMaterialTheme.current.primaryContainer,
            chipColor = playerOnAccentColor.copy(alpha = 0.8f),
            chipContentColor = playerAccentColor,
            onQueueClick = onSongMetadataQueueClick,
            onArtistClick = onSongMetadataArtistClick
        )
    }

    val landscapeSongMetadataSection: @Composable () -> Unit = {
        FullPlayerSongMetadataSection(
            song = song,
            currentSongArtists = currentSongArtists,
            loadingTweaks = loadingTweaks,
            isSheetDragGestureActive = isSheetDragGestureActive,
            expansionFractionProvider = expansionFractionProvider,
            currentSheetState = currentSheetState,
            placeholderColor = placeholderColor,
            placeholderOnColor = placeholderOnColor,
            isLandscape = true,
            onLyricsClick = onLyricsClick,
            playerOnBaseColor = playerOnBaseColor,
            playerViewModel = playerViewModel,
            gradientEdgeColor = LocalMaterialTheme.current.primaryContainer,
            chipColor = playerOnAccentColor.copy(alpha = 0.8f),
            chipContentColor = playerAccentColor,
            onQueueClick = onSongMetadataQueueClick,
            onArtistClick = onSongMetadataArtistClick
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.pointerInput(currentSheetState, queueGestureBottomExclusionPx) {
            val queueDragActivationThresholdPx = 4.dp.toPx()
            val quickFlickVelocityThreshold = -520f

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // Check condition AFTER the down event occurs
                val isFullyExpanded = currentSheetState == PlayerSheetState.EXPANDED && expansionFractionProvider() >= 0.99f

                if (!isFullyExpanded) {
                    return@awaitEachGesture
                }

                val bottomGestureBoundaryY =
                    (size.height.toFloat() - queueGestureBottomExclusionPx).coerceAtLeast(0f)
                if (down.position.y >= bottomGestureBoundaryY) {
                    // Let the system Home/back gesture win near the bottom edge.
                    return@awaitEachGesture
                }

                // Proceed with gesture logic
                var dragConsumedByQueue = false
                val velocityTracker = VelocityTracker()
                var totalDrag = 0f
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                drag(down.id) { change ->
                    val dragAmount = change.positionChange().y
                    totalDrag += dragAmount
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val isDraggingUp = totalDrag < -queueDragActivationThresholdPx

                    if (isDraggingUp && !dragConsumedByQueue) {
                        dragConsumedByQueue = true
                        onQueueDragStart()
                    }

                    if (dragConsumedByQueue) {
                        change.consume()
                        onQueueDrag(dragAmount)
                    }
                }

                val velocity = velocityTracker.calculateVelocity().y
                if (dragConsumedByQueue) {
                    onQueueRelease(totalDrag, velocity)
                } else if (
                    totalDrag < -(queueDragActivationThresholdPx * 2f) &&
                    velocity < quickFlickVelocityThreshold
                ) {
                    // Treat short/fast upward flick as queue-open intent.
                    onQueueRelease(totalDrag, velocity)
                }
            }
        },
        topBar = {
            // MD3: TopAppBar 在竖屏时滑入，横屏时向上滑出淡出
            AnimatedVisibility(
                visible = !isLandscape,
                enter = fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                       slideOutVertically(
                           targetOffsetY = { -it / 2 },
                           animationSpec = tween(220, easing = FastOutSlowInEasing)
                       )
            ) {
                TopAppBar(
                    modifier = Modifier.graphicsLayer {
                        val fraction = expansionFractionProvider()
                        // TopBar should always fade in smoothly, ignoring delayAll to avoid empty UI
                        val startThreshold = 0f
                        val endThreshold = 1f
                        alpha = ((fraction - startThreshold) / (endThreshold - startThreshold)).coerceIn(0f, 1f)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = LocalMaterialTheme.current.onPrimaryContainer,
                    ),
                    title = {
                        if (!isCastConnecting) {
                            AnimatedVisibility(visible = (!isRemotePlaybackActive)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        modifier = Modifier.padding(start = 18.dp),
                                        text = stringResource(R.string.setcat_now_playing),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (currentSong != null && (currentSong.telegramChatId != null || currentSong.contentUriString.startsWith("telegram:"))) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Rounded.Cloud,
                                            contentDescription = stringResource(R.string.presentation_batch_g_player_cd_cloud_stream),
                                            tint = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                // Ancho total = 14dp de padding + 42dp del botón
                                .width(56.dp)
                                .height(42.dp),
                            // 2. Alinea el contenido (el botón) al final (derecha) y centrado verticalmente
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            // 3. Tu botón circular original, sin cambios
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(playerOnAccentColor.copy(alpha = 0.7f))
                                    .clickable(onClick = onCollapse),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                                    contentDescription = stringResource(R.string.presentation_batch_g_player_cd_collapse),
                                    tint = playerAccentColor
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier
                                .padding(end = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showCastLabel = isCastConnecting || (isRemotePlaybackActive && selectedRouteName != null)
                            val isBluetoothActive =
                                isBluetoothEnabled && !bluetoothName.isNullOrEmpty() && !isRemotePlaybackActive && !isCastConnecting
                            val castIconPainter = when {
                                isCastConnecting || isRemotePlaybackActive -> painterResource(R.drawable.rounded_cast_24)
                                isBluetoothActive -> painterResource(R.drawable.rounded_bluetooth_24)
                                else -> painterResource(R.drawable.rounded_mobile_speaker_24)
                            }
                            val castCornersExpanded = 50.dp
                            val castCornersCompact = 6.dp
                            val castTopStart = castCornersExpanded
                            val castTopEnd by animateDpAsState(
                                targetValue = if (showCastLabel) castCornersExpanded else castCornersCompact,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                            val castBottomStart = castCornersExpanded
                            val castBottomEnd by animateDpAsState(
                                targetValue = if (showCastLabel) castCornersExpanded else castCornersCompact,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                            val castContainerColor = playerOnAccentColor.copy(alpha = 0.7f)
                            Box(
                                modifier = Modifier
                                    .height(42.dp)
                                    .align(Alignment.CenterVertically)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    .widthIn(
                                        min = 50.dp,
                                        max = if (showCastLabel) 190.dp else 58.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = castTopStart.coerceAtLeast(0.dp),
                                            topEnd = castTopEnd.coerceAtLeast(0.dp),
                                            bottomStart = castBottomStart.coerceAtLeast(0.dp),
                                            bottomEnd = castBottomEnd.coerceAtLeast(0.dp)
                                        )
                                    )
                                    .background(castContainerColor)
                                    .clickable { onShowCastClicked() },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(start = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        painter = castIconPainter,
                                        contentDescription = when {
                                            isCastConnecting || isRemotePlaybackActive -> stringResource(R.string.presentation_batch_g_player_cd_cast)
                                            isBluetoothActive -> stringResource(R.string.presentation_batch_g_player_cd_bluetooth)
                                            else -> stringResource(R.string.presentation_batch_g_player_cd_local_playback)
                                        },
                                        tint = playerAccentColor
                                    )
                                    AnimatedVisibility(visible = showCastLabel) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(Modifier.width(8.dp))
                                            AnimatedContent(
                                                targetState = when {
                                                    isCastConnecting -> stringResource(R.string.presentation_batch_g_player_connecting)
                                                    isRemotePlaybackActive && selectedRouteName != null -> selectedRouteName
                                                    else -> ""
                                                },
                                                transitionSpec = {
                                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(120))
                                                },
                                                label = "castButtonLabel"
                                            ) { label ->
                                                Row(
                                                    modifier = Modifier.padding(end = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = playerAccentColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    AnimatedVisibility(visible = isCastConnecting) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(14.dp),
                                                            strokeWidth = 2.dp,
                                                            color = playerAccentColor
                                                        )
                                                    }
                                                    if (isRemotePlaybackActive && !isCastConnecting) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(LocalMaterialTheme.current.onTertiaryContainer)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Queue Button
                            Box(
                                modifier = Modifier
                                    .size(height = 42.dp, width = 50.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 6.dp,
                                            topEnd = 50.dp,
                                            bottomStart = 6.dp,
                                            bottomEnd = 50.dp
                                        )
                                    )
                                    .background(playerOnAccentColor.copy(alpha = 0.7f))
                                    .clickable {
                                        showSongInfoBottomSheet = true
                                        onShowQueueClicked()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_queue_music_24),
                                    contentDescription = stringResource(R.string.presentation_batch_g_player_cd_queue),
                                    tint = playerAccentColor
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // MD3: 方向变化时先 alpha=0 再淡入新布局，避免双布局同时测量导致错位
        var contentVisible by remember(isLandscape) { mutableStateOf(false) }
        LaunchedEffect(isLandscape) { contentVisible = true }
        val contentAlpha by animateFloatAsState(
            targetValue = if (contentVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            label = "orientationAlpha"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
        ) {
            if (isLandscape) {
                FullPlayerLandscapeContent(
                    paddingValues = paddingValues,
                    albumCoverSection = albumCoverSection,
                    songMetadataSection = landscapeSongMetadataSection,
                    playerProgressSection = playerProgressSection,
                    controlsSection = controlsSection
                )
            } else {
                FullPlayerPortraitContent(
                    paddingValues = paddingValues,
                    albumCoverSection = albumCoverSection,
                    songMetadataSection = portraitSongMetadataSection,
                    playerProgressSection = playerProgressSection,
                    controlsSection = controlsSection
                )
            }
        }
    }
    AnimatedVisibility(
        visible = showLyricsSheet,
        enter = slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 160)),
        exit = slideOutVertically(
            targetOffsetY = { it / 6 },
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 120))
    ) {
        LyricsSheet(
            stablePlayerStateFlow = playerViewModel.stablePlayerState,
            playbackPositionFlow = playerViewModel.currentPlaybackPosition,
            lyricsSearchUiState = lyricsSearchUiState,
            resetLyricsForCurrentSong = {
                showLyricsSheet = false
                playerViewModel.resetLyricsForCurrentSong()
            },
            onSearchLyrics = { forcePick -> playerViewModel.fetchLyricsForCurrentSong(forcePick) },
            onPickResult = { playerViewModel.acceptLyricsSearchResultForCurrentSong(it) },
            onManualSearch = { title, artist -> playerViewModel.searchLyricsManually(title, artist) },
            onImportLyrics = { filePickerLauncher.launch(com.theveloper.pixelplay.utils.LyricsImportSecurity.pickerMimeTypes()) },
            onDismissLyricsSearch = { playerViewModel.resetLyricsSearchState() },
            lyricsSyncOffset = lyricsSyncOffset,
            onLyricsSyncOffsetChange = { currentSong?.id?.let { songId -> playerViewModel.setLyricsSyncOffset(songId, it) } },
            lyricsTextStyle = MaterialTheme.typography.titleLarge,
            colorScheme = LocalMaterialTheme.current,
            onBackClick = { showLyricsSheet = false },
            onSaveLyricsToFile = playerViewModel::saveLyricsToFile,
            onSeekTo = { playerViewModel.seekTo(it) },
            onPlayPause = {
                playerViewModel.playPause()
            },
            onNext = onNext,
            onPrev = onPrevious,
            immersiveLyricsEnabled = immersiveLyricsEnabled,
            immersiveLyricsTimeout = immersiveLyricsTimeout,
            isImmersiveTemporarilyDisabled = isImmersiveTemporarilyDisabled,
            onSetImmersiveTemporarilyDisabled = { playerViewModel.setImmersiveTemporarilyDisabled(it) },
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
            isFavoriteProvider = isFavoriteProvider,
            onShuffleToggle = onShuffleToggle,
            onRepeatToggle = onRepeatToggle,
            onFavoriteToggle = onFavoriteToggle
        )
    }

    val artistPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showArtistPicker && currentSongArtists.isNotEmpty()) {
        PlayerArtistPickerBottomSheet(
            song = song,
            artists = currentSongArtists,
            sheetState = artistPickerSheetState,
            onDismiss = { showArtistPicker = false },
            onArtistClick = { artist ->
                playerViewModel.triggerArtistNavigationFromPlayer(artist.id)
                showArtistPicker = false
            }
        )
    }
}
```

*(Note: The remaining parts of `fullplayercontent.kt` from the original code were not received due to size limits.)*

---

## File 2: `animatedplaybackcontrols.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.presentation.components.LocalMaterialTheme
import kotlinx.coroutines.delay
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private enum class PlaybackButtonType { NONE, PREVIOUS, PLAY_PAUSE, NEXT }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedPlaybackControls(
    isPlayingProvider: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp,
    baseWeight: Float = 1f,
    expansionWeight: Float = 1.1f,
    compressionWeight: Float = 0.65f,
    pressAnimationSpec: AnimationSpec<Float>,
    releaseDelay: Long = 220L,
    playPauseCornerPlaying: Dp = 60.dp,
    playPauseCornerPaused: Dp = 26.dp,
    colorOtherButtons: Color = LocalMaterialTheme.current.secondaryContainer,
    colorPlayPause: Color = LocalMaterialTheme.current.primary,
    tintPlayPauseIcon: Color = LocalMaterialTheme.current.onPrimary,
    tintOtherIcons: Color = LocalMaterialTheme.current.onSecondaryContainer,
    colorPreviousButton: Color = colorOtherButtons,
    colorNextButton: Color = colorOtherButtons,
    tintPreviousIcon: Color = tintOtherIcons,
    tintNextIcon: Color = tintOtherIcons,
    playPauseIconSize: Dp = 36.dp,
    iconSize: Dp = 32.dp,
) {
    val isPlaying = isPlayingProvider()
    var lastClicked by remember { mutableStateOf<PlaybackButtonType?>(null) }
    val latestIsPlayingProvider by rememberUpdatedState(newValue = isPlayingProvider)
    val latestLastClicked by rememberUpdatedState(newValue = lastClicked)
    val isPlayPauseLocked =
        lastClicked == PlaybackButtonType.NEXT || lastClicked == PlaybackButtonType.PREVIOUS
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(lastClicked) {
        if (lastClicked != null) {
            delay(releaseDelay)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }

        val shouldDelay = latestLastClicked != PlaybackButtonType.PLAY_PAUSE
        if (shouldDelay) {
            delay(releaseDelay)
        }
        if (!latestIsPlayingProvider()) {
            pendingPlayPauseState = false
        }
    }

    LaunchedEffect(isPlayPauseLocked, pendingPlayPauseState) {
        if (!isPlayPauseLocked) {
            pendingPlayPauseState?.let {
                playPauseVisualState = it
                pendingPlayPauseState = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun weightFor(button: PlaybackButtonType): Float = when (lastClicked) {
                button -> expansionWeight
                null -> baseWeight
                else -> compressionWeight
            }

            val prevWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.PREVIOUS),
                animationSpec = pressAnimationSpec,
                label = "prevWeight"
            )
            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colorPreviousButton)
                    .clickable {
                        lastClicked = PlaybackButtonType.PREVIOUS
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = tintPreviousIcon,
                    modifier = Modifier.size(iconSize)
                )
            }

            val playWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.PLAY_PAUSE),
                animationSpec = pressAnimationSpec,
                label = "playWeight"
            )
            // Tween (matching the Crossfade duration) instead of a spring with
            // StiffnessMedium. The old spring took ~600 ms to settle and read
            // playCorner in the composition phase, recomposing AnimatedPlaybackControls
            // every frame for the entire settle. A bounded 220 ms tween that completes
            // alongside the icon Crossfade keeps the recomposition window small enough
            // that it doesn't overlap with a subsequent sheet-collapse gesture.
            val playCorner by animateDpAsState(
                targetValue = if (!playPauseVisualState) playPauseCornerPlaying else playPauseCornerPaused,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "playCorner"
            )
            val playShape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = playCorner,
                smoothnessAsPercentTR = 60,
                cornerRadiusBL = playCorner,
                smoothnessAsPercentTL = 60,
                cornerRadiusTR = playCorner,
                smoothnessAsPercentBL = 60,
                cornerRadiusBR = playCorner,
                smoothnessAsPercentBR = 60
            )
            Box(
                modifier = Modifier
                    .weight(playWeight)
                    .fillMaxHeight()
                    .clip(playShape)
                    .background(colorPlayPause)
                    .clickable {
                        lastClicked = PlaybackButtonType.PLAY_PAUSE
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                MorphingPlayPauseIcon(
                    isPlaying = playPauseVisualState,
                    tint = tintPlayPauseIcon,
                    size = playPauseIconSize
                )
            }

            val nextWeight by animateFloatAsState(
                targetValue = weightFor(PlaybackButtonType.NEXT),
                animationSpec = pressAnimationSpec,
                label = "nextWeight"
            )
            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(colorNextButton)
                    .clickable {
                        lastClicked = PlaybackButtonType.NEXT
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Siguiente",
                    tint = tintNextIcon,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun MorphingPlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    size: Dp,
) {
    Crossfade(
        targetState = isPlaying,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "playPauseCrossfade"
    ) { playing ->
        Icon(
            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Pausar" else "Reproducir",
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}
```

---

## File 3: `bottomtogglerow.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.presentation.components.LocalMaterialTheme
import com.theveloper.pixelplay.presentation.components.ToggleSegmentButton
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

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
    val rowCorners = 60.dp

    Box(
        modifier = modifier.background(
            color = containerColor,
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusBL = rowCorners,
                smoothnessAsPercentTR = 60,
                cornerRadiusBR = rowCorners,
                smoothnessAsPercentBL = 60,
                cornerRadiusTL = rowCorners,
                smoothnessAsPercentBR = 60,
                cornerRadiusTR = rowCorners,
                smoothnessAsPercentTL = 60
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(
                    AbsoluteSmoothCornerShape(
                        cornerRadiusBL = rowCorners,
                        smoothnessAsPercentTR = 60,
                        cornerRadiusBR = rowCorners,
                        smoothnessAsPercentBL = 60,
                        cornerRadiusTL = rowCorners,
                        smoothnessAsPercentBR = 60,
                        cornerRadiusTR = rowCorners,
                        smoothnessAsPercentTL = 60
                    )
                )
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commonModifier = Modifier.weight(1f)

            ToggleSegmentButton(
                modifier = commonModifier,
                active = isShuffleEnabled,
                activeColor = activeColorMain,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorMain,
                inactiveColor = inactiveColor,
                inactiveContentColor = inactiveContentColor,
                onClick = onShuffleToggle,
                iconId = R.drawable.rounded_shuffle_24,
                contentDesc = "Shuffle"
            )
            val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIcon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.rounded_repeat_one_24
                Player.REPEAT_MODE_ALL -> R.drawable.rounded_repeat_24
                else -> R.drawable.rounded_repeat_24
            }
            ToggleSegmentButton(
                modifier = commonModifier,
                active = repeatActive,
                activeColor = activeColorSecondary,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorSecondary,
                inactiveColor = inactiveColor,
                inactiveContentColor = inactiveContentColor,
                onClick = onRepeatToggle,
                iconId = repeatIcon,
                contentDesc = "Repeat"
            )
            ToggleSegmentButton(
                modifier = commonModifier,
                active = isFavorite,
                activeColor = activeColorTertiary,
                activeCornerRadius = rowCorners,
                activeContentColor = onActiveColorTertiary,
                inactiveColor = inactiveColor,
                inactiveContentColor = inactiveContentColor,
                onClick = onFavoriteToggle,
                iconId = if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                contentDesc = "Favorite"
            )
        }
    }
}
```

---

## File 4: `PlayerArtistPlayer.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Artist
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.components.SmartImage
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private data class PlayerArtistShortcutItem(
    val artist: Artist,
    val isPrimary: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerArtistPickerBottomSheet(
    song: Song,
    artists: List<Artist>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shortcutItems = remember(song.id, song.artistId, song.artists, artists) {
        val primaryArtist = song.primaryArtist
        val computedItems = artists.mapIndexed { index, artist ->
            val isPrimary = when {
                primaryArtist.id != 0L && primaryArtist.id != -1L -> artist.id == primaryArtist.id
                primaryArtist.name.isNotBlank() -> artist.name.equals(primaryArtist.name, ignoreCase = true)
                else -> index == 0
            }
            PlayerArtistShortcutItem(
                artist = artist,
                isPrimary = isPrimary
            )
        }

        val hasPrimary = computedItems.any { it.isPrimary }
        val normalizedItems = if (hasPrimary || computedItems.isEmpty()) {
            computedItems
        } else {
            computedItems.mapIndexed { index, item ->
                item.copy(isPrimary = index == 0)
            }
        }
        normalizedItems.sortedByDescending { it.isPrimary }
    }

    val countLabel = when (shortcutItems.size) {
        1 -> stringResource(R.string.artist_picker_count_single)
        else -> stringResource(R.string.artist_picker_count_multiple, shortcutItems.size)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        },
        containerColor = colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.artist_picker_title),
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                shortcutItems.forEachIndexed { index, item ->
                    PlayerArtistShortcutCard(
                        artist = item.artist,
                        isPrimary = item.isPrimary,
                        shape = artistShortcutShape(
                            index = index,
                            count = shortcutItems.size
                        ),
                        onClick = { onArtistClick(item.artist) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PlayerArtistShortcutCard(
    artist: Artist,
    isPrimary: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isPrimary) {
        colorScheme.secondaryContainer
    } else {
        colorScheme.surfaceContainerLow
    }
    val contentColor = if (isPrimary) {
        colorScheme.onSecondaryContainer
    } else {
        colorScheme.onSurface
    }
    val labelContainerColor = if (isPrimary) {
        colorScheme.tertiary
    } else {
        colorScheme.surfaceContainerHighest
    }
    val labelContentColor = if (isPrimary) {
        colorScheme.onTertiary
    } else {
        colorScheme.onSurfaceVariant
    }
    val avatarBackground = if (isPrimary) {
        colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
    } else {
        colorScheme.surfaceContainerHighest
    }
    val trailingContainerColor = contentColor.copy(alpha = 0.12f)
    val avatarSize = 52.dp

    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(avatarBackground),
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    model = artist.effectiveImageUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    placeholderResId = R.drawable.rounded_artist_24,
                    errorResId = R.drawable.rounded_artist_24,
                    shape = CircleShape,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(180, 180),
                    placeHolderBackgroundColor = Color.Transparent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    color = labelContainerColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = stringResource(
                            if (isPrimary) {
                                R.string.artist_picker_primary_label
                            } else {
                                R.string.artist_picker_shortcut_label
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = labelContentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(trailingContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}

private fun artistShortcutShape(
    index: Int,
    count: Int
): RoundedCornerShape {
    val outerCorner = 26.dp
    val innerCorner = 10.dp
    return when {
        count <= 1 -> RoundedCornerShape(outerCorner)
        index == 0 -> RoundedCornerShape(
            topStart = outerCorner,
            topEnd = outerCorner,
            bottomStart = innerCorner,
            bottomEnd = innerCorner
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = innerCorner,
            topEnd = innerCorner,
            bottomStart = outerCorner,
            bottomEnd = outerCorner
        )
        else -> RoundedCornerShape(innerCorner)
    }
}
```

---

## File 5: `unifiedplayerminiplayer.kt`

```kotlin
@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.size.Size
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded

internal val LocalMaterialTheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

val MiniPlayerHeight = 64.dp
const val ANIMATION_DURATION_MS = 255
val MiniPlayerBottomSpacer = 8.dp

@Composable
fun getNavigationBarHeight(): Dp {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    return sanitizeNavigationBarBottomInset(insets.calculateBottomPadding())
}

@Composable
internal fun MiniPlayerContentInternal(
    song: Song,
    isPlaying: Boolean,
    isCastConnecting: Boolean,
    isPreparingPlayback: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    cornerRadiusAlb: Dp,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val controlsEnabled = !isCastConnecting && !isPreparingPlayback

    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val miniPlayerIndication = remember { ripple(bounded = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArtModel = song.albumArtUriString?.takeIf { it.isNotBlank() }
        Box(contentAlignment = Alignment.Center) {
            key(song.id) {
                SmartImage(
                    model = albumArtModel,
                    contentDescription = "Carátula de ${song.title}",
                    shape = CircleShape,
                    targetSize = Size(150, 150),
                    modifier = Modifier.size(44.dp),
                    placeholderModel = if (albumArtModel?.startsWith("telegram_art") == true) {
                        "$albumArtModel?quality=thumb"
                    } else null
                )
            }
            if (isCastConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = LocalMaterialTheme.current.onPrimaryContainer
                )
            } else if (isPreparingPlayback) {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            val titleStyle = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFamily = GoogleSansRounded,
                color = LocalMaterialTheme.current.onPrimaryContainer
            )
            val artistStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                fontFamily = GoogleSansRounded,
                color = LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.7f)
            )

            AutoScrollingText(
                text = when {
                    isCastConnecting -> "Connecting to device…"
                    isPreparingPlayback -> "Preparing playback…"
                    else -> song.title
                },
                style = titleStyle,
                gradientEdgeColor = LocalMaterialTheme.current.primaryContainer
            )
            AutoScrollingText(
                text = if (isPreparingPlayback) "Loading audio…" else song.displayArtist,
                style = artistStyle,
                gradientEdgeColor = LocalMaterialTheme.current.primaryContainer
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalMaterialTheme.current.onPrimary)
                .clickable(
                    interactionSource = previousInteraction,
                    indication = miniPlayerIndication,
                    enabled = controlsEnabled
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrevious()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Anterior",
                tint = LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalMaterialTheme.current.primary)
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = miniPlayerIndication,
                    enabled = controlsEnabled
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = LocalMaterialTheme.current.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LocalMaterialTheme.current.onPrimary)
                .clickable(
                    interactionSource = nextInteraction,
                    indication = miniPlayerIndication,
                    enabled = controlsEnabled
                ) { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Siguiente",
                tint = LocalMaterialTheme.current.primary,
                modifier = Modifier.size(22.dp)
        }
    }
}
```

---

## File 6: `unifiedplayerminiplayersheet.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.FullPlayerLoadingTweaks
import com.theveloper.pixelplay.presentation.components.player.FullPlayerContent
import com.theveloper.pixelplay.presentation.components.scoped.FullPlayerVisualState
import com.theveloper.pixelplay.presentation.components.scoped.rememberFullPlayerRuntimePolicy
import com.theveloper.pixelplay.presentation.viewmodel.PlayerSheetState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.viewmodel.StablePlayerState

@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.UnifiedPlayerMiniAndFullLayers(
    currentSong: Song?,
    miniPlayerScheme: ColorScheme?,
    overallSheetTopCornerRadiusProvider: () -> Dp,
    infrequentPlayerState: StablePlayerState,
    isCastConnecting: Boolean,
    isPreparingPlayback: Boolean,
    playerContentExpansionFraction: Animatable<Float, AnimationVector1D>,
    albumColorScheme: ColorScheme,
    bottomSheetOpenFraction: Float,
    fullPlayerVisualState: FullPlayerVisualState,
    containerHeight: Dp,
    currentQueueSourceName: String,
    currentSheetContentState: PlayerSheetState,
    carouselStyle: String,
    fullPlayerLoadingTweaks: FullPlayerLoadingTweaks,
    isSheetDragGestureActive: Boolean = false,
    playerViewModel: PlayerViewModel,
    currentPositionProvider: () -> Long,
    isFavorite: Boolean,
    shouldRenderFullPlayer: Boolean = true,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit,
    onShowCastClicked: () -> Unit
) {
    currentSong?.let { currentSongNonNull ->
        miniPlayerScheme?.let { readyScheme ->
            CompositionLocalProvider(
                LocalMaterialTheme provides readyScheme
            ) {
                val miniPlayerZIndex by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value < 0.5f) 1f else 0f
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            // Compute miniAlpha in the draw phase from the Animatable,
                            // avoiding per-frame recomposition during gestures.
                            alpha = (1f - playerContentExpansionFraction.value * 2f)
                                .coerceIn(0f, 1f)
                        }
                        .zIndex(miniPlayerZIndex)
                ) {
                    val miniAlbumCornerRadius by remember(overallSheetTopCornerRadiusProvider) {
                        derivedStateOf {
                            (overallSheetTopCornerRadiusProvider().value * 0.5f).dp
                        }
                    }
                    MiniPlayerContentInternal(
                        song = currentSongNonNull,
                        cornerRadiusAlb = miniAlbumCornerRadius,
                        isPlaying = infrequentPlayerState.isPlaying,
                        isCastConnecting = isCastConnecting,
                        isPreparingPlayback = isPreparingPlayback,
                        onPlayPause = { playerViewModel.playPause() },
                        onPrevious = { playerViewModel.previousSong() },
                        onNext = { playerViewModel.nextSong() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (shouldRenderFullPlayer) {
            CompositionLocalProvider(
                LocalMaterialTheme provides albumColorScheme
            ) {
                val fullPlayerScale by remember(bottomSheetOpenFraction) {
                    // Keep the depth effect, but avoid aggressive full-screen rescaling on every frame.
                    derivedStateOf { lerp(1f, 0.972f, bottomSheetOpenFraction) }
                }

                val fullPlayerZIndex by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value >= 0.5f) 1f else 0f
                    }
                }
                val fullPlayerOffset by remember {
                    derivedStateOf {
                        if (playerContentExpansionFraction.value <= 0.01f) IntOffset(0, 10000)
                        else IntOffset.Zero
                    }
                }
                val fullPlayerRuntimePolicy = rememberFullPlayerRuntimePolicy(
                    currentSheetState = currentSheetContentState,
                    expansionFraction = playerContentExpansionFraction,
                    bottomSheetOpenFraction = bottomSheetOpenFraction
                )

                // Scoped queue collection: only the FullPlayer subtree observes
                // the queue. Sibling MiniPlayer composable and the whole
                // UnifiedPlayerSheetV2 caller are insulated from queue churn.
                val currentPlaybackQueue by playerViewModel.queueFlow
                    .collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(containerHeight)
                        .graphicsLayer {
                            // Read from FullPlayerVisualState lazy getters in the draw phase;
                            // these read Animatable.value internally → re-draw only, no recomposition.
                            alpha = fullPlayerVisualState.contentAlpha
                            translationY = fullPlayerVisualState.translationY
                            scaleX = fullPlayerScale
                            scaleY = fullPlayerScale
                        }
                        .zIndex(fullPlayerZIndex)
                        .offset { fullPlayerOffset }
                ) {
                    val latestInfrequentPlayerState = rememberUpdatedState(infrequentPlayerState)
                    val latestIsFavorite = rememberUpdatedState(isFavorite)
                    val expansionFractionProvider = remember(playerContentExpansionFraction) {
                        { playerContentExpansionFraction.value }
                    }
                    val isPlayingProvider = remember {
                        { latestInfrequentPlayerState.value.isPlaying }
                    }
                    val playWhenReadyProvider = remember {
                        { latestInfrequentPlayerState.value.playWhenReady }
                    }
                    val repeatModeProvider = remember {
                        { latestInfrequentPlayerState.value.repeatMode }
                    }
                    val isShuffleEnabledProvider = remember {
                        { latestInfrequentPlayerState.value.isShuffleEnabled }
                    }
                    val totalDurationProvider = remember {
                        { latestInfrequentPlayerState.value.totalDuration }
                    }
                    val lyricsProvider = remember {
                        { latestInfrequentPlayerState.value.lyrics }
                    }
                    val isFavoriteProvider = remember {
                        { latestIsFavorite.value }
                    }
                    val onPlayPause = remember(playerViewModel) { playerViewModel::playPause }
                    val onSeek = remember(playerViewModel) { playerViewModel::seekTo }
                    val onNext = remember(playerViewModel) { playerViewModel::nextSong }
                    val onPrevious = remember(playerViewModel) { playerViewModel::previousSong }
                    val onCollapse = remember(playerViewModel) {
                        { playerViewModel.collapsePlayerSheet() }
                    }
                    val onShuffleToggle = remember(playerViewModel) {
                        { playerViewModel.toggleShuffle() }
                    }
                    val onRepeatToggle = remember(playerViewModel) { playerViewModel::cycleRepeatMode }
                    val onFavoriteToggle = remember(playerViewModel) { playerViewModel::toggleFavorite }

                    FullPlayerContent(
                        currentSong = currentSongNonNull,
                        currentPlaybackQueue = currentPlaybackQueue,
                        currentQueueSourceName = currentQueueSourceName,
                        currentMediaItemIndex = infrequentPlayerState.currentMediaItemIndex,
                        isShuffleEnabled = infrequentPlayerState.isShuffleEnabled,
                        shuffleTransitionInProgress = infrequentPlayerState.isShuffleTransitionInProgress,
                        repeatMode = infrequentPlayerState.repeatMode,
                        allowRealtimeUpdates = fullPlayerRuntimePolicy.allowRealtimeUpdates,
                        expansionFractionProvider = expansionFractionProvider,
                        currentSheetState = currentSheetContentState,
                        carouselStyle = carouselStyle,
                        loadingTweaks = fullPlayerLoadingTweaks,
                        isSheetDragGestureActive = isSheetDragGestureActive,
                        playerViewModel = playerViewModel,
                        currentPositionProvider = currentPositionProvider,
                        isPlayingProvider = isPlayingProvider,
                        playWhenReadyProvider = playWhenReadyProvider,
                        repeatModeProvider = repeatModeProvider,
                        isShuffleEnabledProvider = isShuffleEnabledProvider,
                        totalDurationProvider = totalDurationProvider,
                        lyricsProvider = lyricsProvider,
                        isCastConnecting = isCastConnecting,
                        isFavoriteProvider = isFavoriteProvider,
                        onPlayPause = onPlayPause,
                        onSeek = onSeek,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onCollapse = onCollapse,
                        onShowQueueClicked = onShowQueueClicked,
                        onQueueDragStart = onQueueDragStart,
                        onQueueDrag = onQueueDrag,
                        onQueueRelease = onQueueRelease,
                        onShowCastClicked = onShowCastClicked,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatToggle = onRepeatToggle,
                        onFavoriteToggle = onFavoriteToggle
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun UnifiedPlayerPrewarmLayer(
    prewarmFullPlayer: Boolean,
    currentSong: Song?,
    containerHeight: Dp,
    albumColorScheme: ColorScheme,
    currentQueueSourceName: String,
    infrequentPlayerState: StablePlayerState,
    carouselStyle: String,
    fullPlayerLoadingTweaks: FullPlayerLoadingTweaks,
    playerViewModel: PlayerViewModel,
    currentPositionProvider: () -> Long,
    isCastConnecting: Boolean,
    isFavorite: Boolean,
    onShowQueueClicked: () -> Unit,
    onQueueDragStart: () -> Unit,
    onQueueDrag: (Float) -> Unit,
    onQueueRelease: (Float, Float) -> Unit
) {
    if (prewarmFullPlayer && currentSong != null) {
        // Scoped queue collection: the prewarmed FullPlayer owns its own
        // subscription, keeping the queue out of the outer sheet's state.
        val currentPlaybackQueue by playerViewModel.queueFlow
            .collectAsStateWithLifecycle()
        CompositionLocalProvider(
            LocalMaterialTheme provides albumColorScheme
        ) {
            Box(
                modifier = Modifier
                    .height(containerHeight)
                    .fillMaxWidth()
                    .alpha(0f)
                    .clipToBounds()
            ) {
                // Memoize closures the same way the main layer does to avoid creating
                // new lambda instances on every recomposition.
                val latestInfrequentPlayerState = rememberUpdatedState(infrequentPlayerState)
                val latestIsFavorite = rememberUpdatedState(isFavorite)
                val isPlayingProvider = remember { { latestInfrequentPlayerState.value.isPlaying } }
                val playWhenReadyProvider = remember { { latestInfrequentPlayerState.value.playWhenReady } }
                val repeatModeProvider = remember { { latestInfrequentPlayerState.value.repeatMode } }
                val isShuffleEnabledProvider = remember { { latestInfrequentPlayerState.value.isShuffleEnabled } }
                val totalDurationProvider = remember { { latestInfrequentPlayerState.value.totalDuration } }
                val lyricsProvider = remember { { latestInfrequentPlayerState.value.lyrics } }
                val isFavoriteProvider = remember { { latestIsFavorite.value } }
                val onPlayPause = remember(playerViewModel) { playerViewModel::playPause }
                val onSeek = remember(playerViewModel) { playerViewModel::seekTo }
                val onNext = remember(playerViewModel) { playerViewModel::nextSong }
                val onPrevious = remember(playerViewModel) { playerViewModel::previousSong }
                val onShuffleToggle = remember(playerViewModel) { { playerViewModel.toggleShuffle() } }
                val onRepeatToggle = remember(playerViewModel) { playerViewModel::cycleRepeatMode }
                val onFavoriteToggle = remember(playerViewModel) { playerViewModel::toggleFavorite }

                FullPlayerContent(
                    currentSong = currentSong,
                    currentPlaybackQueue = currentPlaybackQueue,
                    currentQueueSourceName = currentQueueSourceName,
                    currentMediaItemIndex = infrequentPlayerState.currentMediaItemIndex,
                    isShuffleEnabled = infrequentPlayerState.isShuffleEnabled,
                    shuffleTransitionInProgress = infrequentPlayerState.isShuffleTransitionInProgress,
                    repeatMode = infrequentPlayerState.repeatMode,
                    allowRealtimeUpdates = false,
                    expansionFractionProvider = { 1f },
                    currentSheetState = PlayerSheetState.EXPANDED,
                    carouselStyle = carouselStyle,
                    loadingTweaks = fullPlayerLoadingTweaks,
                    playerViewModel = playerViewModel,
                    currentPositionProvider = currentPositionProvider,
                    isPlayingProvider = isPlayingProvider,
                    playWhenReadyProvider = playWhenReadyProvider,
                    repeatModeProvider = repeatModeProvider,
                    isShuffleEnabledProvider = isShuffleEnabledProvider,
                    totalDurationProvider = totalDurationProvider,
                    lyricsProvider = lyricsProvider,
                    isCastConnecting = isCastConnecting,
                    isFavoriteProvider = isFavoriteProvider,
                    onShowQueueClicked = onShowQueueClicked,
                    onQueueDragStart = onQueueDragStart,
                    onQueueDrag = onQueueDrag,
                    onQueueRelease = onQueueRelease,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onCollapse = {},
                    onShowCastClicked = {},
                    onShuffleToggle = onShuffleToggle,
                    onRepeatToggle = onRepeatToggle,
                    onFavoriteToggle = onFavoriteToggle
                )
            }
        }
    }
}
```

---

## File 7: `unifiedplayersheet2.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components

import android.widget.Toast
import com.theveloper.pixelplay.presentation.components.ExpressiveOfflineDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.sanitizeNavBarCornerRadius
import com.theveloper.pixelplay.presentation.components.scoped.PlayerAlbumNavigationEffect
import com.theveloper.pixelplay.presentation.components.scoped.PlayerArtistNavigationEffect
import com.theveloper.pixelplay.presentation.components.scoped.PlayerSheetPredictiveBackHandler
import com.theveloper.pixelplay.presentation.components.scoped.QueueSheetRuntimeEffects
import com.theveloper.pixelplay.presentation.components.scoped.SheetMotionController
import com.theveloper.pixelplay.presentation.components.scoped.miniPlayerDismissHorizontalGesture
import com.theveloper.pixelplay.presentation.components.scoped.playerSheetVerticalDragGesture
import com.theveloper.pixelplay.presentation.components.scoped.rememberFullPlayerCompositionPolicy
import com.theveloper.pixelplay.presentation.components.scoped.rememberCastSheetState
import com.theveloper.pixelplay.presentation.components.scoped.rememberFullPlayerVisualState
import com.theveloper.pixelplay.presentation.components.scoped.rememberMiniPlayerDismissGestureHandler
import com.theveloper.pixelplay.presentation.components.scoped.rememberPrewarmFullPlayer
import com.theveloper.pixelplay.presentation.components.scoped.rememberQueueSheetState
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetActionHandlers
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetBackAndDragState
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetInteractionState
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetModalOverlayController
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetOverlayState
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetThemeState
import com.theveloper.pixelplay.presentation.components.scoped.rememberSheetVisualState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerSheetState
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import com.theveloper.pixelplay.presentation.viewmodel.StablePlayerState
import com.theveloper.pixelplay.ui.theme.LocalPixelPlayDarkTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class PlayerUiSheetSliceV2(
    val currentQueueSourceName: String = "",
    val preparingSongId: String? = null
)

/**
 * V2 real host: no longer delegates to the legacy `UnifiedPlayerSheet`.
 *
 * This path keeps behavior parity, but now owns its own runtime wiring so we can
 * profile and optimize V2 independently while preserving the Experimental switch.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun UnifiedPlayerSheetV2(
    playerViewModel: PlayerViewModel,
    sheetCollapsedTargetY: Float,
    containerHeight: Dp,
    collapsedStateHorizontalPadding: Dp = 12.dp,
    navController: NavHostController,
    hideMiniPlayer: Boolean = false,
    isNavBarHidden: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestContext by rememberUpdatedState(context)
    var showNoInternetDialog by remember { mutableStateOf(false) }

    // MediaStore write-permission launcher (for metadata editing without MANAGE_EXTERNAL_STORAGE)
    val writePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onWritePermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    // MediaStore delete-permission launcher (system delete confirmation dialog)
    val deletePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        playerViewModel.onDeletePermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(playerViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                playerViewModel.toastEvents.collect { message ->
                    Toast.makeText(latestContext, message, Toast.LENGTH_SHORT).show()
                }
            }
            launch {
                playerViewModel.showNoInternetDialog.collect {
                    showNoInternetDialog = true
                }
            }
            launch {
                playerViewModel.writePermissionRequest.collect { intentSender ->
                    writePermissionLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
            launch {
                playerViewModel.deletePermissionRequest.collect { intentSender ->
                    deletePermissionLauncher.launch(
                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                    )
                }
            }
        }
    }

    if (showNoInternetDialog) {
        ExpressiveOfflineDialog(
            onDismiss = { showNoInternetDialog = false },
            onRetry = {
                 playerViewModel.refreshLocalConnectionInfo()
                 showNoInternetDialog = false
            }
        )
    }

    val infrequentPlayerStateReference = playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val infrequentPlayerState = infrequentPlayerStateReference.value
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    val currentPositionState = playerViewModel.currentPlaybackPosition.collectAsStateWithLifecycle()
    val remotePositionState = playerViewModel.remotePosition.collectAsStateWithLifecycle()
    val isRemotePlaybackActive by playerViewModel.isRemotePlaybackActive.collectAsStateWithLifecycle()
    val positionToDisplayProvider = remember(isRemotePlaybackActive) {
        {
            if (isRemotePlaybackActive) remotePositionState.value
            else currentPositionState.value
        }
    }

    val isFavorite by playerViewModel.isCurrentSongFavorite.collectAsStateWithLifecycle()

    val playerUiSheetSlice by remember {
        playerViewModel.playerUiState
            .map { state ->
                PlayerUiSheetSliceV2(
                    currentQueueSourceName = state.currentQueueSourceName,
                    preparingSongId = state.preparingSongId
                )
            }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = PlayerUiSheetSliceV2())
    val currentQueueSourceName = playerUiSheetSlice.currentQueueSourceName
    val preparingSongId = playerUiSheetSlice.preparingSongId

    val currentSheetContentState by playerViewModel.sheetState.collectAsStateWithLifecycle()
    val predictiveBackCollapseProgress by playerViewModel.predictiveBackCollapseFraction.collectAsStateWithLifecycle()
    val predictiveBackSwipeEdge by playerViewModel.predictiveBackSwipeEdge.collectAsStateWithLifecycle()
    val prewarmFullPlayer = rememberPrewarmFullPlayer(infrequentPlayerState.currentSong?.id)

    val playerConfig by playerViewModel.playerConfigSlice.collectAsStateWithLifecycle()
    val navBarCornerRadius = sanitizeNavBarCornerRadius(playerConfig.navBarCornerRadius)
    val navBarStyle = playerConfig.navBarStyle
    val carouselStyle = playerConfig.carouselStyle
    val fullPlayerLoadingTweaks = playerConfig.fullPlayerLoadingTweaks
    val tapBackgroundClosesPlayer = playerConfig.tapBackgroundClosesPlayer
    val useSmoothCorners = playerConfig.useSmoothCorners
    val playerThemePreference = playerConfig.playerThemePreference

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val offsetAnimatable = remember { Animatable(0f) }
    val screenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val dismissThresholdPx = remember(screenWidthPx) { screenWidthPx * 0.4f }
    val swipeDismissProgress by remember(dismissThresholdPx) {
        derivedStateOf {
            if (dismissThresholdPx == 0f) 0f
            else (abs(offsetAnimatable.value) / dismissThresholdPx).coerceIn(0f, 1f)
        }
    }

    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val miniPlayerContentHeightPx = remember { with(density) { MiniPlayerHeight.toPx() } }

    val isCastConnecting by playerViewModel.isCastConnecting.collectAsStateWithLifecycle()
    val showPlayerContentArea by remember(infrequentPlayerState.currentSong, isCastConnecting) {
        derivedStateOf { infrequentPlayerState.currentSong != null || isCastConnecting }
    }

    val playerContentExpansionFraction = playerViewModel.playerContentExpansionFraction
    val visualOvershootScaleY = remember { Animatable(1f) }
    val initialFullPlayerOffsetY = remember(density) { with(density) { 24.dp.toPx() } }
    val sheetAnimationSpec = remember {
        tween<Float>(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
    }
    val sheetAnimationMutex = remember { MutatorMutex() }
    val sheetExpandedTargetY = 0f
    val initialY =
        if (currentSheetContentState == PlayerSheetState.COLLAPSED) sheetCollapsedTargetY
        else sheetExpandedTargetY
    val currentSheetTranslationY = remember { Animatable(initialY) }
    val sheetMotionController = remember(
        currentSheetTranslationY,
        playerContentExpansionFraction,
        sheetAnimationMutex,
        sheetAnimationSpec
    ) {
        SheetMotionController(
            translationY = currentSheetTranslationY,
            expansionFraction = playerContentExpansionFraction,
            mutex = sheetAnimationMutex,
            defaultAnimationSpec = sheetAnimationSpec,
            expandedY = sheetExpandedTargetY
        )
    }

    PlayerArtistNavigationEffect(
        navController = navController,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetMotionController = sheetMotionController,
        playerViewModel = playerViewModel
    )
    PlayerAlbumNavigationEffect(
        navController = navController,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetMotionController = sheetMotionController,
        playerViewModel = playerViewModel
    )

    // FullPlayerVisualState now holds lazy getters that read from the Animatable
    // inside graphicsLayer (draw-phase), avoiding per-frame recomposition.
    val fullPlayerVisualState = rememberFullPlayerVisualState(
        expansionFraction = playerContentExpansionFraction,
        initialOffsetY = initialFullPlayerOffsetY
    )
    val fullPlayerCompositionPolicy = rememberFullPlayerCompositionPolicy(
        currentSongId = infrequentPlayerState.currentSong?.id,
        currentSheetState = currentSheetContentState,
        expansionFraction = playerContentExpansionFraction
    )
    val shouldRenderFullPlayer = fullPlayerCompositionPolicy.shouldRenderFullPlayer

    suspend fun animatePlayerSheet(
        targetExpanded: Boolean,
        animationSpec: androidx.compose.animation.core.AnimationSpec<Float> = sheetAnimationSpec,
        initialVelocity: Float = 0f
    ) {
        sheetMotionController.animateTo(
            targetExpanded = targetExpanded,
            canExpand = showPlayerContentArea,
            collapsedY = sheetCollapsedTargetY,
            animationSpec = animationSpec,
            initialVelocity = initialVelocity
        )
    }

    LaunchedEffect(sheetCollapsedTargetY, sheetMotionController) {
        // Keep the mini player anchored to the latest collapsed target whenever
        // the navbar height/visibility changes under it.
        sheetMotionController.syncToExpansion(sheetCollapsedTargetY)
    }

    var previousSheetState by remember { mutableStateOf(currentSheetContentState) }
    LaunchedEffect(showPlayerContentArea, currentSheetContentState) {
        val targetExpanded = showPlayerContentArea && currentSheetContentState == PlayerSheetState.EXPANDED
        val shouldBounceCollapse =
            showPlayerContentArea &&
                previousSheetState == PlayerSheetState.EXPANDED &&
                currentSheetContentState == PlayerSheetState.COLLAPSED

        previousSheetState = currentSheetContentState
        animatePlayerSheet(targetExpanded = targetExpanded)

        if (showPlayerContentArea) {
            scope.launch {
                visualOvershootScaleY.snapTo(1f)
                if (targetExpanded) {
                    visualOvershootScaleY.animateTo(
                        targetValue = 1f,
                        animationSpec = keyframes {
                            durationMillis = 50
                            1.0f at 0
                            1.05f at 125
                            1.0f at 250
                        }
                    )
                } else if (shouldBounceCollapse) {
                    visualOvershootScaleY.snapTo(0.96f)
                    visualOvershootScaleY.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                } else {
                    visualOvershootScaleY.snapTo(1f)
                }
            }
        } else {
            scope.launch { visualOvershootScaleY.snapTo(1f) }
        }
    }

    val sheetVisualState = rememberSheetVisualState(
        showPlayerContentArea = showPlayerContentArea,
        collapsedStateHorizontalPadding = collapsedStateHorizontalPadding,
        predictiveBackCollapseProgress = predictiveBackCollapseProgress,
        predictiveBackSwipeEdge = predictiveBackSwipeEdge,
        currentSheetContentState = currentSheetContentState,
        playerContentExpansionFraction = playerContentExpansionFraction,
        containerHeight = containerHeight,
        currentSheetTranslationY = currentSheetTranslationY,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        navBarStyle = navBarStyle,
        navBarCornerRadiusDp = navBarCornerRadius.dp,
        isNavBarHidden = isNavBarHidden,
        isPlaying = infrequentPlayerState.isPlaying,
        hasCurrentSong = infrequentPlayerState.currentSong != null,
        swipeDismissProgress = swipeDismissProgress
    )
    val currentBottomPadding = sheetVisualState.currentBottomPadding
    val playerContentAreaHeightPxProvider = sheetVisualState.playerContentAreaHeightPxProvider
    val visualSheetTranslationYProvider = sheetVisualState.visualSheetTranslationYProvider
    val overallSheetTopCornerRadiusProvider = sheetVisualState.overallSheetTopCornerRadiusProvider
    val playerContentActualBottomRadiusProvider = sheetVisualState.playerContentActualBottomRadiusProvider
    val currentHorizontalPaddingStartPxProvider = sheetVisualState.currentHorizontalPaddingStartPxProvider
    val currentHorizontalPaddingEndPxProvider = sheetVisualState.currentHorizontalPaddingEndPxProvider

    val queueSheetState = rememberQueueSheetState(
        scope = scope,
        screenHeightPx = screenHeightPx,
        density = density,
        currentBottomPadding = currentBottomPadding,
        showPlayerContentArea = showPlayerContentArea,
        currentSheetContentState = currentSheetContentState
    )
    val showQueueSheet = queueSheetState.showQueueSheet
    val allowQueueSheetInteraction = queueSheetState.allowQueueSheetInteraction
    val queueSheetOffset = queueSheetState.queueSheetOffset
    val queueSheetHeightPx = queueSheetState.queueSheetHeightPx
    val queueHiddenOffsetPx = queueSheetState.queueHiddenOffsetPx
    val queueSheetController = queueSheetState.queueSheetController
    val onQueueSheetHeightPxChange = queueSheetState.onQueueSheetHeightPxChange

    val castSheetState = rememberCastSheetState()
    val sheetBackAndDragState = rememberSheetBackAndDragState(
        showPlayerContentArea = showPlayerContentArea,
        currentSheetContentState = currentSheetContentState
    )
    val canHandlePlayerBack by remember(
        sheetBackAndDragState.predictiveBackEnabled,
        showQueueSheet,
        castSheetState.showCastSheet
    ) {
        derivedStateOf {
            sheetBackAndDragState.predictiveBackEnabled &&
                !showQueueSheet &&
                !castSheetState.showCastSheet
        }
    }
    val velocityTracker = remember { VelocityTracker() }
    val sheetModalOverlayController = rememberSheetModalOverlayController(
        scope = scope,
        queueSheetController = queueSheetController,
        animationDurationMs = ANIMATION_DURATION_MS,
        onCollapsePlayerSheet = { playerViewModel.collapsePlayerSheet() }
    )
    val pendingSaveQueueOverlay = sheetModalOverlayController.pendingSaveQueueOverlay
    val selectedSongForInfo = sheetModalOverlayController.selectedSongForInfo
    val sheetActionHandlers = rememberSheetActionHandlers(
        scope = scope,
        navController = navController,
        playerViewModel = playerViewModel,
        sheetMotionController = sheetMotionController,
        queueSheetController = queueSheetController,
        sheetModalOverlayController = sheetModalOverlayController,
        sheetCollapsedTargetY = sheetCollapsedTargetY
    )

    val hapticFeedback = LocalHapticFeedback.current
    val miniDismissGestureHandler = rememberMiniPlayerDismissGestureHandler(
        scope = scope,
        density = density,
        hapticFeedback = hapticFeedback,
        offsetAnimatable = offsetAnimatable,
        screenWidthPx = screenWidthPx,
        onDismissPlaylistAndShowUndo = { playerViewModel.dismissPlaylistAndShowUndo() }
    )

    QueueSheetRuntimeEffects(
        queueSheetController = queueSheetController,
        queueSheetOffset = queueSheetOffset,
        queueHiddenOffsetPx = queueHiddenOffsetPx,
        showQueueSheet = showQueueSheet,
        allowQueueSheetInteraction = allowQueueSheetInteraction,
        onTopEdgeReached = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    PlayerSheetPredictiveBackHandler(
        enabled = canHandlePlayerBack,
        playerViewModel = playerViewModel,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetExpandedTargetY = sheetExpandedTargetY,
        sheetMotionController = sheetMotionController,
        animationDurationMs = ANIMATION_DURATION_MS,
        onSwipeEdgeChanged = { playerViewModel.updatePredictiveBackSwipeEdge(it) },
        registrationKey = currentBackStackEntry?.id
    )

    val sheetOverlayState = rememberSheetOverlayState(
        density = density,
        showPlayerContentArea = showPlayerContentArea,
        hideMiniPlayer = hideMiniPlayer,
        showQueueSheet = showQueueSheet,
        queueHiddenOffsetPx = queueHiddenOffsetPx,
        screenHeightPx = screenHeightPx,
        castSheetOpenFraction = castSheetState.castSheetOpenFraction
    )
    val internalIsKeyboardVisible = sheetOverlayState.internalIsKeyboardVisible
    val actuallyShowSheetContent = sheetOverlayState.actuallyShowSheetContent
    val isQueueVisible = sheetOverlayState.isQueueVisible
    val bottomSheetOpenFraction = sheetOverlayState.bottomSheetOpenFraction
    val queueScrimAlpha = sheetOverlayState.queueScrimAlpha
    val shouldRenderQueueHost by remember(internalIsKeyboardVisible, selectedSongForInfo) {
        derivedStateOf {
            !internalIsKeyboardVisible || selectedSongForInfo != null
        }
    }
    val isQueueTelemetryActive = showQueueSheet

    LaunchedEffect(showQueueSheet) {
        playerViewModel.updateQueueSheetVisibility(showQueueSheet)
    }
    LaunchedEffect(castSheetState.showCastSheet) {
        playerViewModel.updateCastSheetVisibility(castSheetState.showCastSheet)
    }
    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.updateQueueSheetVisibility(false)
            playerViewModel.updateCastSheetVisibility(false)
        }
    }

    val activePlayerSchemePair by playerViewModel.activePlayerColorSchemePair.collectAsStateWithLifecycle()
    val themedAlbumArtUri by playerViewModel.currentThemedAlbumArtUri.collectAsStateWithLifecycle()
    val isDarkTheme = LocalPixelPlayDarkTheme.current
    val currentSong = infrequentPlayerState.currentSong
    val sheetThemeState = rememberSheetThemeState(
        activePlayerSchemePair = activePlayerSchemePair,
        isDarkTheme = isDarkTheme,
        playerThemePreference = playerThemePreference,
        currentSong = currentSong,
        themedAlbumArtUri = themedAlbumArtUri,
        preparingSongId = preparingSongId,
        systemColorScheme = MaterialTheme.colorScheme
    )
    val albumColorScheme = sheetThemeState.albumColorScheme
    val miniPlayerScheme = sheetThemeState.miniPlayerScheme
    val isPreparingPlayback = sheetThemeState.isPreparingPlayback
    val miniReadyAlpha = sheetThemeState.miniReadyAlpha
    val miniAppearScale = sheetThemeState.miniAppearScale
    val playerAreaBackground = sheetThemeState.playerAreaBackground
    // Elevation is only visible in the mini/collapsed state (expansion < 0.18).
    // miniReadyAlpha fades the shadow in during the initial song-appear animation.
    val visualCardShadowElevation by remember(showQueueSheet, miniReadyAlpha) {
        derivedStateOf {
            if (
                showQueueSheet ||
                playerContentExpansionFraction.isRunning ||
                playerContentExpansionFraction.value > 0.18f
            ) {
                0.dp
            } else {
                (3f * miniReadyAlpha).dp
            }
        }
    }

    val sheetInteractionState = rememberSheetInteractionState(
        scope = scope,
        velocityTracker = velocityTracker,
        sheetMotionController = sheetMotionController,
        playerContentExpansionFraction = playerContentExpansionFraction,
        currentSheetTranslationY = currentSheetTranslationY,
        visualOvershootScaleY = visualOvershootScaleY,
        sheetCollapsedTargetY = sheetCollapsedTargetY,
        sheetExpandedTargetY = sheetExpandedTargetY,
        miniPlayerContentHeightPx = miniPlayerContentHeightPx,
        currentSheetContentState = currentSheetContentState,
        showPlayerContentArea = showPlayerContentArea,
        overallSheetTopCornerRadiusProvider = overallSheetTopCornerRadiusProvider,
        playerContentActualBottomRadiusProvider = playerContentActualBottomRadiusProvider,
        useSmoothCorners = useSmoothCorners,
        isDragging = sheetBackAndDragState.isDragging,
        onAnimateSheet = { targetExpanded, animationSpec, initialVelocity ->
            if (animationSpec == null) {
                animatePlayerSheet(targetExpanded = targetExpanded)
            } else {
                animatePlayerSheet(
                    targetExpanded = targetExpanded,
                    animationSpec = animationSpec,
                    initialVelocity = initialVelocity
                )
            }
        },
        onExpandSheetState = { playerViewModel.expandPlayerSheet() },
        onCollapseSheetState = { playerViewModel.collapsePlayerSheet() },
        onDraggingChange = sheetBackAndDragState.onDraggingChange,
        onDraggingPlayerAreaChange = sheetBackAndDragState.onDraggingPlayerAreaChange
    )

    if (!actuallyShowSheetContent) return

    val playerSheetSemanticsDescription = remember(
        currentSheetContentState,
        infrequentPlayerState.currentSong?.title
    ) {
        "PixelPlay player sheet ${currentSheetContentState.name.lowercase()} " +
            (infrequentPlayerState.currentSong?.title ?: "")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, visualSheetTranslationYProvider().roundToInt()) }
            .height(containerHeight),
        shadowElevation = 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = currentBottomPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showPlayerContentArea) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Modifier.layout reads from pixel lambdas during the layout phase —
                            // this avoids recomposition per drag frame (unlike derivedStateOf).
                            // Layout still runs per-frame, but composition is skipped entirely.
                            .layout { measurable, constraints ->
                                val targetHeightPx = playerContentAreaHeightPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val startPaddingPx = currentHorizontalPaddingStartPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val endPaddingPx = currentHorizontalPaddingEndPxProvider()
                                    .toInt().coerceAtLeast(0)
                                val innerWidth = (constraints.maxWidth - startPaddingPx - endPaddingPx)
                                    .coerceAtLeast(0)
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = innerWidth,
                                        maxWidth = innerWidth,
                                        minHeight = targetHeightPx,
                                        maxHeight = targetHeightPx
                                    )
                                )
                                layout(constraints.maxWidth, targetHeightPx) {
                                    placeable.placeRelative(startPaddingPx, 0)
                                }
                            }
                            .miniPlayerDismissHorizontalGesture(
                                enabled = currentSheetContentState == PlayerSheetState.COLLAPSED,
                                handler = miniDismissGestureHandler
                            )
                            .graphicsLayer {
                                translationX = offsetAnimatable.value
                                scaleX = miniAppearScale
                                scaleY = visualOvershootScaleY.value * miniAppearScale
                                alpha = miniReadyAlpha
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            // Always apply Modifier.shadow with the dynamic elevation
                            // (0.dp renders nothing). Keeping the modifier chain
                            // structurally stable avoids the costly relayout/redraw
                            // restructure when the elevation crosses 0.dp during
                            // expand/collapse or right after play/pause.
                            .shadow(
                                elevation = visualCardShadowElevation,
                                shape = sheetInteractionState.playerShadowShape,
                                clip = false
                            )
                            .background(
                                color = playerAreaBackground,
                                shape = sheetInteractionState.playerShadowShape
                            )
                            .clipToBounds()
                            .semantics {
                                contentDescription = playerSheetSemanticsDescription
                            }
                            .playerSheetVerticalDragGesture(
                                enabled = sheetInteractionState.canDragSheet,
                                handler = sheetInteractionState.sheetVerticalDragGestureHandler
                            )
                            .clickable(
                                enabled = tapBackgroundClosesPlayer || currentSheetContentState == PlayerSheetState.COLLAPSED,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                playerViewModel.togglePlayerSheetState()
                            }
                    ) {
                        UnifiedPlayerMiniAndFullLayers(
                            currentSong = infrequentPlayerState.currentSong,
                            miniPlayerScheme = miniPlayerScheme,
                            overallSheetTopCornerRadiusProvider = overallSheetTopCornerRadiusProvider,
                            infrequentPlayerState = infrequentPlayerState,
                            isCastConnecting = isCastConnecting,
                            isPreparingPlayback = isPreparingPlayback,
                            playerContentExpansionFraction = playerContentExpansionFraction,
                            albumColorScheme = albumColorScheme,
                            bottomSheetOpenFraction = bottomSheetOpenFraction,
                            fullPlayerVisualState = fullPlayerVisualState,
                            containerHeight = containerHeight,
                            currentQueueSourceName = currentQueueSourceName,
                            currentSheetContentState = currentSheetContentState,
                            carouselStyle = carouselStyle,
                            fullPlayerLoadingTweaks = fullPlayerLoadingTweaks,
                            isSheetDragGestureActive = sheetBackAndDragState.isDraggingPlayerArea,
                            playerViewModel = playerViewModel,
                            currentPositionProvider = positionToDisplayProvider,
                            isFavorite = isFavorite,
                            shouldRenderFullPlayer = shouldRenderFullPlayer,
                            onShowQueueClicked = sheetActionHandlers.openQueueSheet,
                            onQueueDragStart = sheetActionHandlers.beginQueueDrag,
                            onQueueDrag = sheetActionHandlers.dragQueueBy,
                            onQueueRelease = sheetActionHandlers.endQueueDrag,
                            onShowCastClicked = castSheetState.openCastSheet
                        )
                    }
                }

                UnifiedPlayerPrewarmLayer(
                    prewarmFullPlayer = prewarmFullPlayer && !shouldRenderFullPlayer,
                    currentSong = infrequentPlayerState.currentSong,
                    containerHeight = containerHeight,
                    albumColorScheme = albumColorScheme,
                    currentQueueSourceName = currentQueueSourceName,
                    infrequentPlayerState = infrequentPlayerState,
                    carouselStyle = carouselStyle,
                    fullPlayerLoadingTweaks = fullPlayerLoadingTweaks,
                    playerViewModel = playerViewModel,
                    currentPositionProvider = positionToDisplayProvider,
                    isCastConnecting = isCastConnecting,
                    isFavorite = isFavorite,
                    onShowQueueClicked = sheetActionHandlers.openQueueSheet,
                    onQueueDragStart = sheetActionHandlers.beginQueueDrag,
                    onQueueDrag = sheetActionHandlers.dragQueueBy,
                    onQueueRelease = sheetActionHandlers.endQueueDrag
                )
            }

            BackHandler(enabled = isQueueVisible && !internalIsKeyboardVisible) {
                sheetActionHandlers.animateQueueSheet(false)
            }

            UnifiedPlayerQueueAndSongInfoHost(
                shouldRenderHost = shouldRenderQueueHost,
                isQueueTelemetryActive = isQueueTelemetryActive,
                albumColorScheme = albumColorScheme,
                queueScrimAlpha = queueScrimAlpha,
                showQueueSheet = showQueueSheet,
                queueHiddenOffsetPx = queueHiddenOffsetPx,
                queueSheetOffset = queueSheetOffset,
                queueSheetHeightPx = queueSheetHeightPx,
                onQueueSheetHeightPxChange = onQueueSheetHeightPxChange,
                configurationResetKey = configuration,
                currentQueueSourceName = currentQueueSourceName,
                infrequentPlayerState = infrequentPlayerState,
                playerViewModel = playerViewModel,
                selectedSongForInfo = selectedSongForInfo,
                onSelectedSongForInfoChange = sheetActionHandlers.onSelectedSongForInfoChange,
                onAnimateQueueSheet = sheetActionHandlers.animateQueueSheet,
                onBeginQueueDrag = sheetActionHandlers.beginQueueDrag,
                onDragQueueBy = sheetActionHandlers.dragQueueBy,
                onEndQueueDrag = sheetActionHandlers.endQueueDrag,
                onLaunchSaveQueueOverlay = sheetActionHandlers.onLaunchSaveQueueOverlay,
                onNavigateToAlbum = sheetActionHandlers.onNavigateToAlbum,
                onNavigateToArtist = sheetActionHandlers.onNavigateToArtist,
                onNavigateToGenre = sheetActionHandlers.onNavigateToGenre
            )
        }
    }

    UnifiedPlayerCastLayer(
        showCastSheet = castSheetState.showCastSheet,
        internalIsKeyboardVisible = internalIsKeyboardVisible,
        albumColorScheme = albumColorScheme,
        playerViewModel = playerViewModel,
        onDismiss = castSheetState.dismissCastSheet,
        onExpansionChanged = castSheetState.onCastExpansionChanged
    )

    UnifiedPlayerSaveQueueLayer(
        pendingOverlay = pendingSaveQueueOverlay,
        onDismissOverlay = { sheetModalOverlayController.dismissSaveQueueOverlay() }
    )
}
```

## File 8: `miniplayer_dismiss_gesture_handler.kt`

```kotlin
package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private enum class MiniDismissDragPhase { IDLE, TENSION, SNAPPING, FREE_DRAG }

/**
 * Keeps mini-player dismiss gesture behavior isolated from the sheet host.
 * Logic is unchanged; this only centralizes gesture transitions and animation dispatch.
 */
internal class MiniPlayerDismissGestureHandler(
    private val scope: CoroutineScope,
    private val density: Density,
    private val hapticFeedback: HapticFeedback,
    private val offsetAnimatable: Animatable<Float, AnimationVector1D>,
    private val screenWidthPx: Float,
    private val onDismissPlaylistAndShowUndo: () -> Unit
) {
    private var dragPhase: MiniDismissDragPhase = MiniDismissDragPhase.IDLE
    private var accumulatedDragX: Float = 0f
    private var offsetJob: Job? = null

    fun onDragStart() {
        dragPhase = MiniDismissDragPhase.TENSION
        accumulatedDragX = 0f
        offsetJob?.cancel()
        offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            offsetAnimatable.stop()
        }
    }

    fun onHorizontalDrag(dragAmount: Float) {
        accumulatedDragX += dragAmount

        when (dragPhase) {
            MiniDismissDragPhase.TENSION -> {
                val snapThresholdPx = 100f * density.density
                if (abs(accumulatedDragX) < snapThresholdPx) {
                    val maxTensionOffsetPx = 30f * density.density
                    val dragFraction = (abs(accumulatedDragX) / snapThresholdPx).coerceIn(0f, 1f)
                    val tensionOffset = lerp(0f, maxTensionOffsetPx, dragFraction)
                    offsetJob?.cancel()
                    offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        offsetAnimatable.snapTo(tensionOffset * accumulatedDragX.sign)
                    }
                } else {
                    dragPhase = MiniDismissDragPhase.SNAPPING
                }
            }

            MiniDismissDragPhase.SNAPPING -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                offsetJob?.cancel()
                offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    offsetAnimatable.animateTo(
                        targetValue = accumulatedDragX,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                dragPhase = MiniDismissDragPhase.FREE_DRAG
            }

            MiniDismissDragPhase.FREE_DRAG -> {
                offsetJob?.cancel()
                offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    offsetAnimatable.animateTo(
                        targetValue = accumulatedDragX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                }
            }

            MiniDismissDragPhase.IDLE -> Unit
        }
    }

    fun onDragEnd() {
        dragPhase = MiniDismissDragPhase.IDLE
        offsetJob?.cancel()
        val dismissThreshold = screenWidthPx * 0.4f
        if (abs(accumulatedDragX) > dismissThreshold) {
            val targetDismissOffset = if (accumulatedDragX < 0) -screenWidthPx else screenWidthPx
            offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                offsetAnimatable.animateTo(
                    targetValue = targetDismissOffset,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
                onDismissPlaylistAndShowUndo()
                offsetAnimatable.snapTo(0f)
            }
        } else {
            offsetJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                offsetAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }
}

@Composable
internal fun rememberMiniPlayerDismissGestureHandler(
    scope: CoroutineScope,
    density: Density,
    hapticFeedback: HapticFeedback,
    offsetAnimatable: Animatable<Float, AnimationVector1D>,
    screenWidthPx: Float,
    onDismissPlaylistAndShowUndo: () -> Unit
): MiniPlayerDismissGestureHandler {
    val onDismissPlaylistAndShowUndoState = rememberUpdatedState(onDismissPlaylistAndShowUndo)
    return remember(scope, density, hapticFeedback, offsetAnimatable, screenWidthPx) {
        MiniPlayerDismissGestureHandler(
            scope = scope,
            density = density,
            hapticFeedback = hapticFeedback,
            offsetAnimatable = offsetAnimatable,
            screenWidthPx = screenWidthPx,
            onDismissPlaylistAndShowUndo = { onDismissPlaylistAndShowUndoState.value() }
        )
    }
}

internal fun Modifier.miniPlayerDismissHorizontalGesture(
    enabled: Boolean,
    handler: MiniPlayerDismissGestureHandler
): Modifier {
    if (!enabled) return this
    return this.pointerInput(enabled, handler) {
        detectHorizontalDragGestures(
            onDragStart = { handler.onDragStart() },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                handler.onHorizontalDrag(dragAmount)
            },
            onDragEnd = { handler.onDragEnd() }
        )
    }
}
```



