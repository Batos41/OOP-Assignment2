class SquareWaveStrategy : WaveformStrategy {
    override fun generateSample(frequency: Double, time: Double): Double {
        // A square wave is just the sign (+1 or -1) of a sine wave
        val value = kotlin.math.sin(2.0 * kotlin.math.PI * frequency * time)
        return when {
            value > 0 -> 1.0
            value < 0 -> -1.0
            else -> 0.0
        }
    }
}