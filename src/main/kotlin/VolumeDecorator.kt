class VolumeDecorator(
    decoratedStream: AudioStream,
    private val level: Double
) : EffectDecorator(decoratedStream) {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        // 1. Fetch the raw or previously modified audio array from below us in the stack
        val samples = super.getSamples(sampleRate, tempo)

        // 2. Linearly scale the amplitude of every single sample by the volume level
        for (i in samples.indices) {
            samples[i] *= level
        }

        return samples
    }
}