class AudioChannel(
    notes: List<Note>,
    waveformStrategy: WaveformStrategy,
    effects: List<(AudioStream) -> AudioStream> = emptyList()
) {
    private val stream: AudioStream

    init {
        // Build the single pipeline
        var pipeline: AudioStream = RawWaveformStream(notes, waveformStrategy)
        for (effectFactory in effects) {
            pipeline = effectFactory(pipeline)
        }
        this.stream = pipeline
    }

    /**
     * Composes the internal streaming pipeline and generates the final,
     * fully processed array of double precision audio samples.
     */
    fun generateChannelSamples(sampleRate: Int, tempo: Int): DoubleArray {
        // Simply pull from the single pipeline property
        return stream.getSamples(sampleRate, tempo)
    }
}