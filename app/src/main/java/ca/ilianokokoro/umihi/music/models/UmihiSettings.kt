package ca.ilianokokoro.umihi.music.models

import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.UpdateChannel

data class UmihiSettings(
    val updateChannel: UpdateChannel = UpdateChannel.Stable,
    val cookies: Cookies,
    val dataSyncId: String,
    val showPodcastPlaylist: Boolean = true,
    val useSpecialLanguage: Boolean = false,
    val useAudioOffload: Boolean = false,
    val keepScreenOn: Boolean = false,
    val useAnimatedLyrics: Boolean = true,
    val animatedLyricsBlurEnabled: Boolean = true,
    val useImmersiveLyrics: Boolean = true,
    val lyricsAutoHideDelay: Int = 4,
    val showPlayerFileInfo: Boolean = false,
    val playerThemePreference: String = "ALBUM_ART"
)