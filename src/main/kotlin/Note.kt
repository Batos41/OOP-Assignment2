import kotlin.math.pow

class Note(
    private val pitch: String,
    private val durationInBeats: Double
) {

    /**
     * Calculates the frequency of the note in Hz.
     * Returns 0.0 if the note is a rest ("-").
     */
    fun getFrequency(): Double {
        if (pitch == "-") return 0.0

        // Parse the pitch components using a regex
        // Groups: 1 = Base Note (A-G), 2 = Accidental (#/b), 3 = Octave
        val regex = """([A-G])([#b]?)(-?\d+)""".toRegex()
        val matchResult = regex.matchEntire(pitch)
            ?: throw IllegalArgumentException("Invalid pitch format: $pitch")

        val (letter, accidental, octaveStr) = matchResult.destructured
        val octave = octaveStr.toInt()

        // Base MIDI offsets for C through B in octave 0
        val baseNoteOffsets = mapOf(
            "C" to 12, "D" to 14, "E" to 16, "F" to 17,
            "G" to 19, "A" to 21, "B" to 23
        )

        var midiNumber = baseNoteOffsets[letter]!! + (octave * 12)

        // Apply accidental adjustments
        when (accidental) {
            "#" -> midiNumber += 1
            "b" -> midiNumber -= 1
        }

        // Calculate standard frequency relative to A4 (MIDI 69)
        return 440.0 * 2.0.pow((midiNumber - 69) / 12.0)
    }

    /**
     * Converts the duration from beats to seconds based on the song tempo.
     * Formula: seconds = beats * (60.0 / tempo)
     */
    fun getDurationInSeconds(tempo: Int): Double {
        require(tempo > 0) { "Tempo must be greater than 0" }
        return durationInBeats * (60.0 / tempo)
    }
}