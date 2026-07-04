class SineWaveStrategy : WaveformStrategy {
    override fun generateSample(frequency: Double, time: Double): Double {
        return kotlin.math.sin(2.0 * kotlin.math.PI * frequency * time)
    }
}