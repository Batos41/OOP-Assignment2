class AudioChannel(
    val notes: List<Note>,
    val waveformStrategy: WaveformStrategy,
    private val effects: List<(AudioStream) -> AudioStream> = emptyList()
) {
    /**
     * Composes the internal streaming pipeline and generates the final,
     * fully processed array of double precision audio samples.
     */
    fun generateChannelSamples(sampleRate: Int, tempo: Int): DoubleArray {
        // 1. Instantiate the foundational raw generator stream
        var pipeline: AudioStream = RawWaveformStream(notes, waveformStrategy)

        // 2. Wrap the stream sequentially with each configured decorator modifier
        for (effectFactory in effects) {
            pipeline = effectFactory(pipeline)
        }

        // 3. Evaluate the fully decorated pipeline to output processed data
        return pipeline.getSamples(sampleRate, tempo)
    }
}