import kotlin.math.tanh

class TanhDistortionDecorator(
    decoratedStream: AudioStream,
    private val drive: Double
) : EffectDecorator(decoratedStream) {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        val samples = super.getSamples(sampleRate, tempo)

        for (i in samples.indices) {
            // Formula: tanh(sample * drive) / tanh(drive)
            // Dividing by tanh(drive) normalizes the maximum possible output back to roughly 1.0
            if (drive != 0.0) {
                samples[i] = tanh(samples[i] * drive) / tanh(drive)
            }
        }
        return samples
    }
}