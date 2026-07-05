class SawWaveStrategy : WaveformStrategy {
    override fun generateSample(phase: Double): Double {
        // Maps 0.0...1.0 linearly to -1.0...1.0
        return 2.0 * phase - 1.0
    }
}