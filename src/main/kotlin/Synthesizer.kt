class Synthesizer(
    private val sampleRate: Int,
    private val loadingStrategy: SongLoadingStrategy
) {
    private var tempo: Int = 120
    private var channels: List<AudioChannel> = emptyList()

    /**
     * Delegates file processing entirely to the injected strategy,
     * allowing the synthesizer to strictly focus on mixing.
     */
    fun loadSong(sourceIdentifier: String) {
        val songData = loadingStrategy.load(sourceIdentifier)
        this.tempo = songData.tempo
        this.channels = songData.channels
    }

    fun generateMixedChannels(): DoubleArray {
        if (channels.isEmpty()) return DoubleArray(0)

        val channelBuffers = channels.map { it.generateChannelSamples(sampleRate, tempo) }
        val maxSamples = channelBuffers.fold(0) { max, buffer ->
            if (buffer.size > max) buffer.size else max
        }
        val masterMixedBuffer = DoubleArray(maxSamples)

        for (i in 0 until maxSamples) {
            var sampleSum = 0.0
            for (buffer in channelBuffers) {
                if (i < buffer.size) {
                    sampleSum += buffer[i]
                }
            }
            masterMixedBuffer[i] = sampleSum / channels.size
        }

        return masterMixedBuffer
    }
}