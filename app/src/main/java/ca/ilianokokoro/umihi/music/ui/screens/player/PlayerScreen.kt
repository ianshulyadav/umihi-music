@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class
)

package ca.ilianokokoro.umihi.music.ui.screens.player

import android.app.Application
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu

// Animation configuration for collapse/expand transitions
object PlayerAnimationConfig {
    const val COLLAPSE_EXPAND_DURATION_MS = 400
    const val STAGGER_DELAY_BASE_MS = 0
}

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    application: Application,
    playerViewModel: PlayerViewModel = viewModel(
        factory =
            PlayerViewModel.Factory(application = application)
    ),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState = playerViewModel.uiState.collectAsStateWithLifecycle().value
    val orientation = LocalConfiguration.current.orientation
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val dragThreshold = screenHeightPx * 0.25f
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    // Gesture progress calculations
    val dragProgress = (dragOffsetY.value / screenHeightPx).coerceIn(0f, 1f)
    val containerScale = 1f - (dragProgress * 0.15f)
    val containerCornerRadius = (dragProgress * 36f).dp
    val containerAlpha = 1f - (dragProgress * 0.5f)

    // Dynamic background colors (LocalAlbumColorScheme)
    val albumColorSchemePair = ca.ilianokokoro.umihi.music.ui.theme.LocalAlbumColorScheme.current
    val isDark = ca.ilianokokoro.umihi.music.ui.theme.LocalPixelPlayDarkTheme.current
    val activeScheme = albumColorSchemePair?.let { if (isDark) it.dark else it.light }

    // Linear pastel colors from scheme
    val primaryContainer = activeScheme?.primaryContainer
        ?: MaterialTheme.colorScheme.primaryContainer
    val background = activeScheme?.background
        ?: MaterialTheme.colorScheme.background
    val surface = activeScheme?.surface
        ?: MaterialTheme.colorScheme.surface

    // Staggered reveal animations
    val showContent = remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        showContent.value = true
    }

    val bgFadeProgress by animateFloatAsState(
        targetValue = if (showContent.value) 1f else 0f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS),
        label = "bgFadeProgress"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent.value) 1f else 0f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS, delayMillis = 50),
        label = "contentAlpha"
    )
    val contentOffsetY by animateFloatAsState(
        targetValue = if (showContent.value) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentOffsetY"
    )

    val progressAlpha by animateFloatAsState(
        targetValue = if (showContent.value) 1f else 0f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS, delayMillis = 100),
        label = "progressAlpha"
    )
    val progressOffsetY by animateFloatAsState(
        targetValue = if (showContent.value) 0f else 30f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS, delayMillis = 50),
        label = "progressOffsetY"
    )

    val controlsAlpha by animateFloatAsState(
        targetValue = if (showContent.value) 1f else 0f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS, delayMillis = 150),
        label = "controlsAlpha"
    )
    val controlsOffsetY by animateFloatAsState(
        targetValue = if (showContent.value) 0f else 30f,
        animationSpec = tween(durationMillis = PlayerAnimationConfig.COLLAPSE_EXPAND_DURATION_MS, delayMillis = 100),
        label = "controlsOffsetY"
    )

    // Close the screen in resumed with an empty queue
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack by androidx.compose.runtime.rememberUpdatedState(onBack)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val controller = ca.ilianokokoro.umihi.music.core.managers.PlayerManager.currentController
                if (controller != null && controller.mediaItemCount == 0) {
                    currentOnBack()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val containerModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
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
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(containerModifier)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        primaryContainer.copy(alpha = 0.35f),
                        surface.copy(alpha = 0.95f),
                        background
                    )
                )
            )
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
                translationY = dragOffsetY.value
                alpha = containerAlpha
                clip = true
                shape = RoundedCornerShape(containerCornerRadius)
            }
            .pointerInput(uiState.isQueueModalShown, uiState.showLyrics) {
                if (uiState.isQueueModalShown || uiState.showLyrics) return@pointerInput
                
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffsetY.value > dragThreshold) {
                            onBack()
                        } else {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(0f)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || dragOffsetY.value > 0f) {
                            change.consume()
                            coroutineScope.launch {
                                dragOffsetY.snapTo((dragOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                        } else if (dragAmount < -15f && dragOffsetY.value == 0f) {
                            change.consume()
                            playerViewModel.setQueueVisibility(true)
                        }
                    }
                )
            }
    ) {
        // Blur Art Background
        if (currentSong != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.22f * bgFadeProgress }
                    .blur(50.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
            ) {
                ca.ilianokokoro.umihi.music.ui.components.SmartImage(
                    model = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopPlayerHeader(
                    onBack = onBack,
                    onOpenQueue = { playerViewModel.setQueueVisibility(true) },
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffsetY
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .graphicsLayer { alpha = contentAlpha },
                    contentAlignment = Alignment.Center
                ) {
                    Thumbnail(
                        href = currentSong?.thumbnailHref.toString(),
                        isPlaying = uiState.isPlaying,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlayerControls(
                        song = currentSong,
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
                        onFavoriteToggle = playerViewModel::toggleFavorite,
                        audioFormat = uiState.audioFormat,
                        audioSize = uiState.audioSize,
                        audioBitrate = uiState.audioBitrate,
                        progressAlpha = progressAlpha,
                        progressOffsetY = progressOffsetY,
                        controlsAlpha = controlsAlpha,
                        controlsOffsetY = controlsOffsetY,
                        secondaryAlpha = controlsAlpha,
                        secondaryOffsetY = controlsOffsetY
                    )
                }
            }

        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                TopPlayerHeader(
                    onBack = onBack,
                    onOpenQueue = { playerViewModel.setQueueVisibility(true) },
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha
                        translationY = contentOffsetY
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .graphicsLayer { alpha = contentAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        Thumbnail(
                            href = currentSong?.thumbnailHref.toString(),
                            isPlaying = uiState.isPlaying,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PlayerControls(
                            song = currentSong,
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
                            onFavoriteToggle = playerViewModel::toggleFavorite,
                            audioFormat = uiState.audioFormat,
                            audioSize = uiState.audioSize,
                            audioBitrate = uiState.audioBitrate,
                            progressAlpha = progressAlpha,
                            progressOffsetY = progressOffsetY,
                            controlsAlpha = controlsAlpha,
                            controlsOffsetY = controlsOffsetY,
                            secondaryAlpha = controlsAlpha,
                            secondaryOffsetY = controlsOffsetY
                        )
                    }
                }
            }
        }
    }


    // Back gesture interception for active sheets
    BackHandler(enabled = uiState.isQueueModalShown || uiState.showLyrics) {
        if (uiState.isQueueModalShown) {
            playerViewModel.setQueueVisibility(false)
        } else if (uiState.showLyrics) {
            playerViewModel.toggleLyricsVisibility(false)
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
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
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
            },
            label = "albumArtAnimatedContent"
        ) { targetState ->
            val cornerRadius by animateDpAsState(
                targetValue = if (isPlaying) 32.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "albumCornerRadius"
            )

            val artworkModifier = Modifier

            SquareImage(
                uri = targetState,
                cornerRadius = cornerRadius,
                modifier = Modifier
                    .size(size)
                    .then(artworkModifier)
            )
        }
    }
}


@Composable
fun SongInfo(
    song: Song?,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        val titleModifier = Modifier

        Text(
            text = song?.title ?: "",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .then(titleModifier)
                .basicMarquee()
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
    var showMenu by androidx.compose.runtime.remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back / Dropdown Arrow button
        androidx.compose.material3.IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Go Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        // Title (Perfectly Centered)
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        // Actions Row (Bluetooth/Cast, Queue, & More Menu)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
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
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Cast,
                    contentDescription = "Connect Device",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Queue Button
            androidx.compose.material3.IconButton(
                onClick = onOpenQueue,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "Open Queue",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            // More Options Button
            androidx.compose.material3.IconButton(
                onClick = { showMenu = !showMenu },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Download Track") },
                        onClick = {
                            showMenu = false
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}