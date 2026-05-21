@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package ca.ilianokokoro.umihi.music.ui.screens.player.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import ca.ilianokokoro.umihi.music.ui.components.ExpressiveSwitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.components.WavySliderExpressive
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded
import ca.ilianokokoro.umihi.music.ui.theme.PixelPlayStatusBarStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

@Composable
fun LyricsSheet(
    onClose: () -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by playerViewModel.uiState.collectAsState()
    BackHandler { onClose() }
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

    // Internal Lyrics Display, Style, & Control States
    var showSyncedLyrics by remember(uiState.lyrics) {
        mutableStateOf(uiState.lyrics?.synced != null)
    }
    var localOffsetMillis by remember { mutableIntStateOf(0) }
    var showSyncControls by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // User Custom Settings
    var showTranslation by remember { mutableStateOf(true) }
    var showRomanization by remember { mutableStateOf(true) }
    var lyricsAlignment by remember { mutableStateOf(TextAlign.Start) }
    var lyricsFontSize by remember { mutableStateOf(22.sp) }
    var bgBlurIntensity by remember { mutableStateOf(80.dp) }
    var animationSpeedMs by remember { mutableStateOf(300) }

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

    // Dynamic status bar translucent/immersive integration
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val isSystemDark = isSystemInDarkTheme()
    PixelPlayStatusBarStyle(
        color = Color.Transparent,
        useDarkIcons = if (isSystemDark) false else (androidx.core.graphics.ColorUtils.calculateLuminance(primaryContainer.toArgb()) > 0.5)
    )

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
        LaunchedEffect(isImmersiveActive, settings.useImmersiveLyrics, settings.useImmersiveLyricsStatusBar) {
            when {
                settings.useImmersiveLyrics && isImmersiveActive && settings.useImmersiveLyricsStatusBar -> {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior =
                        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                settings.useImmersiveLyricsStatusBar -> {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
                else -> {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val albumColorSchemePair = ca.ilianokokoro.umihi.music.ui.theme.LocalAlbumColorScheme.current
    val isDarkTheme = ca.ilianokokoro.umihi.music.ui.theme.LocalPixelPlayDarkTheme.current
    val activeScheme = albumColorSchemePair?.let { if (isDarkTheme) it.dark else it.light }
    val albumArtColor = activeScheme?.background ?: MaterialTheme.colorScheme.surface

    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = MaterialTheme.colorScheme.onPrimary
    val containerColor = Color.Black
    val onBackgroundColor = Color.White.copy(alpha = 0.7f)

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
        // Living cinematic blurred cover artwork background
        if (currentSong != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "livingBackground")
            val translationX by infiniteTransition.animateFloat(
                initialValue = -25f,
                targetValue = 25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bgX"
            )
            val translationY by infiniteTransition.animateFloat(
                initialValue = -25f,
                targetValue = 25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(22000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bgY"
            )
            val scaleBg by infiniteTransition.animateFloat(
                initialValue = 1.05f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bgScale"
            )

            SmartImage(
                model = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleBg
                        scaleY = scaleBg
                        this.translationX = translationX
                        this.translationY = translationY
                    }
                    .blur(bgBlurIntensity),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.85f),
                            albumArtColor.copy(alpha = 0.6f),
                            albumArtColor.copy(alpha = 0.75f),
                            albumArtColor.copy(alpha = 0.75f),
                            albumArtColor.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        )
                    )
                )
        )

        // --- LYRICS CONTENT ---
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoadingLyrics -> {
                    ContainedLoadingIndicator(
                        indicatorColor = accentColor
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

                        // BUG FIX: Track user dragging explicitly. Programmatic auto-scrolling does NOT hide controls
                        val isUserDragging by lazyListState.interactionSource.collectIsDraggedAsState()
                        LaunchedEffect(isUserDragging) {
                            if (isUserDragging) {
                                resetInteraction()
                            }
                        }

                        val positionFlow = remember {
                            snapshotFlow { (uiState.playbackProgress.position + localOffsetMillis).toLong() }
                        }

                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = 140.dp,
                                bottom = 140.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyrics.synced) { index, line ->
                                val isActive = index == activeLineIndex
                                val scale by animateFloatAsState(
                                    targetValue = if (isActive) 1.08f else 0.94f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "scale"
                                )
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = if (isActive) 1.0f else 0.45f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "alpha"
                                )
                                val animatedColor by animateColorAsState(
                                    targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
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
                                    horizontalAlignment = if (lyricsAlignment == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
                                ) {
                                    Text(
                                        text = line.line,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = fontWeight,
                                            fontSize = lyricsFontSize,
                                            lineHeight = lyricsFontSize * 1.4f,
                                            textAlign = lyricsAlignment
                                        ),
                                        color = animatedColor,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                                    if (lyricsAlignment == TextAlign.Center) 0.5f else 0f,
                                                    0.5f
                                                )
                                                this.alpha = animatedAlpha
                                            }
                                    )

                                    if (showRomanization && !line.romanization.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = line.romanization,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = lyricsFontSize * 0.75f,
                                                lineHeight = (lyricsFontSize * 0.75f) * 1.3f,
                                                textAlign = lyricsAlignment
                                            ),
                                            color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.graphicsLayer {
                                                this.alpha = animatedAlpha * 0.8f
                                            }
                                        )
                                    }

                                    if (showTranslation && !line.translation.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = line.translation,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontFamily = GoogleSansRounded,
                                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                                fontSize = lyricsFontSize * 0.75f,
                                                lineHeight = (lyricsFontSize * 0.75f) * 1.3f,
                                                textAlign = lyricsAlignment
                                            ),
                                            color = if (isActive) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.45f),
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

                        val isStaticDragging by staticListState.interactionSource.collectIsDraggedAsState()
                        LaunchedEffect(isStaticDragging) {
                            if (isStaticDragging) {
                                resetInteraction()
                            }
                        }

                        LazyColumn(
                            state = staticListState,
                            contentPadding = PaddingValues(
                                start = 24.dp,
                                end = 24.dp,
                                top = 140.dp,
                                bottom = 140.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(plainLines) { _, line ->
                                Text(
                                    text = line,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = lyricsFontSize,
                                        lineHeight = lyricsFontSize * 1.4f,
                                        textAlign = lyricsAlignment
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- IMMERSIVE TOP GRADIENT SCRIM ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // --- IMMERSIVE BOTTOM GRADIENT SCRIM ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
        )

        // --- PERMANENTLY VISIBLE FLOATING CAPSULE MINI-PLAYER ---
        Box(
            modifier = Modifier
                .align(if (settings.lyricsMiniPlayerPosition == "BOTTOM") Alignment.BottomCenter else Alignment.TopCenter)
                .then(
                    if (settings.lyricsMiniPlayerPosition == "BOTTOM") {
                        Modifier.navigationBarsPadding().padding(bottom = 116.dp)
                    } else {
                        Modifier.statusBarsPadding()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(36.dp))
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(36.dp)
                )
                .border(BorderStroke(1.5.dp, Color.Black.copy(alpha = 0.08f)), RoundedCornerShape(36.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val miniPlayerTextColor = Color(0xFF1E1E1E)
            val miniPlayerSubtextColor = Color(0xFF666666)
            val miniPlayerIconColor = Color(0xFF1E1E1E)

            if (settings.lyricsMiniPlayerAlignment == "LEFT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CD artwork (Rotating)
                    val currentRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(uiState.isPlaying) {
                        if (uiState.isPlaying) {
                            while (true) {
                                currentRotation.animateTo(
                                    targetValue = currentRotation.value + 360f,
                                    animationSpec = tween(10000, easing = LinearEasing)
                                )
                            }
                        } else {
                            currentRotation.stop()
                        }
                    }

                    SmartImage(
                        model = currentSong?.thumbnailPath ?: currentSong?.thumbnailHref,
                        contentDescription = "Cover Art CD",
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                rotationZ = currentRotation.value % 360f
                            }
                            .clip(CircleShape)
                            .clickable {
                                resetInteraction()
                                val controller = PlayerManager.currentController
                                if (uiState.isPlaying) controller?.pause() else controller?.play()
                            },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title/Artist details column (start-aligned)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentSong?.title ?: "Unknown Title",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = miniPlayerTextColor
                        )
                        Text(
                            text = currentSong?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = GoogleSansRounded,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = miniPlayerSubtextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Equalizer
                    PlayingEqIcon(
                        modifier = Modifier.size(width = 16.dp, height = 14.dp),
                        color = accentColor,
                        isPlaying = uiState.isPlaying
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Customization Settings",
                            tint = miniPlayerIconColor
                        )
                    }

                    // Minimize Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize Lyrics",
                            tint = miniPlayerIconColor
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close Button (leftmost)
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize Lyrics",
                            tint = miniPlayerIconColor
                        )
                    }

                    // Center Content (CD + Text + EQ)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val currentRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(uiState.isPlaying) {
                            if (uiState.isPlaying) {
                                while (true) {
                                    currentRotation.animateTo(
                                        targetValue = currentRotation.value + 360f,
                                        animationSpec = tween(10000, easing = LinearEasing)
                                    )
                                }
                            } else {
                                currentRotation.stop()
                            }
                        }

                        SmartImage(
                            model = currentSong?.thumbnailPath ?: currentSong?.thumbnailHref,
                            contentDescription = "Cover Art CD",
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    rotationZ = currentRotation.value % 360f
                                }
                                .clip(CircleShape)
                                .clickable {
                                    resetInteraction()
                                    val controller = PlayerManager.currentController
                                    if (uiState.isPlaying) controller?.pause() else controller?.play()
                                },
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentSong?.title ?: "Unknown Title",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = miniPlayerTextColor
                            )
                            Text(
                                text = currentSong?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GoogleSansRounded,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = miniPlayerSubtextColor
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        PlayingEqIcon(
                            modifier = Modifier.size(width = 16.dp, height = 14.dp),
                            color = accentColor,
                            isPlaying = uiState.isPlaying
                        )
                    }

                    // Settings button (rightmost)
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Customization Settings",
                            tint = miniPlayerIconColor
                        )
                    }
                }
            }
        }

        // --- FLOATING COMPACT BOTTOM DOCK (Hidden in Immersive Mode) ---
        AnimatedVisibility(
            visible = !isImmersiveActive,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Timing adjustment panel controls (Toggled from settings dialog)
                    AnimatedVisibility(
                        visible = showSyncControls && showSyncedLyrics && uiState.lyrics?.synced != null,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 }
                    ) {
                        LyricsSyncControls(
                            modifier = Modifier.fillMaxWidth(),
                            offsetMillis = localOffsetMillis,
                            onOffsetChange = { localOffsetMillis = it },
                            backgroundColor = onBackgroundColor.copy(alpha = 0.08f),
                            accentColor = accentColor,
                            onAccentColor = onAccentColor,
                            onBackgroundColor = onBackgroundColor
                        )
                    }

                    // Compact control dock row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minimize Button (leftmost)
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Minimize Lyrics",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Play/Pause Button
                        val playPauseRadius by animateDpAsState(
                            targetValue = if (uiState.isPlaying) 12.dp else 22.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "playPauseRadius"
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(playPauseRadius))
                                .background(accentColor)
                                .clickable {
                                    resetInteraction()
                                    val controller = PlayerManager.currentController
                                    if (uiState.isPlaying) controller?.pause() else controller?.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = onAccentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Progress seek slider
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

                        // Synced/Static mode toggle capsule
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), CircleShape)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val syncedBg by animateColorAsState(
                                targetValue = if (showSyncedLyrics) accentColor else Color.Transparent,
                                label = "syncedBg"
                            )
                            val syncedTint by animateColorAsState(
                                targetValue = if (showSyncedLyrics) onAccentColor else MaterialTheme.colorScheme.onSurface,
                                label = "syncedTint"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(46.dp)
                                    .clip(CircleShape)
                                    .background(syncedBg)
                                    .clickable(enabled = !uiState.lyrics?.synced.isNullOrEmpty()) {
                                        resetInteraction()
                                        showSyncedLyrics = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sync",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = syncedTint
                                )
                            }

                            val staticBg by animateColorAsState(
                                targetValue = if (!showSyncedLyrics) accentColor else Color.Transparent,
                                label = "staticBg"
                            )
                            val staticTint by animateColorAsState(
                                targetValue = if (!showSyncedLyrics) onAccentColor else MaterialTheme.colorScheme.onSurface,
                                label = "staticTint"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(46.dp)
                                    .clip(CircleShape)
                                    .background(staticBg)
                                    .clickable {
                                        resetInteraction()
                                        showSyncedLyrics = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Text",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = staticTint
                                )
                            }
                        }
                    }
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

    // Comprehensive Dialog Settings customization
    if (showSettingsDialog) {
        LyricsSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            showSyncedLyrics = showSyncedLyrics,
            onShowSyncedLyricsChange = { showSyncedLyrics = it },
            hasSyncedLyrics = !uiState.lyrics?.synced.isNullOrEmpty(),
            showTranslation = showTranslation,
            onShowTranslationChange = { showTranslation = it },
            showRomanization = showRomanization,
            onShowRomanizationChange = { showRomanization = it },
            lyricsAlignment = lyricsAlignment,
            onLyricsAlignmentChange = { lyricsAlignment = it },
            lyricsFontSize = lyricsFontSize,
            onLyricsFontSizeChange = { lyricsFontSize = it },
            bgBlurIntensity = bgBlurIntensity,
            onBgBlurIntensityChange = { bgBlurIntensity = it },
            animationSpeedMs = animationSpeedMs,
            onAnimationSpeedMsChange = { animationSpeedMs = it },
            showSyncControls = showSyncControls,
            onShowSyncControlsChange = { showSyncControls = it },
            settings = settings,
            settingsRepository = settingsRepository,
            scope = scope
        )
    }
}

