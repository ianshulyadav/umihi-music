package ca.ilianokokoro.umihi.music.core.helpers

import android.content.Context
import ca.ilianokokoro.umihi.music.data.database.AppDatabase
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LikedSongsSyncHelper {
    private var lastSyncTime = 0L
    private val SYNC_INTERVAL = 2 * 60 * 1000L // 2 minutes

    fun syncLikedSongsIfNeeded(
        context: Context,
        scope: CoroutineScope
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncTime < SYNC_INTERVAL) {
            return
        }
        lastSyncTime = currentTime

        scope.launch(Dispatchers.IO) {
            try {
                val datastoreRepository = DatastoreRepository(context)
                val settings = datastoreRepository.getSettings()
                if (settings.cookies.isEmpty()) {
                    return@launch
                }

                // Fetch remote Liked Songs playlist ("LM")
                val responseJson = YoutubeRequestHelper.browse("LM", settings)
                val remoteSongs = YoutubeHelper.extractSongList(responseJson, settings)

                if (remoteSongs.isNotEmpty()) {
                    val localPlaylistRepository = AppDatabase.getInstance(context).playlistRepository()
                    val localSongRepository = AppDatabase.getInstance(context).songRepository()

                    val playlistInfo = ca.ilianokokoro.umihi.music.models.PlaylistInfo(
                        id = "liked_songs",
                        title = "Liked Songs"
                    )
                    localPlaylistRepository.insertPlaylist(playlistInfo)

                    val remoteIds = remoteSongs.map { it.youtubeId }.toSet()
                    val currentSongs = localPlaylistRepository.getPlaylistById("liked_songs")?.songs ?: emptyList()

                    // Insert/update remote songs and add cross-refs
                    remoteSongs.forEach { song ->
                        localSongRepository.create(song)
                        localPlaylistRepository.insertCrossRef(
                            ca.ilianokokoro.umihi.music.models.PlaylistSongCrossRef("liked_songs", song.youtubeId)
                        )
                    }

                    // Remove local cross-refs that are no longer in remote list
                    currentSongs.forEach { localSong ->
                        if (localSong.youtubeId !in remoteIds) {
                            localPlaylistRepository.deleteCrossRef("liked_songs", localSong.youtubeId)
                        }
                    }
                }
            } catch (e: Exception) {
                UmihiHelper.printe("Failed to sync liked songs in background: ${e.message}")
            }
        }
    }
}
