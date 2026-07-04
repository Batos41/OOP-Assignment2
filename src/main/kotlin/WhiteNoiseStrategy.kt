class WhiteNoiseStrategy : WaveformStrategy {
    // Shared random instance for generating uniform noise values
    private val random = kotlin.random.Random

    override fun generateSample(frequency: Double, time: Double): Double {
        // White noise ignores the frequency/time components completely
        // and returns a random value between -1.0 and 1.0
        return random.nextDouble(-1.0, 1.0)
    }
}