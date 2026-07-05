import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.PI
import kotlin.math.sin

class WaveformStrategiesTests {

    private val epsilon = 0.00001

    @Test
    fun testSineWaveStrategy() {
        val strategy = SineWaveStrategy()

        // Verify known exact mathematical phase boundaries: sin(2 * PI * phase)
        assertEquals(0.0, strategy.generateSample(0.0), epsilon)       // sin(0)
        assertEquals(1.0, strategy.generateSample(0.25), epsilon)      // sin(π/2) -> Peak Positive
        assertEquals(0.0, strategy.generateSample(0.5), epsilon)       // sin(π)
        assertEquals(-1.0, strategy.generateSample(0.75), epsilon)     // sin(3π/2) -> Peak Negative

        // Continuous bounds check over the entire normalized cycle range [0.0, 1.0)
        var phase = 0.0
        while (phase < 1.0) {
            val sample = strategy.generateSample(phase)
            assertTrue(sample in -1.0..1.0, "Sine sample out of bounds at phase $phase: $sample")
            phase += 0.01
        }
    }

    @Test
    fun testSquareWaveStrategy() {
        val strategy = SquareWaveStrategy()

        // Verify the step-function behavior across normalized phase thresholds
        assertEquals(1.0, strategy.generateSample(0.0), epsilon)       // Boundary/zero crossover point
        assertEquals(1.0, strategy.generateSample(0.25), epsilon)      // First half of the duty cycle
        assertEquals(-1.0, strategy.generateSample(0.75), epsilon)     // Second half of the duty cycle

        // Strict value checking across the entire cycle
        var phase = 0.01 // Step slightly off the boundary to check pure states
        while (phase < 1.0) {
            val sample = strategy.generateSample(phase)
            assertTrue(sample == 1.0 || sample == -1.0 || sample == 0.0,
                "Square sample unexpected value at phase $phase: $sample")
            phase += 0.01
        }
    }

    @Test
    fun testSawWaveStrategy() {
        val strategy = SawWaveStrategy()

        // Ensure the sawtooth ramps predictably or respects exact boundaries at key checkpoints
        assertEquals(-1.0, strategy.generateSample(0.0), epsilon)

        // Continuous bounds testing across the full phase spectrum
        var phase = 0.0
        while (phase < 1.0) {
            val sample = strategy.generateSample(phase)
            assertTrue(sample in -1.0..1.0, "Sawtooth sample out of bounds at phase $phase: $sample")
            phase += 0.01
        }
    }

    @Test
    fun testWhiteNoiseStrategy() {
        val strategy = WhiteNoiseStrategy()

        // Generate a collection of samples across changing phase steps to evaluate randomness
        val samples = DoubleArray(1000) { i ->
            val simulatedPhase = i.toDouble() / 1000.0
            strategy.generateSample(simulatedPhase)
        }

        // 1. All samples must respect the digital ceiling/floor clipping boundaries
        for (sample in samples) {
            assertTrue(sample in -1.0..1.0, "White noise sample out of bounds: $sample")
        }

        // 2. White noise should be un-phased by the argument and generate distinct random values
        val distinctCount = samples.distinct().size
        assertTrue(distinctCount > 950, "White noise failed statistical randomness check")
    }
}