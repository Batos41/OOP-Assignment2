import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LiveAudioPlayer(
    private val sampleRate: Float = 44100f,
    private val bitDepth: Int = 16
) : AutoCloseable {

    private val line: SourceDataLine

    init {
        // 1. Define the audio format (16-bit signed PCM, Mono, Little-Endian)
        val format = AudioFormat(
            sampleRate,
            bitDepth,
            1,    // Channels (1 = Mono)
            true, // Signed
            false // Big-Endian (false = Little-Endian, standard for PC hardware)
        )

        // 2. Acquire and open the system audio output line
        line = AudioSystem.getSourceDataLine(format)
        // Using a 100ms internal hardware buffer to keep latency low but prevent stuttering
        val bufferSize = (sampleRate * format.frameSize * 0.1).toInt()
        line.open(format, bufferSize)
    }

    /**
     * Starts the hardware audio line. Call this before streaming samples.
     */
    fun start() {
        if (!line.isActive) {
            line.start()
        }
    }

    /**
     * Converts a stream of floating-point audio samples [-1.0, 1.0]
     * to 16-bit PCM bytes and writes them directly to the hardware buffer.
     */
    fun play(samples: DoubleArray) {
        val rawBytes = convertSamplesToPcmBytes(samples)
        line.write(rawBytes, 0, rawBytes.size)
    }

    /**
     * Blocks until the hardware finishes playing any remaining buffered samples.
     */
    fun drain() {
        line.drain()
    }

    override fun close() {
        line.stop()
        line.close()
    }

    /**
     * Internal helper to handle the DSP translation.
     * This is what we will target in our unit tests.
     */
    internal fun convertSamplesToPcmBytes(samples: DoubleArray): ByteArray {
        val byteBuffer = java.nio.ByteBuffer.allocate(samples.size * 2).apply {
            order(java.nio.ByteOrder.LITTLE_ENDIAN)
        }

        for (sample in samples) {
            val clamped = sample.coerceIn(-1.0, 1.0)
            val pcmSample = (clamped * Short.MAX_VALUE).toInt().toShort()
            byteBuffer.putShort(pcmSample)
        }

        return byteBuffer.array()
    }
}