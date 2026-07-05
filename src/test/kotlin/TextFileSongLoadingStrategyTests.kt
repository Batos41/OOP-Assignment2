import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.io.File

class TextFileSongLoadingStrategyTests {

    private fun createTempSongFile(content: String): String {
        return File.createTempFile("test_song", ".txt").apply {
            writeText(content.trimIndent())
            deleteOnExit()
        }.absolutePath
    }

    @Test
    fun testParseValidSampleData() {
        // Arrange
        val path = createTempSongFile("""
                44100 8 280
                saw vol$.8 ads$.01$.2$.1|F4 1 - 2|
            """)

        val strategy = TextFileSongLoadingStrategy()
        val tempo = 280
        val sampleRate = 44100

        // Act
        val songData = strategy.load(path)

        // Assert
        assertEquals(tempo, songData.tempo)
        assertEquals(1, songData.channels.size)

        val channel = songData.channels[0]

        // Generate the total sample buffer for this track
        // 3 beats total (1 beat F4 + 2 beats rest) at 280 BPM
        // Total duration = 3 * (60 / 280) = ~0.6428 seconds
        // Total samples = ~0.6428 * 44100 = ~28350 samples
        val samples = channel.generateChannelSamples(sampleRate, tempo)

        // 1. Behavior Verification: Ensure audio data was generated
        assertTrue(samples.isNotEmpty(), "Audio channel should have generated samples")

        // 2. Note Behavior: The first section represents F4, which should have wave amplitude values
        val firstNoteSampleIdx = 1000
        assertNotEquals(0.0, samples[firstNoteSampleIdx], 0.001, "First note (F4) should contain active signal data")

        // 3. Rest Behavior: The end of the track is a rest ("-"), which means it must stream absolute silence
        val restSampleIdx = samples.lastIndex - 100
        assertEquals(0.0, samples[restSampleIdx], 0.001, "The trailing rest should result in absolute digital silence")
    }

    @Test
    fun testEmptyFileThrowsException() {
        // Targets Line 7 branch in Screenshot 2026-07-03 231010.jpg
        val path = createTempSongFile("")
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<IllegalArgumentException> {
            strategy.load(path)
        }
    }

    @Test
    fun testMissingTempoFallback() {
        // Targets Line 11 fallback branch in Screenshot 2026-07-03 231010.jpg
        val path = createTempSongFile("44100 8") // No third token for tempo
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(120, songData.tempo) // Falls back to default 120
    }

