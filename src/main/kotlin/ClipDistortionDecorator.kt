class ClipDistortionDecorator(
    decoratedStream: AudioStream,
    private val threshold: Double
) : EffectDecorator(decoratedStream) {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        val samples = super.getSamples(sampleRate, tempo)
        // Ensure the threshold parameter is positive
        val t = kotlin.math.abs(threshold)

        for (i in samples.indices) {
            samples[i] = samples[i].coerceIn(-t, t)
        }
        return samples
    }
}