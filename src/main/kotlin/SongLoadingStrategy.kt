interface SongLoadingStrategy {
    /**
     * Reads a source target and parses out the structured channels
     * and song-wide configuration parameters.
     */
    fun load(source: String): SongData
}