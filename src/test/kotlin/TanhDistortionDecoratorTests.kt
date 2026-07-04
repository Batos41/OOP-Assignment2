import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TanhDistortionDecoratorTests {

    private val sampleRate = 44100
    private val tempo = 120

    @Test
    fun testTanhDistortionCompression() {
        val notes = listOf(Note("A4", 1.0))
        val rawStream = RawWaveformStream(notes, SineWaveStrategy())

        // Apply a heavy drive factor of 5.0
        val tanhStream = TanhDistortionDecorator(rawStream, 5.0)

        // Act
        val rawSamples = rawStream.getSamples(sampleRate, tempo)
        val distortedSamples = tanhStream.getSamples(sampleRate, tempo)

        // Assert
        assertEquals(rawSamples.size, distortedSamples.size)
        for (i in rawSamples.indices) {
            // Verify that everything stays safely normalized inside the digital ceiling
            assertTrue(distortedSamples[i] in -1.0001..1.0001, "Tanh sample out of bounds: ${distortedSamples[i]}")
        }
    }

    @Test
    fun testTanhDistortionWithZeroDrive() {
        // Arrange: A stream wrapped with a Tanh distortion where drive is exactly 0.0
        val notes = listOf(Note("A4", 0.5))
        val rawStream = RawWaveformStream(notes, SineWaveStrategy())
        val zeroDriveStream = TanhDistortionDecorator(rawStream, 0.0)

        // Act
        val rawSamples = rawStream.getSamples(sampleRate, tempo)
        val distortedSamples = zeroDriveStream.getSamples(sampleRate, tempo)

        // Assert: The output should exactly match the input with no modification
        assertArrayEquals(rawSamples, distortedSamples, 0.00001)
    }
}