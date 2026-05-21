package ca.ilianokokoro.umihi.music.core.managers

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import ca.ilianokokoro.umihi.music.core.ApiResult
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printe
import ca.ilianokokoro.umihi.music.core.helpers.UmihiHelper.printd
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import ca.ilianokokoro.umihi.music.data.repositories.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AutoQueueManager — Radio Mode
 *
 * Watches the player queue and automatically appends YouTube-suggested songs
 * when there are ≤2 songs remaining after the current one.
 *
 * Call [attach] from PlaybackService.onCreate() and [detach] from onDestroy().
 */
object AutoQueueManager {

    private const val TRIGGER_REMAINING = 2
    private const val MAX_HISTORY = 30

    private var fetchJob: Job? = null
    private var lastFetchedVideoId: String? = null
    private val addedVideoIds = mutableSetOf<String>()
    private var scope: CoroutineScope? = null
    private var datastoreRepository: DatastoreRepository? = null
    private val songRepository = SongRepository()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            checkAndRefillQueue()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                checkAndRefillQueue()
            }
        }
    }

    fun attach(player: Player, datastoreRepo: DatastoreRepository, coroutineScope: CoroutineScope) {
        scope = coroutineScope
        datastoreRepository = datastoreRepo
        player.addListener(playerListener)
        printd("AutoQueueManager attached")
    }

    fun detach(player: Player?) {
        player?.removeListener(playerListener)
        fetchJob?.cancel()
        scope = null
        datastoreRepository = null
    }

    private fun checkAndRefillQueue() {
        val currentScope = scope ?: return
        val controller = PlayerManager.currentController ?: return

        currentScope.launch(Dispatchers.IO) {
            val settings = datastoreRepository?.settings?.first() ?: return@launch
            if (!settings.autoQueueEnabled) return@launch

            val currentIndex = controller.currentMediaItemIndex
            val totalCount = controller.mediaItemCount
            val remaining = totalCount - currentIndex - 1

            if (remaining > TRIGGER_REMAINING) return@launch

            val currentId = controller.currentMediaItem?.mediaId ?: return@launch
            if (currentId == lastFetchedVideoId) return@launch
            lastFetchedVideoId = currentId

            printd("AutoQueueManager: Only $remaining songs remaining. Fetching related for $currentId")
            fetchAndAppend(currentId, controller)
        }
    }

    private suspend fun fetchAndAppend(videoId: String, player: Player) {
        try {
            songRepository.getRelatedSongs(videoId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val newSongs = result.data
                            .filter { it.youtubeId !in addedVideoIds }
                            .take(5)

                        if (newSongs.isEmpty()) {
                            printd("AutoQueueManager: No new related songs found")
                            return@collect
                        }

                        val mediaItems = newSongs.map { it.mediaItem }

                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            player.addMediaItems(mediaItems)
                        }

                        newSongs.forEach { addedVideoIds.add(it.youtubeId) }

                        // Trim history to avoid unbounded growth
                        if (addedVideoIds.size > MAX_HISTORY) {
                            val excess = addedVideoIds.size - MAX_HISTORY
                            val toRemove = addedVideoIds.take(excess)
                            addedVideoIds.removeAll(toRemove.toSet())
                        }

                        printd("AutoQueueManager: Appended ${newSongs.size} songs to queue")
                    }
                    is ApiResult.Error -> {
                        printe("AutoQueueManager: Failed to fetch related songs: ${result.exception.message}")
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            printe("AutoQueueManager: Exception fetching related songs: ${e.message}")
        }
    }

    /** Call to reset when the user manually clears the queue */
    fun reset() {
        lastFetchedVideoId = null
        addedVideoIds.clear()
        fetchJob?.cancel()
    }
}
