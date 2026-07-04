import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LiveAudioPlayerTests {

    @Test
    fun testSampleToPcmConversionMath() {
        val player = LiveAudioPlayer()

        // 0.0 should map exactly to 0
        // 1.0 should map exactly to Short.MAX_VALUE (32767)
        // -1.0 should map exactly to -Short.MAX_VALUE (-32767)
        val inputSamples = doubleArrayOf(0.0, 1.0, -1.0)

        val pcmBytes = player.convertSamplesToPcmBytes(inputSamples)

        // 3 samples * 2 bytes per sample = 6 bytes total
        assertEquals(6, pcmBytes.size)

        // Read the bytes back using a verified ByteBuffer to validate the values
        val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(0.toShort(), buffer.short)
        assertEquals(32767.toShort(), buffer.short)
        assertEquals((-32767).toShort(), buffer.short)
    }

    @Test
    fun testAudioClampingPreventsOverflow() {
        val player = LiveAudioPlayer()

        // Values that trigger your safety check by exceeding the normal bounds
        val extremeSamples = doubleArrayOf(1.5, -2.0)

        val pcmBytes = player.convertSamplesToPcmBytes(extremeSamples)
        val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(32767.toShort(), buffer.short)   // Clamps to positive max
        assertEquals((-32767).toShort(), buffer.short) // Clamps to symmetric negative max
    }
}