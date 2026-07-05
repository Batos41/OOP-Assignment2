interface WaveformStrategy {
    /**
     * Generates a single audio sample value between -1.0 and 1.0
     * based on the given frequency (Hz) and current time (seconds).
     */
    fun generateSample(phase: Double): Double
}