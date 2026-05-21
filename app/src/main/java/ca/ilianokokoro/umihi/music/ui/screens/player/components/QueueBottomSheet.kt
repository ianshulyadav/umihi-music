package ca.ilianokokoro.umihi.music.ui.screens.player.components

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.media3.common.C
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.core.helpers.ComposeHelper
import ca.ilianokokoro.umihi.music.extensions.removeSongFromQueue
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.PlayingEqIcon
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.components.dropdown.ModernDropdownItem
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.toArgb
import ca.ilianokokoro.umihi.music.ui.theme.PixelPlayStatusBarStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    changeVisibility: (visible: Boolean) -> Unit,
    currentSong: Song?,
    songs: List<Song>,
    playerViewModel: ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    var mutableSongList by remember { mutableStateOf(songs) }
    var startIndex by remember { mutableIntStateOf(0) }
    var showTimerOptions by remember { mutableStateOf(false) }
    var showClearQueueDialog by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }
    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        mutableSongList = mutableSongList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }

        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LaunchedEffect(null) {
        val indexToScroll = songs.indexOf(currentSong)
        if (indexToScroll >= 0) {
            lazyListState.animateScrollToItem(index = indexToScroll)
        }
    }

    if (showTimerOptions) {
        val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
        TimerOptionsBottomSheet(
            remainingMillis = sleepTimerRemaining,
            onSetTimer = { mins -> playerViewModel.startSleepTimer(mins) },
            onCancelTimer = { playerViewModel.cancelSleepTimer() },
            onDismiss = { showTimerOptions = false }
        )
    }

    BackHandler {
        changeVisibility(false)
    }

    val isSystemDark = isSystemInDarkTheme()
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    PixelPlayStatusBarStyle(
        color = Color.Transparent,
        useDarkIcons = if (isSystemDark) false else (androidx.core.graphics.ColorUtils.calculateLuminance(containerColor.toArgb()) > 0.5)
    )

    ModalBottomSheet(
        onDismissRequest = {
            changeVisibility(false)
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = containerColor,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
            ) {
                // Next Up header styled like PixelPlayer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.playing_now),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (mutableSongList.isEmpty()) {
                                stringResource(R.string.queue_empty)
                            } else {
                                val trackCount = mutableSongList.size
                                "$trackCount ${if (trackCount == 1) "track" else "tracks"} lined up"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Source pill/badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                        ),
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 16.dp,
                        end = 12.dp,
                        bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING + 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (mutableSongList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.queue_empty),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = mutableSongList, key = { _, song -> song.uid }
                        ) { index, song ->
                            val isCurrentSong = currentSong == song
                            val canReorder = index > mutableSongList.indexOf(currentSong)

                            ReorderableItem(
                                reorderableLazyListState,
                                key = song.uid,
                                enabled = canReorder
                            ) { isDragging ->
                                val scale by animateFloatAsState(
                                    targetValue = if (isDragging) 1.02f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "scale"
                                )

                                QueuePlaylistSongItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    song = song,
                                    isCurrentSong = isCurrentSong,
                                    isDragging = isDragging,
                                    isDragHandleVisible = canReorder,
                                    onPress = {
                                        PlayerManager.currentController?.seekTo(index, C.TIME_UNSET)
                                    },
                                    onRemove = {
                                        mutableSongList = mutableSongList.toMutableList().apply {
                                            removeAt(index)
                                        }
                                        PlayerManager.currentController?.removeSongFromQueue(song)
                                    },
                                    dragHandle = {
                                        val view = LocalView.current
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier
                                                .draggableHandle(
                                                    onDragStarted = {
                                                        startIndex = mutableSongList.indexOf(song)
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.GESTURE_START
                                                        )
                                                    },
                                                    onDragStopped = {
                                                        ViewCompat.performHapticFeedback(
                                                            view,
                                                            HapticFeedbackConstantsCompat.GESTURE_END
                                                        )
                                                        PlayerManager.currentController?.moveMediaItem(
                                                            startIndex,
                                                            mutableSongList.indexOf(song)
                                                        )
                                                    }
                                                )
                                                .size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DragIndicator,
                                                contentDescription = stringResource(R.string.reorder),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Controls Bar (Styled exactly like PixelPlayer)
            val context = LocalContext.current
            val player by PlayerManager.controllerState.collectAsState()
            val repeatMode = ComposeHelper.rememberRepeatMode(player)
            val isShuffleEnabled = ComposeHelper.rememberShuffleMode(player)
            val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()

            // Unified Scrim and Floating Options Menu Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = isFabExpanded,
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(200)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isFabExpanded = false
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = androidx.compose.animation.scaleIn(
                            initialScale = 0.8f,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + androidx.compose.animation.slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.scaleOut(
                            targetScale = 0.8f,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f),
                            animationSpec = tween(150)
                        ) + androidx.compose.animation.fadeOut(),
                        modifier = Modifier.padding(bottom = 110.dp, end = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentWidth(Alignment.End)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Block clicks to scrim */ },
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        QueueToolbarMenuButton(
                            text = "Clear Queue",
                            icon = Icons.Rounded.Delete,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = {
                                isFabExpanded = false
                                showClearQueueDialog = true
                            }
                        )
                        QueueToolbarMenuButton(
                            text = "Save As Playlist",
                            icon = Icons.Rounded.PlaylistAdd,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                isFabExpanded = false
                                showSavePlaylistDialog = true
                            }
                        )
                        }
                    }
                }
            }

            // Unified opaque M3 Floating Controls Bar
            val isTimerActive = remember {
                derivedStateOf { sleepTimerRemaining != null }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .height(64.dp)
                    .zIndex(40f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Shuffle
                    IconButton(
                        onClick = {
                            PlayerManager.currentController?.let {
                                it.shuffleModeEnabled = !it.shuffleModeEnabled
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isShuffleEnabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isShuffleEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = {
                            PlayerManager.currentController?.let {
                                it.repeatMode = when (it.repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                                    else -> Player.REPEAT_MODE_OFF
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        val repeatIcon = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Toggle Repeat",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Sleep Timer
                    IconButton(
                        onClick = { showTimerOptions = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isTimerActive.value) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isTimerActive.value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Snooze,
                            contentDescription = "Sleep Timer",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Subtle divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )

                    // More Actions FAB
                    IconButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isFabExpanded) 90f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "rotation"
                        )
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Queue Actions",
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }
                }
            }

            if (showClearQueueDialog) {
                AlertDialog(
                    onDismissRequest = { showClearQueueDialog = false },
                    title = { Text("Clear Queue", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to clear the queue?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                PlayerManager.currentController?.clearMediaItems()
                                mutableSongList = emptyList()
                                showClearQueueDialog = false
                            }
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearQueueDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            if (showSavePlaylistDialog) {
                SaveQueueAsPlaylistDialog(
                    songs = mutableSongList,
                    onDismiss = { showSavePlaylistDialog = false },
                    onConfirm = { playlistName ->
                        Toast.makeText(context, "Saved queue as '$playlistName' (Offline Mode)", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueuePlaylistSongItem(
    song: Song,
    isCurrentSong: Boolean,
    isDragging: Boolean,
    isDragHandleVisible: Boolean,
    onPress: () -> Unit,
    onRemove: () -> Unit,
    dragHandle: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    val cornerRadius by animateDpAsState(
        targetValue = if (isCurrentSong) 50.dp else 22.dp,
        label = "cornerRadius"
    )
    val itemShape = RoundedCornerShape(cornerRadius)

    val albumCornerRadius by animateDpAsState(
        targetValue = if (isCurrentSong) 50.dp else 10.dp,
        label = "albumCornerRadius"
    )
    val albumShape = RoundedCornerShape(albumCornerRadius)

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 4.dp else 1.dp,
        label = "elevation"
    )

    val backgroundColor = if (isCurrentSong) colors.primaryContainer else colors.surfaceContainerLow
    val hapticView = LocalView.current
    val dismissScope = rememberCoroutineScope()
    val dismissEnabled = !isCurrentSong && !isDragging
    val density = LocalDensity.current

    val dismissOffsetAnimatable = remember(song.uid) { Animatable(0f) }
    var itemWidthPx by remember { mutableStateOf(0f) }

    val dismissHandler = remember(song.uid, dismissEnabled, itemWidthPx) {
        if (dismissEnabled && itemWidthPx > 0f) {
            QueueItemDismissGestureHandler(
                scope = dismissScope,
                density = density,
                hapticView = hapticView,
                offsetAnimatable = dismissOffsetAnimatable,
                itemWidthPx = itemWidthPx,
                onDismiss = onRemove
            )
        } else null
    }

    val isSwipeTargeted = dismissHandler?.isInDismissZone == true
    val currentOffsetPx = dismissOffsetAnimatable.value
    val revealWidthPx = (-currentOffsetPx).coerceAtLeast(0f)
    val revealProgress = if (density.density > 0f) {
        (revealWidthPx / (56.dp.value * density.density)).coerceIn(0f, 1f)
    } else 0f

    val dismissBackgroundColor by animateColorAsState(
        targetValue = if (isSwipeTargeted) colors.errorContainer else colors.errorContainer.copy(alpha = 0.82f),
        animationSpec = tween(durationMillis = 150),
        label = "dismissBackgroundColor"
    )
    val dismissIconAlpha by animateFloatAsState(
        targetValue = revealProgress * if (isSwipeTargeted) 1f else 0.88f,
        animationSpec = tween(durationMillis = 120),
        label = "dismissIconAlpha"
    )
    val dismissIconScale by animateFloatAsState(
        targetValue = if (isSwipeTargeted) 1.08f else 0.95f,
        animationSpec = tween(durationMillis = 120),
        label = "dismissIconScale"
    )

    var surfaceHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val measuredWidth = coordinates.size.width.toFloat()
                if (measuredWidth != itemWidthPx) itemWidthPx = measuredWidth
            }
    ) {
        // Red background reveal for dismiss gesture (PixelPlayer Style)
        if (revealWidthPx > 0f && surfaceHeightPx > 0f) {
            val revealWidthDp = with(density) { revealWidthPx.toDp() }
            val surfaceHeightDp = with(density) { surfaceHeightPx.toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .height(surfaceHeightDp)
                    .width(revealWidthDp)
                    .clip(CircleShape)
                    .background(dismissBackgroundColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove from Queue",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .graphicsLayer {
                            alpha = dismissIconAlpha
                            scaleX = dismissIconScale
                            scaleY = dismissIconScale
                        },
                    tint = colors.onErrorContainer
                )
            }
        }

        Surface(
            modifier = Modifier
                .graphicsLayer { translationX = currentOffsetPx }
                .onGloballyPositioned { coordinates ->
                    val h = coordinates.size.height.toFloat()
                    if (h != surfaceHeightPx) surfaceHeightPx = h
                }
                .padding(horizontal = 12.dp)
                .clip(itemShape)
                .clickable(
                    enabled = currentOffsetPx == 0f,
                    onClick = onPress
                ),
            shape = itemShape,
            color = backgroundColor,
            tonalElevation = elevation,
            shadowElevation = elevation
        ) {
            val contentModifier = Modifier

            Row(
                modifier = contentModifier.padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = isDragHandleVisible) {
                    dragHandle()
                }

                val dismissGestureModifier = if (dismissEnabled && dismissHandler != null) {
                    Modifier.pointerInput(song.uid, dismissHandler) {
                        detectHorizontalDragGestures(
                            onDragStart = { dismissHandler.onDragStart() },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dismissHandler.onHorizontalDrag(dragAmount)
                            },
                            onDragEnd = { dismissHandler.onDragEnd() },
                            onDragCancel = { dismissHandler.onDragCancel() }
                        )
                    }
                } else Modifier

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(dismissGestureModifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val albumArtPadding by animateDpAsState(
                        targetValue = if (isDragHandleVisible) 6.dp else 12.dp,
                        label = "albumArtPadding"
                    )
                    Spacer(Modifier.width(albumArtPadding))

                    SmartImage(
                        model = song.thumbnailPath ?: song.thumbnailHref,
                        shape = albumShape,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(albumShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrentSong) {
                                colors.onPrimaryContainer
                            } else {
                                colors.onSurface
                            },
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${song.artist} • ${song.duration}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentSong) {
                                colors.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                colors.onSurfaceVariant
                            }
                        )
                    }

                    if (isCurrentSong) {
                        PlayingEqIcon(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 12.dp)
                                .size(width = 18.dp, height = 16.dp),
                            color = colors.onPrimaryContainer,
                            isPlaying = PlayerManager.currentController?.isPlaying == true
                        )
                    } else {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More Options",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                ModernDropdownItem(
                                    leadingIcon = Icons.Rounded.Remove,
                                    text = "Remove from queue",
                                    onClick = {
                                        onRemove()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun QueueToolbarMenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier
            .widthIn(min = 184.dp, max = 260.dp)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun SaveQueueAsPlaylistDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("My Queue Playlist") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Queue as Playlist",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter a name for your playlist:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${songs.size} tracks will be saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(playlistName)
                    onDismiss()
                }
            ) {
                Text(
                    text = "Save",
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansRounded
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = GoogleSansRounded
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}