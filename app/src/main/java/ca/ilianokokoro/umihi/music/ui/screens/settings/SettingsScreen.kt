@file:OptIn(ExperimentalMaterial3Api::class)

package ca.ilianokokoro.umihi.music.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FeaturedPlayList
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.ilianokokoro.umihi.music.R
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.VersionManager
import ca.ilianokokoro.umihi.music.ui.components.ErrorMessage
import ca.ilianokokoro.umihi.music.ui.components.LoadingAnimation
import ca.ilianokokoro.umihi.music.ui.components.dialog.ConfirmDialog
import ca.ilianokokoro.umihi.music.ui.components.dialog.UpdateChannelDialog
import ca.ilianokokoro.umihi.music.ui.screens.settings.components.BooleanSettingItem
import ca.ilianokokoro.umihi.music.ui.screens.settings.components.SettingsItem
import ca.ilianokokoro.umihi.music.ui.screens.settings.components.SettingsSection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    openAuthScreen: () -> Unit,
    application: Application,
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(application))
) {
    val uiState = settingsViewModel.uiState.collectAsStateWithLifecycle().value

    // Refresh when returning to the screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                settingsViewModel.getSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
            alignment = Alignment.CenterVertically
        )

    ) {
        when (uiState.screenState) {
            is ScreenState.Success -> {
                val state = uiState.screenState
                val shapeFor = { index: Int, total: Int ->
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = if (index == 0) 24.dp else 4.dp,
                        topEnd = if (index == 0) 24.dp else 4.dp,
                        bottomStart = if (index == total - 1) 24.dp else 4.dp,
                        bottomEnd = if (index == total - 1) 24.dp else 4.dp
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.account)
                ) {
                    if (settingsViewModel.isLoggedIn()) {
                        SettingsItem(
                            title = stringResource(R.string.log_out),
                            subtitle = stringResource(R.string.logged_in_message),
                            leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                            shape = shapeFor(0, 2),
                            onClick = settingsViewModel::logOut
                        )
                    } else {
                        SettingsItem(
                            title = stringResource(R.string.log_in),
                            subtitle = stringResource(R.string.logged_out_message),
                            leadingIcon = Icons.AutoMirrored.Outlined.Login,
                            shape = shapeFor(0, 2),
                            onClick = openAuthScreen
                        )
                    }
                    SettingsItem(
                        title = stringResource(R.string.clear_login_info),
                        subtitle = stringResource(R.string.clear_login_message),
                        leadingIcon = Icons.Outlined.Delete,
                        shape = shapeFor(1, 2),
                        onClick = settingsViewModel::clearLogins
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.general),
                ) {
                    BooleanSettingItem(
                        title = stringResource(R.string.show_podcast_playlist_title),
                        subtitle = stringResource(R.string.show_podcast_playlist_description),
                        leadingIcon = Icons.AutoMirrored.Outlined.FeaturedPlayList,
                        value = uiState.screenState.settings.showPodcastPlaylist,
                        shape = shapeFor(0, 2),
                        onToggle = { settingsViewModel.updatePodcastPlaylistVisibility(it) }
                    )
                    BooleanSettingItem(
                        title = stringResource(R.string.keep_screen_on_title),
                        subtitle = stringResource(R.string.keep_screen_on_title_description),
                        leadingIcon = Icons.Outlined.StayCurrentPortrait,
                        value = uiState.screenState.settings.keepScreenOn,
                        shape = shapeFor(1, 2),
                        onToggle = { settingsViewModel.updateKeepScreenOnSetting(it) }
                    )
                }


                SettingsSection(
                    title = stringResource(R.string.playback),
                ) {
                    BooleanSettingItem(
                        title = stringResource(R.string.enable_audio_offload),
                        subtitle = stringResource(R.string.audio_offload_subtitle),
                        leadingIcon = Icons.Outlined.Memory,
                        value = uiState.screenState.settings.useAudioOffload,
                        shape = shapeFor(0, 1),
                        onToggle = { settingsViewModel.updateAudioOffloadSetting(it) }
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.lyrics_settings_title),
                ) {
                    BooleanSettingItem(
                        title = stringResource(R.string.use_animated_lyrics_title),
                        subtitle = stringResource(R.string.use_animated_lyrics_desc),
                        leadingIcon = androidx.compose.material.icons.Icons.Rounded.Lyrics,
                        value = uiState.screenState.settings.useAnimatedLyrics,
                        shape = shapeFor(0, 2),
                        onToggle = { settingsViewModel.updateUseAnimatedLyricsSetting(it) }
                    )
                    BooleanSettingItem(
                        title = stringResource(R.string.animated_lyrics_blur_title),
                        subtitle = stringResource(R.string.animated_lyrics_blur_desc),
                        leadingIcon = androidx.compose.material.icons.Icons.Rounded.Lyrics,
                        value = uiState.screenState.settings.animatedLyricsBlurEnabled,
                        shape = shapeFor(1, 2),
                        onToggle = { settingsViewModel.updateAnimatedLyricsBlurEnabledSetting(it) }
                    )
                }


                SettingsSection(
                    title = stringResource(R.string.data_and_storage),
                ) {
                    SettingsItem(
                        title = stringResource(R.string.delete_downloads),
                        subtitle = stringResource(R.string.clear_data_message),
                        leadingIcon = Icons.Outlined.Delete,
                        shape = shapeFor(0, 1),
                        onClick = {
                            settingsViewModel.updateShowDownloadDeleteConfirm(true)
                        }
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.app_info),
                ) {
                    SettingsItem(
                        title = stringResource(R.string.check_for_updates),
                        subtitle = stringResource(
                            R.string.current_version,
                            VersionManager.getVersionName()
                        ),
                        leadingIcon = Icons.Outlined.Update,
                        shape = shapeFor(0, 2),
                        onClick = settingsViewModel::checkForUpdates
                    )
                    SettingsItem(
                        title = stringResource(R.string.change_update_channel),
                        subtitle = stringResource(
                            R.string.current_update_channel_body,
                            state.settings.updateChannel
                        ),
                        leadingIcon = Icons.Outlined.SystemUpdate,
                        shape = shapeFor(1, 2),
                        onClick = {
                            settingsViewModel.updateShowUpdateChannelDialog(true)
                        }
                    )
                }

                Box(modifier = Modifier.height(Constants.Ui.SCROLLABLE_BOTTOM_PADDING))

                if (uiState.showUpdateChannelDialog) {
                    UpdateChannelDialog(
                        selectedOption = state.settings.updateChannel,
                        onChange = {
                            settingsViewModel.changeUpdateChannel(it)
                        }, onClose = {
                            settingsViewModel.updateShowUpdateChannelDialog(false)
                        })
                } else if (uiState.showDownloadDeleteConfirm) {
                    ConfirmDialog(
                        title = stringResource(R.string.download_clear_confirm_title),
                        text = stringResource(R.string.download_clear_confirm_text),
                        onConfirm = {
                            settingsViewModel.clearDownloads()
                            settingsViewModel.updateShowDownloadDeleteConfirm(false)
                        },
                        onDismiss = {
                            settingsViewModel.updateShowDownloadDeleteConfirm(false)
                        })

                }
            }

            ScreenState.Loading -> LoadingAnimation()
            is ScreenState.Error -> ErrorMessage(
                ex = uiState.screenState.exception,
                onRetry = settingsViewModel::getSettings
            )
        }
    }


}


