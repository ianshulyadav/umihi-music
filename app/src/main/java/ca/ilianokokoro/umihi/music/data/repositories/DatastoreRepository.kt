package ca.ilianokokoro.umihi.music.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ca.ilianokokoro.umihi.music.core.Constants
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.COOKIES
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.DATA_SYNC_ID
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.KEEP_SCREEN_ON
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.SHOW_PODCAST_PLAYLIST
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.UPDATE_CHANNEL
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.USE_AUDIO_OFFLOAD
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.USE_SPECIAL_LANGUAGE
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.USE_ANIMATED_LYRICS
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.ANIMATED_LYRICS_BLUR_ENABLED
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.USE_IMMERSIVE_LYRICS
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.LYRICS_AUTOHIDE_DELAY
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.SHOW_PLAYER_FILE_INFO
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.PLAYER_THEME_PREFERENCE
import ca.ilianokokoro.umihi.music.data.repositories.DatastoreRepository.PreferenceKeys.COLOR_PALETTE_PREFERENCE
import ca.ilianokokoro.umihi.music.models.Cookies
import ca.ilianokokoro.umihi.music.models.UmihiSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.Datastore.NAME)

class DatastoreRepository(private val context: Context) {
    object PreferenceKeys {
        val COOKIES = stringPreferencesKey(Constants.Datastore.COOKIES_KEY)
        val DATA_SYNC_ID = stringPreferencesKey(Constants.Datastore.DATA_SYNC_ID)
        val UPDATE_CHANNEL = stringPreferencesKey(Constants.Datastore.UPDATE_CHANNEL_KEY)
        val SHOW_PODCAST_PLAYLIST = booleanPreferencesKey(Constants.Datastore.SHOW_PODCAST_PLAYLIST)
        val USE_SPECIAL_LANGUAGE = booleanPreferencesKey(Constants.Datastore.USE_SPECIAL_LANGUAGE)
        val USE_AUDIO_OFFLOAD = booleanPreferencesKey(Constants.Datastore.USE_AUDIO_OFFLOAD)
        val KEEP_SCREEN_ON = booleanPreferencesKey(Constants.Datastore.KEEP_SCREEN_ON)
        val USE_ANIMATED_LYRICS = booleanPreferencesKey(Constants.Datastore.USE_ANIMATED_LYRICS)
        val ANIMATED_LYRICS_BLUR_ENABLED = booleanPreferencesKey(Constants.Datastore.ANIMATED_LYRICS_BLUR_ENABLED)
        val USE_IMMERSIVE_LYRICS = booleanPreferencesKey("use_immersive_lyrics")
        val LYRICS_AUTOHIDE_DELAY = intPreferencesKey("lyrics_autohide_delay")
        val SHOW_PLAYER_FILE_INFO = booleanPreferencesKey("show_player_file_info")
        val PLAYER_THEME_PREFERENCE = stringPreferencesKey("player_theme_preference")
        val COLOR_PALETTE_PREFERENCE = stringPreferencesKey("color_palette_preference")
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit {
            it[key] = value
        }
    }

    val settings = context.dataStore.data.map {
        val updateChannel = it[UPDATE_CHANNEL]?.let { value -> UpdateChannel.valueOf(value) }
            ?: UpdateChannel.Stable
        val showPodcastPlaylist = it[SHOW_PODCAST_PLAYLIST] ?: true
        val useSpecialLanguage = it[USE_SPECIAL_LANGUAGE] ?: false
        val useAudioOffload = it[USE_AUDIO_OFFLOAD] ?: false
        val keepScreenOn = it[KEEP_SCREEN_ON] ?: false
        val useAnimatedLyrics = it[USE_ANIMATED_LYRICS] ?: true
        val animatedLyricsBlurEnabled = it[ANIMATED_LYRICS_BLUR_ENABLED] ?: true
        val useImmersiveLyrics = it[USE_IMMERSIVE_LYRICS] ?: true
        val lyricsAutoHideDelay = it[LYRICS_AUTOHIDE_DELAY] ?: 4
        val showPlayerFileInfo = it[SHOW_PLAYER_FILE_INFO] ?: false
        val playerThemePreference = it[PLAYER_THEME_PREFERENCE] ?: "ALBUM_ART"
        val colorPalettePreference = it[COLOR_PALETTE_PREFERENCE] ?: "SAGE"
        val cookies = cookies.first()
        val dataSyncId = dataSyncId.first()

        UmihiSettings(
            updateChannel = updateChannel,
            showPodcastPlaylist = showPodcastPlaylist,
            cookies = cookies,
            dataSyncId = dataSyncId,
            useSpecialLanguage = useSpecialLanguage,
            useAudioOffload = useAudioOffload,
            keepScreenOn = keepScreenOn,
            useAnimatedLyrics = useAnimatedLyrics,
            animatedLyricsBlurEnabled = animatedLyricsBlurEnabled,
            useImmersiveLyrics = useImmersiveLyrics,
            lyricsAutoHideDelay = lyricsAutoHideDelay,
            showPlayerFileInfo = showPlayerFileInfo,
            playerThemePreference = playerThemePreference,
            colorPalettePreference = colorPalettePreference
        )
    }

    fun getSettings(): UmihiSettings {
        return runBlocking {
            settings.first()
        }
    }

    val cookies = context.dataStore.data.map {
        Cookies(it[COOKIES] ?: "")
    }

    val dataSyncId = context.dataStore.data.map {
        it[DATA_SYNC_ID] ?: ""
    }

    suspend fun saveCookies(cookies: Cookies) {
        context.dataStore.edit {
            it[COOKIES] = cookies.toRawCookie()
        }
    }

    suspend fun saveDataSyncId(newId: String) {
        context.dataStore.edit {
            it[DATA_SYNC_ID] = newId
        }
    }


    enum class UpdateChannel {
        Stable,
        Beta
    }

}