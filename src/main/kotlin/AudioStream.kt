fun interface AudioStream {
    /**
     * Generates and returns a complete array of audio samples
     * for a given sample rate and tempo environment.
     */
    fun getSamples(sampleRate: Int, tempo: Int): DoubleArray
}