class SineWaveStrategy : WaveformStrategy {
    override fun generateSample(phase: Double): Double {
        return kotlin.math.sin(2.0 * kotlin.math.PI * phase)
    }
}