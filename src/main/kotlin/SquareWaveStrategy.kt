class SquareWaveStrategy : WaveformStrategy {
    override fun generateSample(phase: Double): Double {
        // First half of the cycle is high, second half is low
        return if (phase < 0.5) 1.0 else -1.0
    }
}