package ca.ilianokokoro.umihi.music.ui.navigation

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.ui.components.BackButton
import ca.ilianokokoro.umihi.music.ui.screens.auth.AuthScreen
import ca.ilianokokoro.umihi.music.ui.screens.home.HomeScreen
import ca.ilianokokoro.umihi.music.ui.screens.player.PlayerViewModel
import ca.ilianokokoro.umihi.music.ui.screens.player.components.UnifiedPlayerSheet
import ca.ilianokokoro.umihi.music.ui.screens.playlist.PlaylistScreen
import ca.ilianokokoro.umihi.music.ui.screens.search.SearchScreen
import ca.ilianokokoro.umihi.music.ui.screens.settings.SettingsScreen
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeScreenKey)
    val app = LocalContext.current.applicationContext as Application
    val currentScreen = backStack.last()
    val screenConfig = rememberScreenUiConfig(currentScreen)

    val context = LocalContext.current
    val datastoreRepository = remember { ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository(context) }
    val settings by datastoreRepository.settings.collectAsState(
        initial = ca.ilianokokoro.umihi.music.models.UmihiSettings(
            cookies = ca.ilianokokoro.umihi.music.models.Cookies(""),
            dataSyncId = ""
        )
    )

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(application = app)
    )
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isPlayerSheetExpanded = playerUiState.sheetState == ca.ilianokokoro.umihi.music.ui.screens.player.PlayerSheetState.EXPANDED

    SharedTransitionLayout {
        Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),

        topBar = {
            val hasTitle = screenConfig.titleId != 0 || screenConfig.title.isNotBlank()
            if (screenConfig.showBack || hasTitle) {
                TopAppBar(
                    title = {
                        AnimatedVisibility(
                            visible = hasTitle,
                            enter = fadeIn(tween(Constants.Animation.NAVIGATION_DURATION)),
                            exit = fadeOut(tween(Constants.Animation.NAVIGATION_DURATION))
                        ) {
                            when {
                                screenConfig.titleId != 0 ->
                                    Text(stringResource(screenConfig.titleId))

                                screenConfig.title.isNotBlank() ->
                                    Text(screenConfig.title)
                            }
                        }
                    },
                    navigationIcon = {
                        if (screenConfig.showBack) {
                            BackButton(onBack = backStack::safePop)
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = screenConfig.showBottomBar && !isPlayerSheetExpanded,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    BottomNavigationBar(
                        currentTab = screenConfig.selectedTab,
                        onTabSelected = { key ->
                            if (backStack.last() != key) backStack.add(key)
                        }
                    )
                }
            }
        }

    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter
            ) {
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = backStack::safePop,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        slideInHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            targetOffsetX = { -it / 3 }
                        ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))
                    },
                    popTransitionSpec = {
                        slideInHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))
                    },
                    predictivePopTransitionSpec = {
                        slideInHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(Constants.Animation.NAVIGATION_DURATION),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(Constants.Animation.NAVIGATION_DURATION))
                    },
                    entryProvider = { key ->
                        when (key) {
                            is HomeScreenKey -> NavEntry(key) {
                                HomeScreen(
                                    onSettingsButtonPress = { backStack.add(SettingsScreenKey) },
                                    onPlaylistPressed = { playlist ->
                                        backStack.add(PlaylistScreenKey(playlistInfo = playlist))
                                    },
                                    application = app
                                )
                            }

                            is SettingsScreenKey -> NavEntry(key) {
                                SettingsScreen(
                                    openAuthScreen = { backStack.add(AuthScreenKey) },
                                    application = app
                                )
                            }

                            is PlaylistScreenKey -> NavEntry(key) {
                                PlaylistScreen(
                                    playlistInfo = key.playlistInfo,
                                    onOpenPlayer = playerViewModel::expandPlayerSheet,
                                    application = app
                                )
                            }

                            is AuthScreenKey -> NavEntry(key) {
                                AuthScreen(
                                    onBack = backStack::safePop,
                                    application = app
                                )
                            }

                            is SearchScreenKey -> NavEntry(key) {
                                SearchScreen(
                                    application = app,
                                )
                            }

                            else -> throw RuntimeException(
                                app.getString(
                                    R.string.invalid_navkey,
                                    key
                                )
                            )
                        }
                    }
                )
            }

            UnifiedPlayerSheet(
                playerViewModel = playerViewModel,
                showMiniPlayer = screenConfig.showMiniPlayer,
                bottomBarVisible = screenConfig.showBottomBar && !isPlayerSheetExpanded,
                scaffoldBottomPadding = paddingValues.calculateBottomPadding(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    }
}


fun NavBackStack<NavKey>.safePop() {
    if (this.size > 1) {
        this.removeLastOrNull()
    } else {
        printe("Backstack Pop was called unsafely")
    }
}