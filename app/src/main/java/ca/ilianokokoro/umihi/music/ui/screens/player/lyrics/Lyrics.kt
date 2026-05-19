package ca.ilianokokoro.umihi.music.ui.screens.player.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val plain: List<String> = emptyList(),
    val synced: List<SyncedLine> = emptyList(),
    val areFromRemote: Boolean = false
)

@Serializable
data class SyncedLine(
    val time: Int,
    val line: String,
    val translation: String? = null,
    val romanization: String? = null
)
