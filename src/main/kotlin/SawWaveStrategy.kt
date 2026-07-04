class SawWaveStrategy : WaveformStrategy {
    override fun generateSample(frequency: Double, time: Double): Double {
        // Standard sawtooth calculation shifting values between -1.0 and 1.0
        // Formula: 2 * (t * f - floor(0.5 + t * f))
        val tf = time * frequency
        return 2.0 * (tf - kotlin.math.floor(0.5 + tf))
    }
}