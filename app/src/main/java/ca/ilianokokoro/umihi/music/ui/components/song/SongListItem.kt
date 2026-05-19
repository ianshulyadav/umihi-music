package ca.ilianokokoro.umihi.music.ui.components.song

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.models.Song
import ca.ilianokokoro.umihi.music.ui.components.PlayingEqIcon
import ca.ilianokokoro.umihi.music.ui.components.SmartImage
import ca.ilianokokoro.umihi.music.ui.components.dropdown.ModernDropdownItem
import ca.ilianokokoro.umihi.music.ui.theme.GoogleSansRounded

@Composable
fun SongListItem(
    song: Song,
    onPress: () -> Unit,
    playNext: () -> Unit,
    addToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    download: (() -> Unit)? = null
) {
    val playerState by PlayerManager.controllerState.collectAsState()
    val isCurrentSong = playerState?.currentMediaItem?.mediaId == song.youtubeId
    val isPlaying = playerState?.isPlaying ?: false

    var expanded by remember { mutableStateOf(false) }

    val transition = updateTransition(
        targetState = isCurrentSong,
        label = "SongListItemTransition"
    )

    val highlightProgress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 400) },
        label = "highlightProgress"
    ) { state ->
        if (state) 1f else 0f
    }

    val animatedCornerRadius = lerpDp(22.dp, 50.dp, highlightProgress)
    val animatedAlbumCornerRadius = lerpDp(10.dp, 50.dp, highlightProgress)

    val surfaceShape = RoundedCornerShape(animatedCornerRadius)
    val albumShape = RoundedCornerShape(animatedAlbumCornerRadius)

    val colors = MaterialTheme.colorScheme
    val baseContainerColor = colors.surfaceContainerLow
    val containerColor = lerpColor(baseContainerColor, colors.primaryContainer, highlightProgress)
    val contentColor = lerpColor(colors.onSurface, colors.onPrimaryContainer, highlightProgress)
    val mvContainerColor = lerpColor(colors.onSurface, colors.primaryContainer, highlightProgress)
    val mvContentColor = lerpColor(colors.surfaceContainerHigh, colors.onPrimaryContainer, highlightProgress)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(surfaceShape)
            .combinedClickable(
                onClick = onPress,
                onLongClick = { expanded = true }
            ),
        shape = surfaceShape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(albumShape)
            ) {
                SmartImage(
                    model = song.thumbnailPath ?: song.thumbnailHref,
                    contentDescription = song.title,
                    shape = albumShape,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    color = contentColor,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (song.downloaded) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(14.dp),
                            imageVector = Icons.Rounded.DownloadForOffline,
                            contentDescription = stringResource(R.string.download),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = "${song.artist} • ${song.duration}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansRounded),
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isCurrentSong) {
                PlayingEqIcon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(width = 18.dp, height = 16.dp),
                    color = contentColor,
                    isPlaying = isPlaying
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box {
                FilledIconButton(
                    onClick = { expanded = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = mvContentColor,
                        contentColor = mvContainerColor
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(24.dp),
                ) {
                    ModernDropdownItem(
                        leadingIcon = Icons.Rounded.PlayCircleOutline,
                        text = stringResource(R.string.play_next),
                        onClick = {
                            playNext()
                            expanded = false
                        }
                    )
                    ModernDropdownItem(
                        leadingIcon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        text = stringResource(R.string.add_to_queue),
                        onClick = {
                            addToQueue()
                            expanded = false
                        }
                    )
                    if (download != null && !song.downloaded) {
                        ModernDropdownItem(
                            leadingIcon = Icons.Rounded.Download,
                            text = stringResource(R.string.download),
                            onClick = {
                                download()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}