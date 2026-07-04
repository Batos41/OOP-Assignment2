import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AudioChannelTests {

    private val sampleRate = 1000
    private val tempo = 120

    @Test
    fun testPipelineWithNoEffects() {
        val notes = listOf(Note("A4", 1.0))
        val channel = AudioChannel(notes, SineWaveStrategy(), emptyList())

        val rawStream = RawWaveformStream(notes, SineWaveStrategy())

        val expectedSamples = rawStream.getSamples(sampleRate, tempo)
        val actualSamples = channel.generateChannelSamples(sampleRate, tempo)

        assertArrayEquals(expectedSamples, actualSamples, 0.00001)
    }

    @Test
    fun testPipelineAppliesEffectsInOrder() {
        val notes = listOf(Note("A4", 1.0))

        // Arrange a channel that applies a volume scale followed by hard-clipping
        val channel = AudioChannel(
            notes = notes,
            waveformStrategy = SawWaveStrategy(),
            effects = listOf(
                { stream -> VolumeDecorator(stream, 2.0) },      // Amplify wave past 1.0
                { stream -> ClipDistortionDecorator(stream, 0.5) } // Hard clip back down to 0.5
            )
        )

        val samples = channel.generateChannelSamples(sampleRate, tempo)

        // Assert: The clip distortion should have forced every amplified peak strictly to 0.5
        for (sample in samples) {
            assertTrue(sample in -0.50001..0.50001, "Sample broke through the stacked clip ceiling: $sample")
        }
    }
}