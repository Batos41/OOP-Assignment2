import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ClipDistortionDecoratorTests {

    private val sampleRate = 44100
    private val tempo = 120

    @Test
    fun testClipDistortionThreshold() {
        // Arrange: A mock/simple note that creates a high amplitude or standard wave
        val notes = listOf(Note("A4", 1.0))
        val rawStream = RawWaveformStream(notes, SawWaveStrategy()) // Saw reaches up to 1.0

        // Clip at 0.5 threshold
        val clippedStream = ClipDistortionDecorator(rawStream, 0.5)

        // Act
        val samples = clippedStream.getSamples(sampleRate, tempo)

        // Assert: No sample should break past the absolute ceiling of 0.5
        for (sample in samples) {
            assertTrue(sample in -0.5..0.5, "Sample exceeded hard clip boundary: $sample")
        }
    }

}
