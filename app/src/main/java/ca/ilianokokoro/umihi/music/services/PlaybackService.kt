package ca.ilianokokoro.umihi.music.services

import android.app.PendingIntent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_TIMELINE
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.core.ExoCache
import ca.ilianokokoro.umihi.music.core.datasources.YoutubeDataSourceFactory
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.core.managers.AutoQueueManager
import ca.ilianokokoro.umihi.music.core.managers.QueuePreloadManager
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import ca.ilianokokoro.umihi.music.extensions.cappedTo
import ca.ilianokokoro.umihi.music.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var exoCache: ExoCache
    private lateinit var player: Player
    private lateinit var datastoreRepository: DatastoreRepository
    private val songRepository = SongRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Debounce queue-save so we don't write on every frame
    private var queueSaveJob: Job? = null

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    override fun onCreate() {
        super.onCreate()

        datastoreRepository = DatastoreRepository(applicationContext)
        exoCache = ExoCache(application)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, packageName))

        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(exoCache.cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val resolvingFactory = YoutubeDataSourceFactory(application, cacheDataSourceFactory)

        val settings = runBlocking { datastoreRepository.settings.first() }

        val audioOffloadMode = if (settings.useAudioOffload) {
            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
        } else {
            TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
        }

        val audioOffloadPreferences =
            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(audioOffloadMode)
                .setIsGaplessSupportRequired(true)
                .setIsSpeedChangeSupportRequired(true)
                .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setDeviceVolumeControlEnabled(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
            .build()

        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                // Load the full res image when a new song is played
                updateCurrentMediaItemThumbnail(mediaItem)
                // Debounced queue persistence
                scheduleQueueSave()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                scheduleQueueSave()
            }

            // Show toast on error
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_LONG).show()
            }
        })

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(CacheBitmapLoader(DataSourceBitmapLoader.Builder(this).build()))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val commands =
                        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                            .add(COMMAND_GET_TIMELINE)
                            .build()

                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(commands)
                        .build()
                }
            })
            .build()

        // Restore persistent queue BEFORE attaching managers so they see the full queue
        restorePersistentQueue()

        // Attach managers with the ExoPlayer instance
        AutoQueueManager.attach(player, datastoreRepository, serviceScope)
        QueuePreloadManager.attach(player, applicationContext, datastoreRepository, serviceScope)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || player.mediaItemCount == 0) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            // Detach managers
            AutoQueueManager.detach(player)
            QueuePreloadManager.detach(player)
            // Final queue save
            saveQueueNow()
            player.release()
            exoCache.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    // ─── Persistent Queue ────────────────────────────────────────────────────

    private fun restorePersistentQueue() {
        serviceScope.launch {
            try {
                val prefs = datastoreRepository.settings.first()
                val savedJson = runBlocking {
                    datastoreRepository.getPersistentQueue()
                }
                if (savedJson.isBlank()) return@launch

                val state = json.decodeFromString<PersistentQueueState>(savedJson)
                if (state.songs.isEmpty()) return@launch

                val mediaItems = state.songs.map { it.mediaItem }

                withContext(Dispatchers.Main) {
                    player.setMediaItems(
                        mediaItems,
                        state.currentIndex.coerceAtLeast(0),
                        state.positionMs.coerceAtLeast(0L)
                    )
                    player.prepare()
                    // Don't auto-play — user taps play
                }
                UmihiHelper.printd("PlaybackService: Restored ${state.songs.size} songs from persistent queue")
            } catch (e: Exception) {
                printe("PlaybackService: Failed to restore queue: ${e.message}")
            }
        }
    }

    private fun scheduleQueueSave() {
        queueSaveJob?.cancel()
        queueSaveJob = serviceScope.launch {
            delay(800) // debounce
            saveQueueNow()
        }
    }

    private fun saveQueueNow() {
        // Run on Main to safely read ExoPlayer state, then persist on IO
        serviceScope.launch(Dispatchers.Main) {
            try {
                val songs = mutableListOf<Song>()
                for (i in 0 until player.mediaItemCount) {
                    val item = player.getMediaItemAt(i)
                    songs.add(
                        Song(
                            youtubeId = item.mediaId,
                            title = item.mediaMetadata.title?.toString() ?: "",
                            artist = item.mediaMetadata.artist?.toString() ?: "",
                            thumbnailHref = item.mediaMetadata.artworkUri?.toString() ?: ""
                        )
                    )
                }
                val idx = player.currentMediaItemIndex.coerceAtLeast(0)
                val posMs = player.currentPosition.coerceAtLeast(0L)
                val state = PersistentQueueState(songs = songs, currentIndex = idx, positionMs = posMs)
                val encoded = json.encodeToString(state)

                serviceScope.launch(Dispatchers.IO) {
                    datastoreRepository.savePersistentQueue(encoded)
                }
            } catch (e: Exception) {
                printe("PlaybackService: saveQueueNow error: ${e.message}")
            }
        }
    }

    // ─── Thumbnail ───────────────────────────────────────────────────────────

    private fun updateCurrentMediaItemThumbnail(mediaItem: MediaItem?) {
        if (mediaItem == null) return

        val context = applicationContext
        val songId = mediaItem.mediaId

        serviceScope.launch {
            try {
                val imageDir = UmihiHelper.getDownloadDirectory(
                    context,
                    Constants.Downloads.THUMBNAILS_FOLDER
                )

                val downloadedImage = File(imageDir, "$songId.jpg")
                if (downloadedImage.exists()) {
                    val imageBytes = downloadedImage.readBytes()

                    updateMediaItemArtwork(
                        mediaItem,
                        imageBytes.cappedTo(),
                        downloadedImage.toUri()
                    )
                    return@launch
                }

                songRepository.getSongInfo(songId)
                    .collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                val song = result.data
                                val thumbnail = song.thumbnailHref
                                if (thumbnail.isNotBlank()) {
                                    val artBytes = UmihiHelper.fetchArtworkBytes(thumbnail)
                                    if (artBytes != null) {
                                        updateMediaItemArtwork(
                                            mediaItem,
                                            artBytes,
                                            song.thumbnailHref.toUri()
                                        )
                                    }
                                    return@collect
                                }
                            }

                            is ApiResult.Error -> {
                                error("ApiResult.Error was null")
                            }

                            else -> {}
                        }
                    }
            } catch (ex: Exception) {
                printe(
                    message = "Failed to get full res thumbnail for $songId. Error : ${ex.message}",
                )
            }
        }
    }

    private suspend fun updateMediaItemArtwork(
        mediaItem: MediaItem,
        artBytes: ByteArray?,
        uri: Uri
    ) {
        val extras = mediaItem.mediaMetadata.extras
        extras?.putString(
            Constants.ExoPlayer.SongMetadata.UID,
            UUID.randomUUID().toString()
        )

        val updated = mediaItem.buildUpon()
            .setMediaMetadata(
                mediaItem.mediaMetadata.buildUpon()
                    .setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .setArtworkUri(uri)
                    .setExtras(extras)
                    .build()
            )
            .build()

        withContext(Dispatchers.Main) {
            if (player.currentMediaItem?.mediaId == mediaItem.mediaId) {
                player.replaceMediaItem(
                    player.currentMediaItemIndex,
                    updated
                )
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class PersistentQueueState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L
)