@Composable
fun LyricsSettingsDialog(
    onDismiss: () -> Unit,
    showSyncedLyrics: Boolean,
    onShowSyncedLyricsChange: (Boolean) -> Unit,
    hasSyncedLyrics: Boolean,
    showTranslation: Boolean,
    onShowTranslationChange: (Boolean) -> Unit,
    showRomanization: Boolean,
    onShowRomanizationChange: (Boolean) -> Unit,
    lyricsAlignment: TextAlign,
    onLyricsAlignmentChange: (TextAlign) -> Unit,
    lyricsFontSize: androidx.compose.ui.unit.TextUnit,
    onLyricsFontSizeChange: (androidx.compose.ui.unit.TextUnit) -> Unit,
    bgBlurIntensity: Dp,
    onBgBlurIntensityChange: (Dp) -> Unit,
    animationSpeedMs: Int,
    onAnimationSpeedMsChange: (Int) -> Unit,
    showSyncControls: Boolean,
    onShowSyncControlsChange: (Boolean) -> Unit,
    settings: ca.ilianokokoro.umihi.music.models.UmihiSettings,
    settingsRepository: DatastoreRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        title = {
            Text(
                text = "Lyrics Customization",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Section: Layout & Mode
                item {
                    SettingsSectionTitle(title = "Display Options")
                }
                item {
                    SettingsCardGroup {
                        // Synced / Static
                        SettingsSwitchItem(
                            title = "Synced Lyrics",
                            description = "Follow song timing with scrolling animations",
                            checked = showSyncedLyrics,
                            enabled = hasSyncedLyrics,
                            onCheckedChange = onShowSyncedLyricsChange
                        )
                        // Immersive mode
                        SettingsSwitchItem(
                            title = "Immersive Mode",
                            description = "Auto-hide controls during inactive playback",
                            checked = settings.useImmersiveLyrics,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.USE_IMMERSIVE_LYRICS, checked)
                                }
                            }
                        )
                        // Auto-hide delay selection
                        SettingsSpinnerItem(
                            title = "Auto-hide Delay",
                            currentValue = "${settings.lyricsAutoHideDelay} seconds",
                            options = listOf(3, 5, 10, 15),
                            optionToString = { "$it seconds" },
                            onSelect = { sec ->
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.LYRICS_AUTOHIDE_DELAY, sec)
                                }
                            }
                        )
                    }
                }

                // Section: Font & Text Style
                item {
                    SettingsSectionTitle(title = "Text Styles")
                }
                item {
                    SettingsCardGroup {
                        // Font Size Slider
                        SettingsSliderItem(
                            title = "Font Size",
                            value = lyricsFontSize.value,
                            valueRange = 16f..32f,
                            steps = 7,
                            displayValue = "${lyricsFontSize.value.toInt()} sp",
                            onValueChange = { onLyricsFontSizeChange(it.sp) }
                        )
                        // Alignment Selector
                        SettingsRowToggle(
                            title = "Alignment",
                            options = listOf(TextAlign.Start to "Left", TextAlign.Center to "Center"),
                            selected = lyricsAlignment,
                            onSelect = onLyricsAlignmentChange
                        )
                        // Translation Toggle
                        SettingsSwitchItem(
                            title = "Show Translation",
                            description = "Display translated lines if available",
                            checked = showTranslation,
                            onCheckedChange = onShowTranslationChange
                        )
                        // Romanization Toggle
                        SettingsSwitchItem(
                            title = "Show Romanized Lyrics",
                            description = "Display romanized lines for pronunciation",
                            checked = showRomanization,
                            onCheckedChange = onShowRomanizationChange
                        )
                    }
                }

                // Section: Visuals & Motion
                item {
                    SettingsSectionTitle(title = "Visuals & Motion")
                }
                item {
                    SettingsCardGroup {
                        // Background Blur Intensity Slider
                        SettingsSliderItem(
                            title = "Background Blur",
                            value = bgBlurIntensity.value,
                            valueRange = 20f..120f,
                            steps = 9,
                            displayValue = "${bgBlurIntensity.value.toInt()} dp",
                            onValueChange = { onBgBlurIntensityChange(it.dp) }
                        )
                        // Animation Speed Slider
                        SettingsSliderItem(
                            title = "Transition Speed",
                            value = animationSpeedMs.toFloat(),
                            valueRange = 150f..600f,
                            steps = 8,
                            displayValue = "${animationSpeedMs} ms",
                            onValueChange = { onAnimationSpeedMsChange(it.toInt()) }
                        )
                        // Show Timing Controls
                        SettingsSwitchItem(
                            title = "Show Timing Offset Controls",
                            description = "Allows shifting lyrics sync forward or backward",
                            checked = showSyncControls,
                            onCheckedChange = onShowSyncControlsChange
                        )
                        // Technical File Info Toggle
                        SettingsSwitchItem(
                            title = "Show Technical File Info",
                            description = "Display source format and technical details",
                            checked = settings.showPlayerFileInfo,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.SHOW_PLAYER_FILE_INFO, checked)
                                }
                            }
                        )
                    }
                }

                // Section: Floating Mini-Player
                item {
                    SettingsSectionTitle(title = "Floating Mini-Player")
                }
                item {
                    SettingsCardGroup {
                        // Position Toggle
                        SettingsRowToggle(
                            title = "Position",
                            options = listOf("TOP" to "Top", "BOTTOM" to "Bottom"),
                            selected = settings.lyricsMiniPlayerPosition,
                            onSelect = { pos ->
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.LYRICS_MINIPLAYER_POSITION, pos)
                                }
                            }
                        )
                        // Alignment Toggle
                        SettingsRowToggle(
                            title = "Alignment",
                            options = listOf("LEFT" to "Left", "CENTER" to "Center"),
                            selected = settings.lyricsMiniPlayerAlignment,
                            onSelect = { align ->
                                scope.launch {
                                    settingsRepository.save(DatastoreRepository.PreferenceKeys.LYRICS_MINIPLAYER_ALIGNMENT, align)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun SettingsCardGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansRounded
                    ),
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun <T> SettingsSpinnerItem(
    title: String,
    currentValue: String,
    options: List<T>,
    optionToString: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold
            )
        )
        Box {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = optionToString(option), fontFamily = GoogleSansRounded) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> SettingsRowToggle(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { (option, label) ->
                val active = option == selected
                val bg by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "toggleBg"
                )
                val tint by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    label = "toggleTint"
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }
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
        SyncButton(
            text = "-0.5s",
            onClick = { onOffsetChange(offsetMillis - 500) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
        SyncButton(
            text = "-0.1s",
            onClick = { onOffsetChange(offsetMillis - 100) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
        val offsetText = if (offsetMillis == 0) "0.0s" else "${String.format("%.1f", offsetMillis / 1000f)}s"
        SyncButton(
            text = offsetText,
            onClick = { onOffsetChange(0) },
            weight = 1.3f,
            containerColor = if (offsetMillis != 0) accentColor else backgroundColor,
            contentColor = if (offsetMillis != 0) onAccentColor else onBackgroundColor,
            enabled = offsetMillis != 0,
            fontSize = 12.sp
        )
        SyncButton(
            text = "+0.1s",
            onClick = { onOffsetChange(offsetMillis + 100) },
            weight = 1f,
            containerColor = accentColor,
            contentColor = onAccentColor
        )
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
        contentPadding = PaddingValues(0.dp)
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