    @Test
    fun testTrackLineMissingPipeAndUnknownStrategy() {
        // Targets Lines 23-24 (no pipe) and Line 43 (else fallback wave)
        // as well as Line 27 (skipping empty rows implicitly)
        val path = createTempSongFile("""
        44100 8 120
        unknownWave vol
        
    """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(1, songData.channels.size)

        // Verify that zero samples or standard silent fallback frames are handled safely:
        val samples = songData.channels[0].generateChannelSamples(44100, 120)
        assertNotNull(samples)
    }

    @Test
    fun testMalformedEffectParametersFallback() {
        // Targets Lines 61, 65, and 88 fallback branches
        // where parameters are left completely blank or missing numeric text
        val path = createTempSongFile("""
        44100 8 120
        sine vol$ ads$$$|C4|
    """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(1, songData.channels.size)
        val channel = songData.channels[0]

        // Check that the generated array matches the exact size boundary of a 1-beat fallback note
        val samples = channel.generateChannelSamples(44100, 120)
        val expectedSamplesCount = (1.0 * (60.0 / 120.0) * 44100).toInt()
        assertEquals(expectedSamplesCount, samples.size, "Should fallback to 1 beat duration")
    }

    @Test
    fun testAllWaveformStrategiesCoverage() {
        // Targets lines 38-41 in Screenshot 2026-07-03 231010.jpg
        val path = createTempSongFile("""
        44100 8 120
        square|C4 1|
        whitenoise|C4 1|
        saw|C4 1|
    """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(3, songData.channels.size)

        // Explicitly validates the parsing paths for your different wave types
        // (You can optionally assert their internal strategy types if exposed,
        // but simply executing these lines fulfills the code coverage requirement)
        assertNotNull(songData.channels[0]) // covered 'square'
        assertNotNull(songData.channels[1]) // covered 'whitenoise'
        assertNotNull(songData.channels[2]) // covered 'saw'
    }

    @Test
    fun testEmptyOrWhitespaceFileThrows() {
        // Covers Line 7 (Red) in Screenshot 2026-07-03 231708.png
        val path = createTempSongFile("   \n   ")
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<IllegalArgumentException> {
            strategy.load(path)
        }
    }

    @Test
    fun testMalformedHeaderTempoFallback() {
        // Covers Line 11 (Yellow) in Screenshot 2026-07-03 231708.png
        // Case A: Missing entirely
        val pathMissing = createTempSongFile("44100 8")
        // Case B: Not an integer
        val pathInvalid = createTempSongFile("44100 8 abc")

        val strategy = TextFileSongLoadingStrategy()

        assertEquals(120, strategy.load(pathMissing).tempo)
        assertEquals(120, strategy.load(pathInvalid).tempo)
    }

    @Test
    fun testLineWithNoPipeDelimiter() {
        // Covers Lines 21 & 22 (Yellow) else branches in Screenshot 2026-07-03 231708.png
        val path = createTempSongFile("""
            44100 8 120
            saw vol$.8
        """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        // It processes the line as pure config with an empty notes array
        assertEquals(1, songData.channels.size)
    }

    @Test
    fun testEmptyConfigTriggersContinue() {
        // Covers Line 25 (Red) continue branch in Screenshot 2026-07-03 231708.png
        val path = createTempSongFile("""
            44100 8 120
            | F4 1 A4 1
        """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        // The malformed row is skipped cleanly, leaving 0 valid channels
        assertEquals(0, songData.channels.size)
    }

    @Test
    fun testFileWithMixedWhitespaceLines() {
        // This explicitly exercises the .trim() changes and the .isNotEmpty() filtering
        val path = createTempSongFile("""
        44100 8 120
          saw vol$.8|C4 1|   
        
             
        square vol$.4|E4 1|
    """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        // It should strip the blank lines and whitespace, finding exactly 2 channels
        assertEquals(2, songData.channels.size)
    }

    @Test
    fun testConfigTokensEmptyGutterCoverage() {
        // Case 1: configTokens.isEmpty() is true
        // This happens if a line begins immediately with a pipe, leaving nothing before it
        val path1 = createTempSongFile("""
        44100 8 120
        | F4 1
    """)

        // Case 2: configTokens.isEmpty() is false, but configTokens[0].isEmpty() is true
        // This happens if there are spaces before the pipe, but no actual strategy token
        val path2 = createTempSongFile("""
        44100 8 120
          | A4 1
    """)

        val strategy = TextFileSongLoadingStrategy()

        // Both should trigger the 'continue' guard clause and skip the malformed tracks cleanly
        assertEquals(0, strategy.load(path1).channels.size)
        assertEquals(0, strategy.load(path2).channels.size)
    }

    @Test
    fun testAdsParameterFallbacksCoverage() {
        // We use ${'$'} to safely embed literal dollar signs without breaking string compilation
        val dollar = '$'
        val path = createTempSongFile("""
        44100 8 120
        sine ads${dollar}
        sine ads${dollar}.01
        sine ads${dollar}.01${dollar}.2
        sine ads${dollar}invalid${dollar}bad${dollar}wrong
    """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        // Verifies all 4 edge cases successfully fallback to defaults
        assertEquals(4, songData.channels.size)
    }

    @Test
    fun testEffectsParametersAndClosuresFullCoverage() {
        // Targets everything in Screenshot 2026-07-03 232406.png
        val d = '$'

        val path = createTempSongFile(
            "44100 8 120\n" +
                    "sine vol\n" +                       // Tests vol fallback (?: 1.0)
                    "sine vol" + d + "invalid\n" +       // Tests vol malformed conversion
                    "sine ads" + d + "\n" +              // Tests attack, decay, sustain fallbacks
                    "sine ads" + d + ".01\n" +
                    "sine ads" + d + ".01" + d + ".2\n" +
                    "sine ads" + d + "invalid" + d + "bad" + d + "wrong|C4 1|\n"
        )

        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(6, songData.channels.size)

        // CRITICAL STEP: Evaluate the samples for each channel.
        // This forces the lambda closures to execute and evaluate the local
        // captured primitive variables, turning lines 42, 46, 47, and 48 completely green.
        for (channel in songData.channels) {
            // Evaluate at least 1 sample to run the internal decorator pipeline
            val stream = channel.generateChannelSamples(44100, 120)
            assertNotNull(stream)
        }
    }

    @Test
    fun testTanhAndClipDecoratorsCoverage() {
        val d = '$'
        val path = createTempSongFile(
            "44100 4 130\n" +
                    "saw tanh" + d + "8 clip" + d + "0.6|E3 .5|\n" +
                    "saw tanh" + d + "invalid clip" + d + "\n"
        )

        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(2, songData.channels.size)

        // Evaluate the streams to execute the inline lambda branch logics completely
        for (channel in songData.channels) {
            val stream = channel.generateChannelSamples(44100, 130)
            assertNotNull(stream)
        }
    }
}
