class ADSDecorator(
    decoratedStream: AudioStream,
    private val attackEnd: Double,
    private val decayEnd: Double,
    private val sustain: Double,
    private val noteDurations: List<Double>
) : EffectDecorator(decoratedStream) {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        val samples = super.getSamples(sampleRate, tempo)

        // Convert the note durations from beats into exact sample counts
        val secondsPerBeat = 60.0 / tempo
        val noteSampleLengths = noteDurations.map { beats ->
            (beats * secondsPerBeat * sampleRate).toInt()
        }

        var currentNoteIndex = 0
        var samplesProcessedInCurrentNote = 0

        for (i in samples.indices) {
            // If we've passed the length of the current note, move to the next note and reset the timer
            if (currentNoteIndex < noteSampleLengths.size &&
                samplesProcessedInCurrentNote >= noteSampleLengths[currentNoteIndex]) {

                samplesProcessedInCurrentNote = 0
                currentNoteIndex++
            }

            // Calculate time relative to the current note's exact start boundary
            val currentTime = samplesProcessedInCurrentNote.toDouble() / sampleRate

            val factor = when {
                // 1. Attack Phase (Guaranteed attackEnd > 0.0 if this branch matches)
                attackEnd > 0.0 && currentTime < attackEnd -> {
                    currentTime / attackEnd
                }
                // 2. Decay Phase (Guaranteed decayEnd > attackEnd if this branch matches)
                decayEnd > attackEnd && currentTime < decayEnd -> {
                    val decayDuration = decayEnd - attackEnd
                    1.0 - ((currentTime - attackEnd) / decayDuration) * (1.0 - sustain)
                }
                // 3. Sustain Phase (Handles when currentTime >= decayEnd OR when phases are skipped)
                else -> sustain
            }

            samples[i] *= factor
            samplesProcessedInCurrentNote++
        }

        return samples
    }
}