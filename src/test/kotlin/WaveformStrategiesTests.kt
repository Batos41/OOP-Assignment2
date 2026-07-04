import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.PI
import kotlin.math.sin

class WaveformStrategiesTests {

    private val epsilon = 0.00001
    private val testFreq = 440.0 // A4 tuning reference
    private val testTime = 0.001 // 1 millisecond into playback

    @Test
    fun testSineWaveStrategy() {
        val strategy = SineWaveStrategy()

        // Exact mathematical expected value: sin(2 * PI * 440.0 * 0.001)
        val expected = sin(2.0 * PI * testFreq * testTime)
        val actual = strategy.generateSample(testFreq, testTime)

        assertEquals(expected, actual, epsilon)

        // Bounds check over a series of points
        for (i in 0..100) {
            val sample = strategy.generateSample(testFreq, i * 0.0001)
            assertTrue(sample in -1.0..1.0, "Sine sample out of bounds: $sample")
        }
    }

    @Test
    fun testSquareWaveStrategy() {
        val strategy = SquareWaveStrategy()

        // At t = 0.0, sin(0) is 0.0 -> should yield 0.0
        assertEquals(0.0, strategy.generateSample(testFreq, 0.0), epsilon)

        // Test a point known to be in the positive phase of a 440Hz wave
        // T = 1/440 = ~0.00227s. 1/4 of that period is ~0.00056s (peak positive phase)
        val positiveSample = strategy.generateSample(testFreq, 0.0005)
        assertEquals(1.0, positiveSample, epsilon)

        // 3/4 of that period is ~0.0017s (peak negative phase)
        val negativeSample = strategy.generateSample(testFreq, 0.0017)
        assertEquals(-1.0, negativeSample, epsilon)

        // Strict bounds checking
        for (i in 0..100) {
            val sample = strategy.generateSample(testFreq, i * 0.0001)
            assertTrue(sample == 1.0 || sample == -1.0 || sample == 0.0, "Square sample unexpected value: $sample")
        }
    }

    @Test
    fun testSawWaveStrategy() {
        val strategy = SawWaveStrategy()

        // At t = 0.0, phase is 0. 2 * (0 - floor(0.5)) -> 2 * (0 - 0) = 0.0
        assertEquals(0.0, strategy.generateSample(testFreq, 0.0), epsilon)

        // Continuous bounds testing across two full cycles
        val period = 1.0 / testFreq
        var time = 0.0
        while (time < period * 2) {
            val sample = strategy.generateSample(testFreq, time)
            assertTrue(sample in -1.0..1.0, "Sawtooth sample out of bounds: $sample")
            time += 0.0001
        }
    }

    @Test
    fun testWhiteNoiseStrategy() {
        val strategy = WhiteNoiseStrategy()

        // Generate many samples to ensure randomness and strict boundaries
        val samples = DoubleArray(1000) { i ->
            strategy.generateSample(testFreq, i * 0.001)
        }

        // 1. All samples must respect the digital ceiling/floor bounds
        for (sample in samples) {
            assertTrue(sample in -1.0..1.0, "White noise sample out of bounds: $sample")
        }

        // 2. White noise shouldn't output identical constant values
        val distinctCount = samples.distinct().size
        assertTrue(distinctCount > 950, "White noise failed statistical randomness check")
    }
}