@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.home

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.ComposeHelper
import ca.ilianokokoro.umihi.music.models.PlaylistInfo
import ca.ilianokokoro.umihi.music.ui.components.ErrorMessage
import ca.ilianokokoro.umihi.music.ui.components.LoadingAnimation
import ca.ilianokokoro.umihi.music.ui.components.playlist.PlaylistCard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import ca.ilianokokoro.umihi.music.ui.components.ExpressiveChip

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onSettingsButtonPress: () -> Unit,
    onPlaylistPressed: (playlistInfo: PlaylistInfo) -> Unit,
    application: Application,
    homeViewModel: HomeViewModel = viewModel(
        factory =
            HomeViewModel.Factory(application = application)
    )

) {
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle().value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val loggedOut = uiState.screenState is ScreenState.LoggedOut
            val noPlaylistsFound =
                uiState.screenState is ScreenState.LoggedIn && uiState.screenState.playlistInfos.isEmpty()

            if (event == Lifecycle.Event.ON_RESUME && (loggedOut || noPlaylistsFound)) {
                homeViewModel.getPlaylists()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        when (uiState.screenState) {
            is ScreenState.LoggedIn -> {
                val playlists = uiState.screenState.playlistInfos
                var selectedFilter by remember { mutableStateOf("All") }
                val filters = listOf("All", "Playlists", "Downloaded", "Favorites")

                val filteredPlaylists = remember(playlists, selectedFilter) {
                    when (selectedFilter) {
                        "Playlists" -> playlists.filter { it.id != Constants.Downloads.DOWNLOADED_PLAYLIST_ID && it.id != "liked_songs" }
                        "Downloaded" -> playlists.filter { it.id == Constants.Downloads.DOWNLOADED_PLAYLIST_ID }
                        "Favorites" -> playlists.filter { it.id == "liked_songs" }
                        else -> playlists
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        stringResource(R.string.no_playlists),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            filters.forEach { filter ->
                                ExpressiveChip(
                                    label = filter,
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        }

                        if (filteredPlaylists.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "No playlists found",
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val pullState = rememberPullToRefreshState()
                            Box(modifier = Modifier.weight(1f)) {
                                PullToRefreshBox(
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = homeViewModel::refreshPlaylists,
                                    state = pullState,
                                    indicator = {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 12.dp)
                                                .graphicsLayer {
                                                    val scale = if (uiState.isRefreshing) 1f else pullState.distanceFraction.coerceIn(0f, 1f)
                                                    scaleX = scale
                                                    scaleY = scale
                                                    alpha = scale
                                                }
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (uiState.isRefreshing) {
                                                CircularWavyProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            } else {
                                                CircularWavyProgressIndicator(
                                                    progress = { pullState.distanceFraction },
                                                    modifier = Modifier.size(24.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    LazyVerticalGrid(
                                        modifier = Modifier.fillMaxSize(),
                                        columns = GridCells.Adaptive(minSize = 150.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(
                                            bottom = Constants.Ui.SCROLLABLE_BOTTOM_PADDING
                                        )
                                    ) {
                                        itemsIndexed(
                                            items = filteredPlaylists,
                                            key = { index, playlist ->
                                                ComposeHelper.getLazyKey(
                                                    playlist,
                                                    playlist.id,
                                                    index
                                                )
                                            }
                                        ) { _, playlist ->
                                            PlaylistCard(
                                                playlistInfo = playlist,
                                                onClicked = { onPlaylistPressed(playlist) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ScreenState.LoggedOut -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.log_in_message),
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(
                    onClick = onSettingsButtonPress,
                    shapes = ButtonDefaults.shapes()
                )
                {
                    Text(stringResource(R.string.open_settings))
                }
            }

            ScreenState.Loading -> LoadingAnimation()
            is ScreenState.Error -> ErrorMessage(
                ex = uiState.screenState.exception,
                onRetry = homeViewModel::getPlaylists
            )

        }
    }

}
