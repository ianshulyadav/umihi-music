package ca.ilianokokoro.umihi.music.ui.screens.player


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.managers.PlayerManager
import ca.ilianokokoro.umihi.music.extensions.getQueue
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import ca.ilianokokoro.umihi.music.data.database.AppDatabase

class PlayerViewModel(application: Application) :
    AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerState())
    val uiState = _uiState.asStateFlow()

    private val localPlaylistRepository = AppDatabase.getInstance(application).playlistRepository()
    private val localSongRepository = AppDatabase.getInstance(application).songRepository()
    private val datastoreRepository = ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository(application)

    private var lastUpdatedSongIndex: Int = -1
    private var lastLoadedLyricsSongId: String? = null
    private var lyricsFetchJob: kotlinx.coroutines.Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentSong()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateIsPlayingState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateIsLoadingState()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateQueue()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val artworkUri = mediaMetadata.artworkUri ?: return
            updateThumbnail(artworkUri)
        }
    }

    private var attachedController: MediaController? = null

    init {
        viewModelScope.launch {
            PlayerManager.controllerState.collect { controller ->
                if (controller != attachedController) {
                    attachedController?.removeListener(playerListener)
                    controller?.addListener(playerListener)
                    attachedController = controller
                }

                if (controller != null) {
                    updateCurrentSong()
                    updateIsLoadingState()
                    updateIsPlayingState()
                }
            }
        }

        startProgressUpdate()
        updateCurrentSong()
        updateIsLoadingState()
        updateIsPlayingState()

        viewModelScope.launch {
            ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository(application).settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        useAnimatedLyrics = settings.useAnimatedLyrics,
                        animatedLyricsBlurEnabled = settings.animatedLyricsBlurEnabled,
                        autoQueueEnabled = settings.autoQueueEnabled
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        attachedController?.removeListener(playerListener)
    }

    fun expandPlayerSheet() {
        _uiState.update { it.copy(sheetState = PlayerSheetState.EXPANDED) }
    }

    fun collapsePlayerSheet() {
        _uiState.update { it.copy(sheetState = PlayerSheetState.COLLAPSED) }
    }

    fun togglePlayerSheetState() {
        _uiState.update {
            it.copy(
                sheetState = if (it.sheetState == PlayerSheetState.COLLAPSED) {
                    PlayerSheetState.EXPANDED
                } else {
                    PlayerSheetState.COLLAPSED
                }
            )
        }
    }

    fun dismissPlaylist() {
        val controller = PlayerManager.currentController ?: return
        controller.stop()
        controller.clearMediaItems()
        lastUpdatedSongIndex = -1
        _uiState.update {
            it.copy(
                queue = mutableListOf(),
                currentIndex = -1,
                isPlaying = false,
                sheetState = PlayerSheetState.COLLAPSED
            )
        }
    }



    fun seekPlayer() {
        PlayerManager.currentController?.seekTo(_uiState.value.playbackProgress.position.toLong())
    }

    fun seek(location: Float) {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(
                    playbackProgress = PlaybackProgress(
                        duration = it.playbackProgress.duration,
                        position = location,
                    ),
                )
            }
        }
    }

    fun updateSeekBarHeldState(isHeld: Boolean) {
        viewModelScope.launch {
            if (_uiState.value.isSeekBarHeld == isHeld) {
                return@launch
            }


            _uiState.update {
                _uiState.value.copy(
                    isSeekBarHeld = isHeld,
                )
            }
        }
    }

    fun setQueueVisibility(show: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(
                    isQueueModalShown = show
                )
            }
        }
    }

    private val currentSong: Song?
        get() = _uiState.value.queue.getOrNull(_uiState.value.currentIndex)


    private fun updateCurrentSong() {
        val newIndex = PlayerManager.currentController?.currentMediaItemIndex ?: return
        if (newIndex == lastUpdatedSongIndex) {
            return
        }
        lastUpdatedSongIndex = newIndex

        viewModelScope.launch {
            val controller = PlayerManager.currentController
            val newQueue = controller?.getQueue() ?: emptyList()
            val newSong = newQueue.getOrNull(newIndex)

            // Look up favorite status of the new song immediately (on Dispatchers.IO)
            val isFav = if (newSong != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    localPlaylistRepository.isSongInPlaylist("liked_songs", newSong.youtubeId)
                }
            } else {
                false
            }

            _uiState.update {
                it.copy(
                    currentIndex = newIndex,
                    queue = newQueue.toMutableList(),
                    isFavorite = isFav,
                    audioFormat = null,
                    audioSize = null,
                    audioBitrate = null,
                    playbackProgress = PlaybackProgress(
                        duration = 0f,
                        position = 0f,
                    )
                )
            }

            newSong?.let {
                observeFavoriteStatus(it)
                updateAudioDetails(it)
                loadLyricsForSong(it)
            }
        }
    }

    private var favoriteStatusJob: kotlinx.coroutines.Job? = null
    private var audioDetailsJob: kotlinx.coroutines.Job? = null

    private fun updateAudioDetails(song: Song) {
        audioDetailsJob?.cancel()
        audioDetailsJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Check if downloaded
            if (song.audioFilePath != null) {
                val file = java.io.File(song.audioFilePath)
                if (file.exists()) {
                    val sizeMb = String.format(java.util.Locale.US, "%.1f MB", file.length() / (1024f * 1024f))
                    val format = when {
                        song.audioFilePath.endsWith(".mp3", ignoreCase = true) -> "MP3"
                        song.audioFilePath.endsWith(".m4a", ignoreCase = true) -> "M4A"
                        song.audioFilePath.endsWith(".opus", ignoreCase = true) -> "Opus"
                        song.audioFilePath.endsWith(".flac", ignoreCase = true) -> "FLAC"
                        else -> "Local"
                    }
                    _uiState.update {
                        it.copy(
                            audioFormat = format,
                            audioSize = sizeMb,
                            audioBitrate = "320 kbps"
                        )
                    }
                    return@launch
                }
            }

            // 2. Try to get stream details from DB
            var dbSong = localSongRepository.getSong(song.youtubeId)
            var url = dbSong?.streamUrl

            var checkCount = 0
            while (url == null && checkCount < 10) {
                kotlinx.coroutines.delay(500)
                dbSong = localSongRepository.getSong(song.youtubeId)
                url = dbSong?.streamUrl
                checkCount++
            }

            if (url == null) {
                try {
                    url = ca.ilianokokoro.umihi.music.core.helpers.YoutubeHelper.getSongPlayerUrl(
                        getApplication(),
                        song,
                        allowLocal = false
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }

            if (url != null) {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .head()
                    .build()
                try {
                    val client = okhttp3.OkHttpClient()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                            val contentType = response.header("Content-Type") ?: ""

                            val format = when {
                                contentType.contains("webm") -> "WebM"
                                contentType.contains("mp4") || contentType.contains("m4a") -> "M4A"
                                contentType.contains("mpeg") || contentType.contains("mp3") -> "MP3"
                                contentType.contains("ogg") || contentType.contains("opus") -> "Opus"
                                else -> "Opus"
                            }

                            val sizeMb = if (contentLength > 0L) {
                                String.format(java.util.Locale.US, "%.1f MB", contentLength / (1024f * 1024f))
                            } else {
                                "4.2 MB"
                            }

                            val bitrate = if (format == "WebM" || format == "Opus") "160 kbps" else "128 kbps"

                            _uiState.update {
                                it.copy(
                                    audioFormat = format,
                                    audioSize = sizeMb,
                                    audioBitrate = bitrate
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    audioFormat = "Opus",
                                    audioSize = "4.2 MB",
                                    audioBitrate = "160 kbps"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            audioFormat = "Opus",
                            audioSize = "4.2 MB",
                            audioBitrate = "160 kbps"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        audioFormat = "Opus",
                        audioSize = "4.2 MB",
                        audioBitrate = "160 kbps"
                    )
                }
            }
        }
    }

    private fun observeFavoriteStatus(song: Song) {
        favoriteStatusJob?.cancel()
        favoriteStatusJob = viewModelScope.launch {
            localPlaylistRepository.observeIsSongInPlaylist("liked_songs", song.youtubeId)
                .collect { isFav ->
                    _uiState.update {
                        it.copy(isFavorite = isFav)
                    }
                }
        }

        // Sync user's liked songs from YouTube in the background
        ca.ilianokokoro.umihi.music.core.helpers.LikedSongsSyncHelper.syncLikedSongsIfNeeded(
            getApplication(),
            viewModelScope
        )
    }

    fun toggleFavorite() {
        val song = currentSong ?: return
        viewModelScope.launch {
            val isCurrentlyFav = _uiState.value.isFavorite
            if (isCurrentlyFav) {
                localPlaylistRepository.deleteCrossRef("liked_songs", song.youtubeId)
                _uiState.update { it.copy(isFavorite = false) }
            } else {
                val playlistInfo = ca.ilianokokoro.umihi.music.models.PlaylistInfo(
                    id = "liked_songs",
                    title = "Liked Songs"
                )
                localPlaylistRepository.insertPlaylist(playlistInfo)
                localSongRepository.create(song)
                localPlaylistRepository.insertCrossRef(
                    ca.ilianokokoro.umihi.music.models.PlaylistSongCrossRef("liked_songs", song.youtubeId)
                )
                _uiState.update { it.copy(isFavorite = true) }
            }

            // Sync with YouTube if user is logged in
            try {
                val settings = datastoreRepository.getSettings()
                if (settings.cookies.toRawCookie().isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        if (isCurrentlyFav) {
                            ca.ilianokokoro.umihi.music.core.helpers.YoutubeRequestHelper.removeLike(song.youtubeId, settings)
                        } else {
                            ca.ilianokokoro.umihi.music.core.helpers.YoutubeRequestHelper.like(song.youtubeId, settings)
                        }
                    }
                }
            } catch (e: Exception) {
                ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe("Failed to sync favorite with YouTube: ${e.message}")
                // Rollback local database and UI state if sync failed
                if (isCurrentlyFav) {
                    val playlistInfo = ca.ilianokokoro.umihi.music.models.PlaylistInfo(
                        id = "liked_songs",
                        title = "Liked Songs"
                    )
                    localPlaylistRepository.insertPlaylist(playlistInfo)
                    localSongRepository.create(song)
                    localPlaylistRepository.insertCrossRef(
                        ca.ilianokokoro.umihi.music.models.PlaylistSongCrossRef("liked_songs", song.youtubeId)
                    )
                    _uiState.update { it.copy(isFavorite = true) }
                } else {
                    localPlaylistRepository.deleteCrossRef("liked_songs", song.youtubeId)
                    _uiState.update { it.copy(isFavorite = false) }
                }
            }
        }
    }

    private fun updateQueue() {
        val controller = PlayerManager.currentController ?: return

        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(
                    currentIndex = controller.currentMediaItemIndex,
                    queue = controller.getQueue(),
                )
            }
        }
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                val state = _uiState.value

                if (!state.isSeekBarHeld && !state.isLoading) {
                    val controller = PlayerManager.currentController

                    val rawPosition = controller?.currentPosition
                    val rawDuration = controller?.duration

                    val current = state.playbackProgress

                    val safeDuration = when {
                        rawDuration == null -> current.duration
                        rawDuration == C.TIME_UNSET -> 0f
                        rawDuration <= 0 -> 0f
                        else -> rawDuration.toFloat()
                    }

                    val safePosition = when {
                        rawPosition == null -> current.position
                        rawPosition < 0 -> 0f
                        rawDuration == null || rawDuration == C.TIME_UNSET -> 0f
                        else -> rawPosition
                            .coerceAtMost(rawDuration)
                            .toFloat()
                    }.coerceIn(0f, safeDuration)

                    if (
                        safePosition != current.position ||
                        safeDuration != current.duration
                    ) {
                        _uiState.update {
                            it.copy(
                                playbackProgress = PlaybackProgress(
                                    position = safePosition,
                                    duration = safeDuration
                                )
                            )
                        }
                    }
                }

                delay(Constants.Player.PROGRESS_UPDATE_DELAY)
            }
        }
    }

    private fun updateIsLoadingState() {
        viewModelScope.launch {
            when (PlayerManager.currentController?.playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update {
                        _uiState.value.copy(
                            isLoading = true
                        )
                    }
                }

                Player.STATE_READY -> {
                    _uiState.update {
                        _uiState.value.copy(
                            isLoading = false
                        )
                    }
                }

                else -> {
                }
            }
        }
    }

    private fun updateIsPlayingState() {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(
                    isPlaying = PlayerManager.currentController?.isPlaying == true
                )
            }
        }
    }


    private fun resetState() {
        viewModelScope.launch {
            _uiState.update {
                _uiState.value.copy(
                    playbackProgress = PlaybackProgress(
                        duration = 0f,
                        position = 0f,
                    ),
                )
            }
        }
    }

    private fun updateThumbnail(newUri: Uri) {
        _uiState.update { state ->
            val index = state.currentIndex
            val queue = state.queue

            if (index !in queue.indices) return@update state

            val currentSong = queue[index]
            if (currentSong.thumbnailHref == newUri.toString()) return@update state

            val updatedQueue = queue.toMutableList().apply {
                set(index, currentSong.copy(thumbnailHref = newUri.toString()))
            }

            state.copy(queue = updatedQueue)
        }
    }

    fun toggleLyricsVisibility(show: Boolean) {
        _uiState.update { it.copy(showLyrics = show) }
        if (show) {
            val song = currentSong
            if (song != null) {
                loadLyricsForSong(song)
            }
        }
    }

    fun loadLyricsForSong(song: Song) {
        if (_uiState.value.lyrics != null && lastLoadedLyricsSongId == song.uid) {
            return
        }

        lyricsFetchJob?.cancel()
        _uiState.update { it.copy(isLoadingLyrics = true, lyrics = null) }

        lyricsFetchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val client = okhttp3.OkHttpClient()
            try {
                val artistEnc = java.net.URLEncoder.encode(song.artist ?: "", "UTF-8")
                val titleEnc = java.net.URLEncoder.encode(song.title ?: "", "UTF-8")
                val url = "https://lrclib.net/api/get?artist_name=$artistEnc&track_name=$titleEnc"

                val request = okhttp3.Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val jsonElement = json.parseToJsonElement(responseBody)
                    val syncedLrc = jsonElement.jsonObject["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                    val plainLrc = jsonElement.jsonObject["plainLyrics"]?.jsonPrimitive?.contentOrNull

                    val parsed = ca.ilianokokoro.umihi.music.ui.screens.player.lyrics.LyricsParser.parse(syncedLrc ?: plainLrc)
                    _uiState.update { it.copy(lyrics = parsed, isLoadingLyrics = false) }
                    lastLoadedLyricsSongId = song.uid
                } else {
                    val searchUrl = "https://lrclib.net/api/search?q=$artistEnc+$titleEnc"
                    val searchRequest = okhttp3.Request.Builder().url(searchUrl).build()
                    val searchResponse = client.newCall(searchRequest).execute()
                    val searchResponseBody = searchResponse.body?.string()

                    if (searchResponse.isSuccessful && !searchResponseBody.isNullOrEmpty()) {
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val array = json.parseToJsonElement(searchResponseBody).jsonArray
                        if (array.isNotEmpty()) {
                            val firstResult = array[0].jsonObject
                            val syncedLrc = firstResult["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                            val plainLrc = firstResult["plainLyrics"]?.jsonPrimitive?.contentOrNull

                            val parsed = ca.ilianokokoro.umihi.music.ui.screens.player.lyrics.LyricsParser.parse(syncedLrc ?: plainLrc)
                            _uiState.update { it.copy(lyrics = parsed, isLoadingLyrics = false) }
                            lastLoadedLyricsSongId = song.uid
                        } else {
                            _uiState.update { it.copy(isLoadingLyrics = false, lyrics = null) }
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingLyrics = false, lyrics = null) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoadingLyrics = false, lyrics = null) }
            }
        }
    }

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _sleepTimerRemaining = MutableStateFlow<Long?>(null)
    val sleepTimerRemaining = _sleepTimerRemaining.asStateFlow()

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = null
            return
        }
        val durationMs = minutes * 60 * 1000L
        val endTime = System.currentTimeMillis() + durationMs

        sleepTimerJob = viewModelScope.launch {
            while (System.currentTimeMillis() < endTime) {
                val remaining = endTime - System.currentTimeMillis()
                _sleepTimerRemaining.value = remaining
                delay(1000L)
            }
            PlayerManager.currentController?.pause()
            _sleepTimerRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = null
    }

    companion object {
        fun Factory(
            application: Application,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PlayerViewModel(application)
                }
            }
    }
}