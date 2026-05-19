package ca.ilianokokoro.umihi.music.ui.screens.player.lyrics

object LyricsParser {
    private val LRC_LINE_REGEX = Regex("""^\[(\d{2}):(\d{2})[.:](\d{2,3})\](.*)$""")

    fun parse(lrcText: String?): Lyrics {
        return LyricsUtils.parseLyrics(lrcText)
    }
}

