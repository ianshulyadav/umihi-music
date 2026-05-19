package ca.ilianokokoro.umihi.music.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel

@Composable
fun LyricsSheet(
    onClose: () -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by playerViewModel.uiState.collectAsState()
    val currentSong = uiState.queue.getOrNull(uiState.currentIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentSong != null) {
            SmartImage(
                model = currentSong.thumbnailPath ?: currentSong.thumbnailHref,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.45f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Lyrics",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong?.title ?: "Unknown Song",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansRounded,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = GoogleSansRounded
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoadingLyrics -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
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
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                    else -> {
                        val lyrics = uiState.lyrics!!
                        val playbackProgress = uiState.playbackProgress.position

                        if (lyrics.synced.isNotEmpty()) {
                            val activeLineIndex by remember(lyrics, playbackProgress) {
                                derivedStateOf {
                                    val index = lyrics.synced.indexOfLast { playbackProgress >= it.time }
                                    if (index != -1) index else 0
                                }
                            }

                            val lazyListState = rememberLazyListState()

                            LaunchedEffect(activeLineIndex) {
                                val target = (activeLineIndex - 2).coerceAtLeast(0)
                                lazyListState.animateScrollToItem(target)
                            }

                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(lyrics.synced) { index, line ->
                                    val isActive = index == activeLineIndex
                                    val alpha = if (isActive) 1.0f else 0.4f
                                    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                                    val fontSize = if (isActive) 24.sp else 20.sp
                                    val color = if (isActive) MaterialTheme.colorScheme.primary else Color.White

                                    Text(
                                        text = line.line,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = fontWeight,
                                            fontSize = fontSize,
                                            lineHeight = fontSize * 1.4f,
                                            textAlign = TextAlign.Start
                                        ),
                                        color = color.copy(alpha = alpha),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                PlayerManager.currentController?.seekTo(line.time.toLong())
                                            }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(lyrics.plain) { _, line ->
                                    Text(
                                        text = line,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = GoogleSansRounded,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 20.sp,
                                            lineHeight = 28.sp,
                                            textAlign = TextAlign.Start
                                        ),
                                        color = Color.White.copy(alpha = 0.8f),
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
        }
    }
}
