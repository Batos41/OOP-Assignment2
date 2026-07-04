interface SongLoadingStrategy {
    /**
     * Reads a source target and parses out the structured channels
     * and song-wide configuration parameters.
     */
    fun load(source: String): SongData
}

// A simple data holder to cleanly pass configuration back to the engine
data class SongData(
    val tempo: Int,
    val channels: List<AudioChannel>
)