class ADSDecorator(
    decoratedStream: AudioStream,
    private val attackEnd: Double,
    private val decayEnd: Double,
    private val sustain: Double
) : EffectDecorator(decoratedStream) {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        val samples = super.getSamples(sampleRate, tempo)

        for (i in samples.indices) {
            val currentTime = i.toDouble() / sampleRate

            val factor = when {
                // 1. Attack Phase (Guaranteed attackEnd > 0.0 if this branch matches)
                attackEnd > 0.0 && currentTime < attackEnd -> {
                    currentTime / attackEnd
                }
                // 2. Decay Phase (Guaranteed decayEnd > attackEnd if this branch matches)
                decayEnd > attackEnd && currentTime < decayEnd -> {
                    val decayDuration = decayEnd - attackEnd
                    1.0 - ((currentTime - attackEnd) / decayDuration) * (1.0 - sustain)
                }
                // 3. Sustain Phase (Handles when currentTime >= decayEnd OR when phases are skipped)
                else -> sustain
            }

            samples[i] *= factor
        }

        return samples
    }
}