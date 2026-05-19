package ca.ilianokokoro.umihi.music.ui.screens.player.lyrics

object LyricsParser {
    private val LRC_LINE_REGEX = Regex("""^\[(\d{2}):(\d{2})[.:](\d{2,3})\](.*)$""")

    fun parse(lrcText: String?): Lyrics {
        if (lrcText.isNullOrEmpty()) return Lyrics()

        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        var hasTimestamps = false

        lrcText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val match = LRC_LINE_REGEX.find(trimmed)
            if (match != null) {
                hasTimestamps = true
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val fraction = match.groupValues[3].toLong()
                val text = match.groupValues[4].trim()

                val mult = if (match.groupValues[3].length == 2) 10 else 1
                val timeMs = (min * 60 * 1000 + sec * 1000 + fraction * mult).toInt()
                syncedLines.add(SyncedLine(time = timeMs, line = text))
            } else {
                // Strip bracket annotations/metadata or extract plain line
                val cleanLine = trimmed.replace(Regex("""^\[.*\]"""), "").trim()
                if (cleanLine.isNotEmpty()) {
                    plainLines.add(cleanLine)
                }
            }
        }

        return if (hasTimestamps && syncedLines.isNotEmpty()) {
            val sorted = syncedLines.sortedBy { it.time }
            val paired = mutableListOf<SyncedLine>()
            var i = 0
            while (i < sorted.size) {
                val current = sorted[i]
                val next = sorted.getOrNull(i + 1)
                if (next != null && next.time == current.time) {
                    paired.add(current.copy(translation = next.line))
                    i += 2
                } else {
                    paired.add(current)
                    i++
                }
            }
            Lyrics(plain = paired.map { it.line }, synced = paired, areFromRemote = true)
        } else {
            Lyrics(plain = plainLines, synced = emptyList(), areFromRemote = true)
        }
    }
}
