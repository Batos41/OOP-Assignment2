import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class NoteTests {
    private val epsilon = 0.01 // Floating-point tolerance for frequency checks

    @Test
    fun testTypicalPitches() {
        // Standard tuning reference: A4 = 440 Hz
        val a4 = Note("A4", 1.0)
        assertEquals(440.0, a4.getFrequency(), epsilon)

        // Middle C: C4 = 261.63 Hz
        val c4 = Note("C4", 1.0)
        assertEquals(261.63, c4.getFrequency(), epsilon)

        // Lower pitch: Empty bass string E2 = 82.41 Hz
        val e2 = Note("E2", 1.0)
        assertEquals(82.41, e2.getFrequency(), epsilon)
    }

    @Test
    fun testAccidentals() {
        // Sharps: C#4 = 277.18 Hz
        val cSharp4 = Note("C#4", 1.0)
        assertEquals(277.18, cSharp4.getFrequency(), epsilon)

        // Flats: Bb4 = 466.16 Hz
        val bFlat4 = Note("Bb4", 1.0)
        assertEquals(466.16, bFlat4.getFrequency(), epsilon)
    }

    @Test
    fun testRestNote() {
        // Rests should yield exactly 0 Hz
        val rest = Note("-", 2.0)
        assertEquals(0.0, rest.getFrequency(), 0.0)
    }

    @Test
    fun testBoundsChecking() {
        // Lower physical boundary: C0 (Deep sub-bass) = 16.35 Hz
        val c0 = Note("C0", 1.0)
        assertEquals(16.35, c0.getFrequency(), epsilon)

        // Upper physical boundary: B8 (Highest piano key) = 7902.13 Hz
        val b8 = Note("B8", 1.0)
        assertEquals(7902.13, b8.getFrequency(), epsilon)

        // Edge Case: Negative octave index (Valid SPN syntax representation)
        val cNeg1 = Note("C-1", 1.0)
        assertEquals(8.18, cNeg1.getFrequency(), epsilon)
    }

    @Test
    fun testInvalidPitchFormats() {
        // Malformed inputs should fail gracefully via exceptions
        assertThrows<IllegalArgumentException> { Note("H4", 1.0).getFrequency() }
        assertThrows<IllegalArgumentException> { Note("A", 1.0).getFrequency() }
        assertThrows<IllegalArgumentException> { Note("C#", 1.0).getFrequency() }
        assertThrows<IllegalArgumentException> { Note("456", 1.0).getFrequency() }
    }

    @Test
    fun testDurationInSeconds() {
        // Typical calculation: 2 beats at 120 BPM = 1.0 second
        val note1 = Note("A4", 2.0)
        assertEquals(1.0, note1.getDurationInSeconds(120), epsilon)

        // Fractional beats: 0.5 beats at 280 BPM = ~0.107 seconds
        val note2 = Note("C4", 0.5)
        assertEquals(0.1071, note2.getDurationInSeconds(280), epsilon)
    }

    @Test
    fun testDurationBoundsAndErrors() {
        val note = Note("A4", 4.0)

        // Invalid tempo bounds check
        assertThrows<IllegalArgumentException> { note.getDurationInSeconds(0) }
        assertThrows<IllegalArgumentException> { note.getDurationInSeconds(-10) }
    }
}