import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SynthesizerTests {

    private val sampleRate = 1000

    class ConstantWaveStrategy(private val constantValue: Double) : WaveformStrategy {
        override fun generateSample(frequency: Double, time: Double): Double = constantValue
    }

    class MockSongLoadingStrategy(private val stubbedData: SongData) : SongLoadingStrategy {
        override fun load(source: String): SongData = stubbedData
    }

    @Test
    fun testEmptySongReturnsEmptyBuffer() {
        val emptyStrategy = MockSongLoadingStrategy(SongData(tempo = 120, channels = emptyList()))
        val synth = Synthesizer(sampleRate, emptyStrategy)

        synth.loadSong("any_dummy_path_string")
        val output = synth.generateMixedChannels()

        assertEquals(0, output.size)
    }

    @Test
    fun testMixingAndNormalizationAcrossMultipleChannels() {
        // Arrange
        val notes = listOf(Note("A4", 1.0))
        val channel1 = AudioChannel(notes, ConstantWaveStrategy(0.8))
        val channel2 = AudioChannel(notes, ConstantWaveStrategy(0.2))

        val stubbedSong = SongData(tempo = 120, channels = listOf(channel1, channel2))
        val mockStrategy = MockSongLoadingStrategy(stubbedSong)
        val synth = Synthesizer(sampleRate, mockStrategy)

        // Act
        synth.loadSong("mock_path")
        val output = synth.generateMixedChannels()

        // Assert
        assertEquals(500, output.size)
        for (sample in output) {
            assertEquals(0.5, sample, 0.0001)
        }
    }

    @Test
    fun testAsymmetricalTrackLengthsPaddingWithSilence() {
        // Arrange
        val shortChannel = AudioChannel(listOf(Note("A4", 1.0)), ConstantWaveStrategy(0.6))
        val longChannel = AudioChannel(listOf(Note("A4", 2.0)), ConstantWaveStrategy(0.4))

        val stubbedSong = SongData(tempo = 120, channels = listOf(shortChannel, longChannel))
        val mockStrategy = MockSongLoadingStrategy(stubbedSong)
        val synth = Synthesizer(sampleRate, mockStrategy)

        // Act
        synth.loadSong("mock_path")
        val output = synth.generateMixedChannels()

        // Assert
        assertEquals(1000, output.size)
        assertEquals(0.5, output[200], 0.0001)  // Both channels active
        assertEquals(0.2, output[750], 0.0001)  // Only long channel active
    }
}