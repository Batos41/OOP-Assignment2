class Synthesizer(
    private val loadingStrategy: SongLoadingStrategy
) {
    var sampleRate: Int = 44100
    private set
    private var tempo: Int = 120
    private var channels: List<AudioChannel> = emptyList()

    /**
     * Delegates file processing entirely to the injected strategy,
     * allowing the synthesizer to strictly focus on mixing.
     */
    fun loadSong(sourceIdentifier: String) {
        val songData = loadingStrategy.load(sourceIdentifier)
        this.sampleRate = songData.sampleRate
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
            var activeChannelsCount = 0 // Track how many channels are contributing sound at index i

            for (buffer in channelBuffers) {
                if (i < buffer.size) {
                    sampleSum += buffer[i]
                    activeChannelsCount++ // This channel is active at this sample index!
                }
            }

            // Avoid division by zero if all tracks have ended but we are clearing trailing frames
            masterMixedBuffer[i] = if (activeChannelsCount > 0) {
                sampleSum / activeChannelsCount
            } else {
                0.0
            }
        }

        return masterMixedBuffer
    }
}