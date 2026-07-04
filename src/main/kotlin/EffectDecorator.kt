abstract class EffectDecorator(
    protected val decoratedStream: AudioStream
) : AudioStream {

    // Default implementation passes the request right down to the inner stream
    override fun getSamples(sampleRate: Int, tempo: Int): DoubleArray {
        return decoratedStream.getSamples(sampleRate, tempo)
    }
}