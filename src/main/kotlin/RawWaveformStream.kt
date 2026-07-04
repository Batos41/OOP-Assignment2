class RawWaveformStream(
    private val notes: List<Note>,
    private val waveformStrategy: WaveformStrategy
) : AudioStream {

    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }
        require(tempo > 0) { "Tempo must be greater than 0" }

        // 1. Calculate the total number of samples needed for the entire stream
        val totalDurationInSeconds = notes.sumOf { it.getDurationInSeconds(tempo) }
        val totalSamples = (totalDurationInSeconds * sampleRate).toInt()
        val audioData = DoubleArray(totalSamples)

        // 2. Pre-calculate the starting time boundary for every note
        // This avoids re-running loops/accumulating notes over and over inside the audio loop
        val noteStartTimes = DoubleArray(notes.size)
        var accumulatedTime = 0.0
        for (i in notes.indices) {
            noteStartTimes[i] = accumulatedTime
            accumulatedTime += notes[i].getDurationInSeconds(tempo)
        }

        // 3. Populate the audio sample array
        var currentNoteIndex = 0
        for (sampleIndex in 0 until totalSamples) {
            val currentTimeInSeconds = sampleIndex.toDouble() / sampleRate

            // Advance the active note pointer if the current time exceeds the next note's starting threshold
            while (currentNoteIndex + 1 < notes.size &&
                currentTimeInSeconds >= noteStartTimes[currentNoteIndex + 1]) {
                currentNoteIndex++
            }

            // Extract the active note and its properties
            val activeNote = notes[currentNoteIndex]
            val frequency = activeNote.getFrequency()

            // Generate the sample value using our interchangeable strategy pattern
            // Rests (0.0 Hz) naturally return 0.0 across all mathematical strategies
            audioData[sampleIndex] = if (frequency > 0.0) {
                waveformStrategy.generateSample(frequency, currentTimeInSeconds)
            } else {
                0.0
            }
        }

        return audioData
    }
}