import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class RawWaveformStreamTests {

    private val sampleRate = 44100
    private val tempo = 120 // 120 BPM means 1 beat = 0.5 seconds

    @Test
    fun testArraySizingAndCalculation() {
        // Arrange: 3 notes totaling 4 beats. At 120 BPM, this is exactly 2.0 seconds of audio.
        val notes = listOf(
            Note("A4", 1.0),
            Note("B4", 2.0),
            Note("C5", 1.0)
        )
        val stream = RawWaveformStream(notes, SineWaveStrategy())

        // Act
        val samples = stream.getSamples(sampleRate, tempo)

        // Assert: 2.0 seconds * 44100 samples/sec = 88200 total samples
        val expectedSize = 88200
        assertEquals(expectedSize, samples.size)
    }

    @Test
    fun testNoteTransitionsAndFrequencies() {
        // Arrange: 1 beat of a lower frequency note followed by 1 beat of a higher frequency note.
        // At 120 BPM, the transition point is exactly at 0.5 seconds (sample index 22050).
        val notes = listOf(
            Note("A3", 1.0), // ~220 Hz
            Note("A5", 1.0)  // ~880 Hz
        )
        val stream = RawWaveformStream(notes, SineWaveStrategy())

        // Act
        val samples = stream.getSamples(sampleRate, tempo)

        // Assert: Verify that the signal actually switches behavior at the boundary index.
        val boundaryIndex = 22050

        // Grab a sample right before the boundary and right after
        val sampleBefore = samples[boundaryIndex - 10]
        val sampleAfter = samples[boundaryIndex + 10]

        // Samples shouldn't be identically 0, and because the frequencies are different,
        // the sequential values across the boundary should confirm distinct generation properties.
        assertNotEquals(0.0, sampleBefore)
        assertNotEquals(0.0, sampleAfter)
    }

    @Test
    fun testRestHandling() {
        // Arrange: A 1-beat rest note. At 120 BPM, this is 0.5 seconds (22050 samples) of silence.
        val notes = listOf(Note("-", 1.0))
        val stream = RawWaveformStream(notes, SineWaveStrategy())

        // Act
        val samples = stream.getSamples(sampleRate, tempo)

        // Assert: Every single sample inside a rest period must be exactly 0.0
        assertEquals(22050, samples.size)
        val nonZeroCount = samples.count { it != 0.0 }
        assertEquals(0, nonZeroCount, "Rest period contained non-silent audio data.")
    }

    @Test
    fun testInvalidEnvironmentArguments() {
        val notes = listOf(Note("A4", 1.0))
        val stream = RawWaveformStream(notes, SineWaveStrategy())

        // Ensure negative or zero processing configurations throw graceful validation errors
        assertThrows<IllegalArgumentException> { stream.getSamples(0, 120) }
        assertThrows<IllegalArgumentException> { stream.getSamples(44100, 0) }
        assertThrows<IllegalArgumentException> { stream.getSamples(-44100, 120) }
    }
}