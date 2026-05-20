package ca.ilianokokoro.umihi.music.ui.screens.player

import ca.ilianokokoro.umihi.music.models.Song


data class PlayerState(
    val queue: MutableList<Song> = mutableListOf(),
    val currentIndex: Int = -1,
    val playbackProgress: PlaybackProgress = PlaybackProgress(),
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val isSeekBarHeld: Boolean = false,
    val isQueueModalShown: Boolean = false,
    val showLyrics: Boolean = false,
    val isLoadingLyrics: Boolean = false,
    val lyrics: ca.ilianokokoro.umihi.music.ui.screens.player.lyrics.Lyrics? = null,
    val useAnimatedLyrics: Boolean = true,
    val animatedLyricsBlurEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val audioFormat: String? = null,
    val audioSize: String? = null,
    val audioBitrate: String? = null
)

data class PlaybackProgress(
    val position: Float = 0f,
    val duration: Float = 0f,
)