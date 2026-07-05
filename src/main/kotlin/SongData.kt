// A simple data holder to cleanly pass configuration back to the engine
data class SongData(
    val sampleRate: Int,
    val beatsPerMeasure: Int,
    val tempo: Int,
    val channels: List<AudioChannel>
)