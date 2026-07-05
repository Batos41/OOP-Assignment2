class ADSDecorator(
    decoratedStream: AudioStream,
    private val attackEnd: Double,
    private val decayEnd: Double,
    private val sustain: Double,
    private val noteDurationsBeats: List<Double>,
    private val isNoteList: List<Boolean> // True for active pitch notes, False for rests ("-")
) : EffectDecorator(decoratedStream) {

    init {
        // Validation: Fail early if configurations violate physical constraints
        require(attackEnd >= 0.0) { "Attack end time cannot be negative: $attackEnd" }
        require(decayEnd >= attackEnd) { "Decay end time ($decayEnd) cannot be earlier than attack end ($attackEnd)" }
        require(sustain in 0.0..1.0) { "Sustain level must be between 0.0 and 1.0: $sustain" }
    }

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        val samples = super.getSamples(sampleRate, tempo)

        // Convert the note durations from beats into exact sample counts
        val secondsPerBeat = 60.0 / tempo
        val noteSampleLengths = noteDurationsBeats.map { beats ->
            (beats * secondsPerBeat * sampleRate).toInt()
        }

        var currentNoteIndex = 0
        var samplesProcessedInCurrentSegment = 0

        for (i in samples.indices) {
            // Guard/Advance block across note boundaries
            if (currentNoteIndex < noteSampleLengths.size &&
                samplesProcessedInCurrentSegment >= noteSampleLengths[currentNoteIndex]) {
                samplesProcessedInCurrentSegment = 0
                currentNoteIndex++
            }

            // Verify if the current segment index is an active sound or a rest block
            val isCurrentSegmentAnActiveNote = currentNoteIndex < isNoteList.size && isNoteList[currentNoteIndex]

            if (isCurrentSegmentAnActiveNote) {
                // Apply standard envelope math relative strictly to the start of this NOTE
                val currentTime = samplesProcessedInCurrentSegment.toDouble() / sampleRate

                val factor = when {
                    attackEnd > 0.0 && currentTime < attackEnd -> {
                        currentTime / attackEnd
                    }
                    decayEnd > attackEnd && currentTime < decayEnd -> {
                        val decayDuration = decayEnd - attackEnd
                        1.0 - ((currentTime - attackEnd) / decayDuration) * (1.0 - sustain)
                    }
                    else -> sustain
                }
                samples[i] *= factor
            } else {
                // It's a rest! Do not apply or advance an envelope calculation factor.
                // The underlying stream is already silent, or we preserve raw track signal untouched.
            }

            samplesProcessedInCurrentSegment++
        }

        return samples
    }
}