package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import kotlinx.coroutines.isActive
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.components.WavySliderExpressive
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsSheet(
    onClose: () -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by playerViewModel.uiState.collectAsState()
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences & Settings Setup
    val settingsRepository = remember { DatastoreRepository(context) }
    val settings by settingsRepository.settings.collectAsState(
        initial = ca.ilianokokoro.umihi.music.models.UmihiSettings(
            cookies = ca.ilianokokoro.umihi.music.models.Cookies(""),
            dataSyncId = ""
        )
    )

    // Internal Lyrics Display & Control States
    var showSyncedLyrics by remember(uiState.lyrics) {
        mutableStateOf(uiState.lyrics?.synced != null)
    }
    var localOffsetMillis by remember { mutableIntStateOf(0) }
    var showSyncControls by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }

    // Immersive Mode Interaction Management
    var isImmersiveActive by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val autoHideDelayMs = remember(settings.lyricsAutoHideDelay) {
        settings.lyricsAutoHideDelay * 1000L
    }

    fun resetInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        isImmersiveActive = false
    }

    LaunchedEffect(lastInteractionTime, settings.useImmersiveLyrics, autoHideDelayMs, uiState.isPlaying) {
        if (settings.useImmersiveLyrics && uiState.isPlaying) {
            delay(autoHideDelayMs)
            isImmersiveActive = true
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    val window = remember(view) {
        var ctx = view.context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        if (ctx is android.app.Activity) ctx.window else null
    }

    if (window != null) {
        val insetsController = remember(window, view) {
            androidx.core.view.WindowCompat.getInsetsController(window, view)
        }
        LaunchedEffect(isImmersiveActive, settings.useImmersiveLyrics) {
            if (settings.useImmersiveLyrics && isImmersiveActive) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Dynamic dynamic palette configuration
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val onBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = MaterialTheme.colorScheme.onPrimary
    val containerColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { resetInteraction() }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {},
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        resetInteraction()
                        if (dragAmount > 20) {
                            onClose()
                        }
                    }
                )
            }
    ) {
        // Blended Album Art Blurred Background
        if (currentSong != null) {
            SmartImage(
                model = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            containerColor.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Premium Technical Header
            AnimatedVisibility(
                visible = !isImmersiveActive,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(40.dp)
                            .background(onBackgroundColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Lyrics",
                            tint = onBackgroundColor
                        )
                    }

                    // Rounded Rectangular Capsule miniplayer on header in lyrics page
                    LyricsTrackInfo(
                        song = currentSong,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        isPlaying = uiState.isPlaying
                    )

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(onBackgroundColor.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More Settings",
                                tint = onBackgroundColor
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Show Technical File Info", fontFamily = GoogleSansRounded) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = settings.showPlayerFileInfo,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                settingsRepository.save(DatastoreRepository.PreferenceKeys.SHOW_PLAYER_FILE_INFO, checked)
                                            }
                                        }
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        settingsRepository.save(DatastoreRepository.PreferenceKeys.SHOW_PLAYER_FILE_INFO, !settings.showPlayerFileInfo)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Timing Sync Controls", fontFamily = GoogleSansRounded) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = showSyncControls,
                                        onCheckedChange = { showSyncControls = it }
                                    )
                                },
                                onClick = { showSyncControls = !showSyncControls }
                            )

                            DropdownMenuItem(
                                text = { Text("Auto-hide Delay Settings", fontFamily = GoogleSansRounded) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showDelayDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main View Container for lyrics listing
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoadingLyrics -> {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    uiState.lyrics == null -> {
                        Text(
                            text = "No lyrics found for this song.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.Medium
                            ),
                            color = onBackgroundColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                    else -> {
                        val lyrics = uiState.lyrics!!
                        val playbackProgress = uiState.playbackProgress.position

                        if (showSyncedLyrics && !lyrics.synced.isNullOrEmpty()) {
                            val activeLineIndex by remember(lyrics, playbackProgress, localOffsetMillis) {
                                derivedStateOf {
                                    val index = lyrics.synced.indexOfLast { (playbackProgress + localOffsetMillis) >= it.time }
                                    if (index != -1) index else 0
                                }
                            }

                            val lazyListState = rememberLazyListState()

                            LaunchedEffect(activeLineIndex) {
                                val target = (activeLineIndex - 2).coerceAtLeast(0)
                                lazyListState.animateScrollToItem(target)
                            }

                            LaunchedEffect(lazyListState.isScrollInProgress) {
                                if (lazyListState.isScrollInProgress) {
                                    resetInteraction()
                                }
                            }

                            val positionFlow = remember {
                                snapshotFlow { (uiState.playbackProgress.position + localOffsetMillis).toLong() }
                            }

                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(lyrics.synced) { index, line ->
                                    val isActive = index == activeLineIndex
                                    val scale by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (isActive) 1.08f else 0.92f,
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                        ),
                                        label = "scale"
                                    )
                                    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (isActive) 1.0f else 0.45f,
                                        animationSpec = androidx.compose.animation.core.tween(300),
                                        label = "alpha"
                                    )
                                    val animatedColor by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (isActive) accentColor else onBackgroundColor,
                                        animationSpec = androidx.compose.animation.core.tween(300),
                                        label = "color"
                                    )

                                    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                resetInteraction()
                                                PlayerManager.currentController?.seekTo((line.time - localOffsetMillis).toLong())
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = line.line,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = fontWeight,
                                                fontSize = 22.sp,
                                                lineHeight = 22.sp * 1.4f,
                                                textAlign = TextAlign.Start
                                            ),
                                            color = animatedColor,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                    this.alpha = animatedAlpha
                                                }
                                        )

                                        if (!line.romanization.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = line.romanization,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    fontFamily = GoogleSansRounded,
                                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                    fontSize = 16.sp,
                                                    lineHeight = 16.sp * 1.3f,
                                                    textAlign = TextAlign.Start
                                                ),
                                                color = if (isActive) MaterialTheme.colorScheme.secondary else onBackgroundColor,
                                                modifier = Modifier.graphicsLayer {
                                                    this.alpha = animatedAlpha * 0.8f
                                                }
                                            )
                                        }

                                        if (!line.translation.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = line.translation,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    fontFamily = GoogleSansRounded,
                                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                                    fontSize = 16.sp,
                                                    lineHeight = 16.sp * 1.3f,
                                                    textAlign = TextAlign.Start
                                                ),
                                                color = onBackgroundColor,
                                                modifier = Modifier.graphicsLayer {
                                                    this.alpha = animatedAlpha * 0.7f
                                                }
                                            )
                                        }

                                        if (isActive && uiState.useAnimatedLyrics) {
                                            val nextTime = lyrics.synced.getOrNull(index + 1)?.time ?: Int.MAX_VALUE
                                            ca.ilianokokoro.umihi.music.ui.screens.player.lyrics.BubblesLine(
                                                positionFlow = positionFlow,
                                                time = line.time,
                                                color = accentColor,
                                                nextTime = nextTime,
                                                modifier = Modifier
                                                    .padding(top = 8.dp)
                                                    .then(
                                                        if (uiState.animatedLyricsBlurEnabled) {
                                                            Modifier.blur(2.dp)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            val plainLines = lyrics.plain ?: emptyList()
                            val staticListState = rememberLazyListState()

                            LaunchedEffect(staticListState.isScrollInProgress) {
                                if (staticListState.isScrollInProgress) {
                                    resetInteraction()
                                }
                            }

                            LazyColumn(
                                state = staticListState,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(plainLines) { _, line ->
                                    Text(
                                        text = line,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 20.sp,
                                            lineHeight = 28.sp,
                                            textAlign = TextAlign.Start
                                        ),
                                        color = onBackgroundColor.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Immersive mode manual feature toggle floating FAB
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isImmersiveActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 24.dp)
                ) {
                    val isFeatureEnabled = settings.useImmersiveLyrics
                    val fabBgColor by animateColorAsState(
                        targetValue = if (isFeatureEnabled) accentColor else onBackgroundColor.copy(alpha = 0.15f),
                        animationSpec = tween(durationMillis = 300),
                        label = "fabBg"
                    )
                    val fabContentColor by animateColorAsState(
                        targetValue = if (isFeatureEnabled) onAccentColor else onBackgroundColor,
                        animationSpec = tween(durationMillis = 300),
                        label = "fabContent"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(fabBgColor)
                            .clickable {
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.USE_IMMERSIVE_LYRICS, !settings.useImmersiveLyrics)
                                }
                                resetInteraction()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFeatureEnabled) Icons.Rounded.Fullscreen else Icons.Rounded.FullscreenExit,
                            contentDescription = "Toggle Immersive Feature",
                            tint = fabContentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Bottom Player Control Panel (Auto-hide in immersive mode)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isImmersiveActive,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerColor.copy(alpha = 0.95f))
                        .padding(bottom = 16.dp, end = 20.dp, start = 20.dp)
                ) {
                    // Timing adjustment panel controls
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSyncControls && showSyncedLyrics && uiState.lyrics?.synced != null,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        LyricsSyncControls(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            offsetMillis = localOffsetMillis,
                            onOffsetChange = { localOffsetMillis = it },
                            backgroundColor = onBackgroundColor.copy(alpha = 0.08f),
                            accentColor = accentColor,
                            onAccentColor = onAccentColor,
                            onBackgroundColor = onBackgroundColor
                        )
                    }

                    // Playback progress slider row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Animating PlayPause box corner radius
                        val playPauseRadius by animateDpAsState(
                            targetValue = if (uiState.isPlaying) 18.dp else 32.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "playPauseShape"
                        )

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(playPauseRadius))
                                .background(accentColor)
                                .clickable {
                                    resetInteraction()
                                    val controller = PlayerManager.currentController
                                    if (uiState.isPlaying) {
                                        controller?.pause()
                                    } else {
                                        controller?.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = uiState.isPlaying,
                                label = "playPauseIcon"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = onAccentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Premium expressive wavy slider
                        WavySliderExpressive(
                            value = uiState.playbackProgress.position,
                            valueRange = 0f..uiState.playbackProgress.duration.coerceAtLeast(1f),
                            isPlaying = uiState.isPlaying && !uiState.isLoading,
                            onValueChange = { newValue ->
                                resetInteraction()
                                playerViewModel.seek(newValue)
                            },
                            onValueChangeFinished = {
                                resetInteraction()
                                playerViewModel.seekPlayer()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Segmented selection bar at the very bottom
                    LyricsFloatingToolbar(
                        onNavigateBack = onClose,
                        showSyncedLyrics = showSyncedLyrics,
                        onShowSyncedLyricsChange = { showSyncedLyrics = it },
                        hasSyncedLyrics = !uiState.lyrics?.synced.isNullOrEmpty(),
                        onMoreClick = { showMoreMenu = true },
                        backgroundColor = onBackgroundColor.copy(alpha = 0.08f),
                        onBackgroundColor = onBackgroundColor,
                        accentColor = accentColor,
                        onAccentColor = onAccentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Overlay Button (Arrow Up) to reveal controls during immersive mode
        AnimatedVisibility(
            visible = isImmersiveActive,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            FilledIconButton(
                onClick = { resetInteraction() },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Show Controls"
                )
            }
        }
    }

    if (showDelayDialog) {
        AutoHideDelayDialog(
            currentDelay = settings.lyricsAutoHideDelay,
            onDismiss = { showDelayDialog = false },
            onSelectDelay = { newDelay ->
                scope.launch {
                    settingsRepository.save(DatastoreRepository.PreferenceKeys.LYRICS_AUTOHIDE_DELAY, newDelay)
                }
            }
        )
    }
}

@Composable
fun ToggleSegmentButton(
    modifier: Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color,
    inactiveColor: Color = Color.Gray,
    activeContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    activeCornerRadius: Dp = 8.dp,
    onClick: () -> Unit,
    text: String
) {
    val targetBgColor = if (active) activeColor else inactiveColor
    val bgColor by animateColorAsState(
        targetValue = if (enabled) targetBgColor else targetBgColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 250),
        label = "segmentBg"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (active) activeCornerRadius else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "segmentCorner"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.graphicsLayer(alpha = if (enabled) 1f else 0.38f)) {
            Text(
                text = text,
                color = if (active) activeContentColor else inactiveContentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LyricsSyncControls(
    modifier: Modifier = Modifier,
    offsetMillis: Int,
    onOffsetChange: (Int) -> Unit,
    backgroundColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    onBackgroundColor: Color
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // -0.5s
        SyncButton(
            text = "-0.5s",
            onClick = { onOffsetChange(offsetMillis - 500) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
        // -0.1s
        SyncButton(
            text = "-0.1s",
            onClick = { onOffsetChange(offsetMillis - 100) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
        // Center Display / Reset
        val offsetText = if (offsetMillis == 0) "0.0s" else "${String.format("%.1f", offsetMillis / 1000f)}s"
        SyncButton(
            text = offsetText,
            onClick = { onOffsetChange(0) },
            weight = 1.3f, // Slightly wider
            containerColor = if (offsetMillis != 0) accentColor else backgroundColor,
            contentColor = if (offsetMillis != 0) onAccentColor else onBackgroundColor,
            enabled = offsetMillis != 0,
            fontSize = 12.sp
        )
        // +0.1s
        SyncButton(
            text = "+0.1s",
            onClick = { onOffsetChange(offsetMillis + 100) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
        // +0.5s
        SyncButton(
            text = "+0.5s",
            onClick = { onOffsetChange(offsetMillis + 500) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
    }
}

@Composable
private fun RowScope.SyncButton(
    text: String,
    onClick: () -> Unit,
    weight: Float,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp) // Tight padding
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun LyricsFloatingToolbar(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    showSyncedLyrics: Boolean,
    onShowSyncedLyricsChange: (Boolean) -> Unit,
    hasSyncedLyrics: Boolean,
    onMoreClick: () -> Unit,
    backgroundColor: Color,
    onBackgroundColor: Color,
    accentColor: Color,
    onAccentColor: Color
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val backInteractionSource = remember { MutableInteractionSource() }
        val isBackPressed by backInteractionSource.collectIsPressedAsState()

        val backPressScale by animateFloatAsState(
            targetValue = if (isBackPressed) 0.82f else 1f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = "backPressScale"
        )

        IconButton(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = backPressScale
                    scaleY = backPressScale
                },
            interactionSource = backInteractionSource,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = backgroundColor,
                contentColor = onBackgroundColor
            ),
            onClick = onNavigateBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = onBackgroundColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(backgroundColor, CircleShape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleSegmentButton(
                modifier = Modifier.weight(1f),
                active = showSyncedLyrics,
                enabled = hasSyncedLyrics,
                activeColor = accentColor,
                inactiveColor = Color.Transparent,
                activeContentColor = onAccentColor,
                inactiveContentColor = onBackgroundColor,
                activeCornerRadius = 50.dp,
                onClick = { onShowSyncedLyricsChange(true) },
                text = "Synced"
            )

            ToggleSegmentButton(
                modifier = Modifier.weight(1f),
                active = !showSyncedLyrics,
                enabled = true,
                activeColor = accentColor,
                inactiveColor = Color.Transparent,
                activeContentColor = onAccentColor,
                inactiveContentColor = onBackgroundColor,
                activeCornerRadius = 50.dp,
                onClick = { onShowSyncedLyricsChange(false) },
                text = "Static"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = backgroundColor,
                contentColor = onBackgroundColor
            ),
            onClick = onMoreClick
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Options",
                tint = onBackgroundColor
            )
        }
    }
}

@Composable
fun AutoHideDelayDialog(
    currentDelay: Int,
    onDismiss: () -> Unit,
    onSelectDelay: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Auto-hide Delay",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                listOf(3, 5, 10, 15).forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectDelay(sec)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentDelay == sec,
                            onClick = {
                                onSelectDelay(sec)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${sec} seconds",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansRounded
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = GoogleSansRounded)
            }
        }
    )
}

@Composable
private fun LyricsTrackInfo(
    song: ca.ilianokokoro.umihi.music.models.Song?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean
) {
    if (song == null) return

    val albumShape = CircleShape
    val currentRotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                currentRotation.animateTo(
                    targetValue = currentRotation.value + 360f,
                    animationSpec = tween(8000, easing = androidx.compose.animation.core.LinearEasing)
                )
            }
        } else {
            currentRotation.stop()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SmartImage(
            model = song.thumbnailPath ?: song.thumbnailHref,
            contentDescription = "Cover Art",
            modifier = Modifier
                .size(40.dp)
                .padding(2.dp)
                .graphicsLayer {
                    rotationZ = currentRotation.value % 360f
                }
                .clip(albumShape),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(vertical = 4.dp)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = GoogleSansRounded,
                    color = Color.Black.copy(alpha = 0.6f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        PlayingEqIcon(
            modifier = Modifier
                .padding(start = 6.dp, end = 12.dp)
                .size(width = 16.dp, height = 14.dp),
            color = MaterialTheme.colorScheme.primary,
            isPlaying = isPlaying
        )
    }
}

@Composable
fun PlayingEqIcon(
    modifier: Modifier = Modifier,
    color: Color,
    isPlaying: Boolean = true,
    bars: Int = 3,
    minHeightFraction: Float = 0.28f,
    maxHeightFraction: Float = 1.0f,
    phaseDurationMillis: Int = 3600,
    wanderDurationMillis: Int = 12000,
    gapFraction: Float = 0.30f
) {
    val fullRotation = (2f * Math.PI).toFloat()
    val phaseAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val wanderAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(isPlaying, phaseDurationMillis) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive) {
            val start = (phaseAnim.value % fullRotation).let { if (it < 0f) it + fullRotation else it }
            phaseAnim.snapTo(start)
            phaseAnim.animateTo(
                targetValue = start + fullRotation,
                animationSpec = tween(durationMillis = phaseDurationMillis, easing = androidx.compose.animation.core.LinearEasing)
            )
        }
    }

    LaunchedEffect(isPlaying, wanderDurationMillis) {
        if (!isPlaying) return@LaunchedEffect
        while (isActive) {
            val start = (wanderAnim.value % fullRotation).let { if (it < 0f) it + fullRotation else it }
            wanderAnim.snapTo(start)
            wanderAnim.animateTo(
                targetValue = start + fullRotation,
                animationSpec = tween(durationMillis = wanderDurationMillis, easing = androidx.compose.animation.core.LinearEasing)
            )
        }
    }

    val phase = phaseAnim.value
    val wander = wanderAnim.value

    val activity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "activity"
    )

    val speeds = remember(bars) { List(bars) { (it + 1).toFloat() } }
    val shifts = remember(bars) { List(bars) { i -> i * 0.9f } }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val tentativeBarW = w / (bars + (bars - 1) * (1f + gapFraction))
        val gap = tentativeBarW * gapFraction
        val barW = tentativeBarW
        val corner = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)

        repeat(bars) { i ->
            val slowShift = 0.6f * kotlin.math.sin(wander + i * 0.4f)
            val slowAmp   = 0.85f + 0.15f * kotlin.math.sin(wander * 0.5f + 1.1f + i * 0.3f)
            val v = (kotlin.math.sin(phase * speeds[i] + shifts[i] + slowShift) * slowAmp + 1f) * 0.5f
            val eased = v * v * (3 - 2 * v)
            val fracBars = minHeightFraction + (maxHeightFraction - minHeightFraction) * eased
            val barH = h * fracBars
            val dotH = barW
            val blendedH = dotH + (barH - dotH) * activity
            val top = (h - blendedH) / 2f
            val left = i * (barW + gap)

            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barW, blendedH),
                cornerRadius = corner
            )
        }
    }
}

