import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class VolumeDecoratorTests {

    private val sampleRate = 44100
    private val tempo = 120

    @Test
    fun testVolumeScaling() {
        // Arrange: A single simple note stream
        val notes = listOf(Note("A4", 1.0))
        val rawStream = RawWaveformStream(notes, SineWaveStrategy())

        // Wrap it in a volume decorator at 50% volume (0.5)
        val volumeStream = VolumeDecorator(rawStream, 0.5)

        // Act
        val rawSamples = rawStream.getSamples(sampleRate, tempo)
        val scaledSamples = volumeStream.getSamples(sampleRate, tempo)

        // Assert
        assertEquals(rawSamples.size, scaledSamples.size)

        // Every scaled sample should be exactly half of its raw equivalent
        for (i in rawSamples.indices) {
            assertEquals(rawSamples[i] * 0.5, scaledSamples[i], 0.00001)
        }
    }

    @Test
    fun testZeroVolumeSilence() {
        // Arrange: Wrap a stream and scale it to absolute zero volume
        val notes = listOf(Note("A4", 1.0))
        val rawStream = RawWaveformStream(notes, SineWaveStrategy())
        val silentStream = VolumeDecorator(rawStream, 0.0)

        // Act
        val samples = silentStream.getSamples(sampleRate, tempo)

        // Assert: Every single sample value must evaluate exactly to 0.0
        val nonZeroCount = samples.count { it != 0.0 }
        assertEquals(0, nonZeroCount, "Volume level 0.0 should render complete digital silence.")
    }
}