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
        val path = createTempSongFile("")
        val strategy = TextFileSongLoadingStrategy()

        // Updated to catch our descriptive exception
        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testMissingTempoThrows() {
        val path = createTempSongFile("44100 8") // No third token for tempo
        val strategy = TextFileSongLoadingStrategy()

        // Assert that a missing header parameter now properly crashes execution
        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testTrackLineMissingPipeThrows() {
        val path = createTempSongFile("""
            44100 8 120
            unknownWave vol
        """)
        val strategy = TextFileSongLoadingStrategy()

        // Missing structural pipe indicator must fail fast
        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testMalformedEffectParametersThrows() {
        val path = createTempSongFile("""
            44100 8 120
            sine vol$ ads$$$|C4|
        """)
        val strategy = TextFileSongLoadingStrategy()

        // Replaced silent fallback check with strict validation assertion
        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testAllWaveformStrategiesCoverage() {
        val path = createTempSongFile("""
            44100 8 120
            square|C4 1|
            whitenoise|C4 1|
            saw|C4 1|
        """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(3, songData.channels.size)
        assertNotNull(songData.channels[0])
        assertNotNull(songData.channels[1])
        assertNotNull(songData.channels[2])
    }

    @Test
    fun testEmptyOrWhitespaceFileThrows() {
        val path = createTempSongFile("   \n   ")
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testMalformedHeaderTempoThrows() {
        val pathInvalid = createTempSongFile("44100 8 abc")
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<SongParsingException> {
            strategy.load(pathInvalid)
        }
    }

    @Test
    fun testLineWithNoPipeDelimiterThrows() {
        val path = createTempSongFile("""
            44100 8 120
            saw vol$.8
        """)
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testEmptyConfigThrowsInsteadOfContinue() {
        val path = createTempSongFile("""
            44100 8 120
            | F4 1 A4 1
        """)
        val strategy = TextFileSongLoadingStrategy()

        // Lines starting with an empty gutter space are invalid
        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testFileWithMixedWhitespaceLines() {
        val path = createTempSongFile("""
            44100 8 120
              saw |C4 1|   
            
                 
            square |E4 1|
        """)
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(2, songData.channels.size)
    }

    @Test
    fun testConfigTokensEmptyGutterThrows() {
        val path = createTempSongFile("""
            44100 8 120
              | A4 1
        """)
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testAdsParameterFallbacksThrows() {
        val dollar = '$'
        val path = createTempSongFile("""
            44100 8 120
            sine ads${dollar}
        """)
        val strategy = TextFileSongLoadingStrategy()

        assertThrows<SongParsingException> {
            strategy.load(path)
        }
    }

    @Test
    fun testTanhAndClipDecoratorsCoverage() {
        val d = '$'
        val path = createTempSongFile(
            "44100 4 130\n" +
                    "saw tanh${d}8 clip${d}0.6|E3 .5|\n"
        )

        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.load(path)

        assertEquals(1, songData.channels.size)

        for (channel in songData.channels) {
            val stream = channel.generateChannelSamples(44100, 130)
            assertNotNull(stream)
        }
    }

    @Test
    fun testThrowIfFileNotFound() {
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> {
            strategy.load("non_existent_file_path_xyz.txt")
        }

        // Simply check the exception text for the missing file context
        val message = exception.message ?: ""
        assertTrue(message.contains("not found") || message.contains("Failed to read"))
    }

    @Test
    fun testThrowOnMalformedHeaderSize() {
        val path = createTempSongFile("44100")
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        // Check message or the cause message depending on where your code attaches it
        val actualErrorText = exception.message ?: exception.cause?.message ?: ""
        assertTrue(
            actualErrorText.contains("Malformed Header") || actualErrorText.contains("parse"),
            "Expected header size validation failure context, but got: $actualErrorText"
        )
    }

    @Test
    fun testThrowOnInvalidSampleRateToken() {
        val path = createTempSongFile("abc 8 120")
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        // If your parser throws due to NumberFormatException, it will be wrapped in the cause!
        val hasNumberFormatError = exception.cause is java.lang.NumberFormatException
        val hasSampleRateText = exception.message!!.contains("Sample rate") || exception.cause?.message?.contains("abc") == true

        assertTrue(
            hasNumberFormatError || hasSampleRateText,
            "Expected sample rate integer parsing failure. Cause: ${exception.cause}"
        )
    }

    @Test
    fun testThrowOnInvalidTempoToken() {
        val path = createTempSongFile("44100 8 xyz")
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        val hasNumberFormatError = exception.cause is java.lang.NumberFormatException
        val hasTempoText = exception.message!!.contains("Tempo") || exception.cause?.message?.contains("xyz") == true

        assertTrue(
            hasNumberFormatError || hasTempoText,
            "Expected tempo integer parsing failure. Cause: ${exception.cause}"
        )
    }

    @Test
    fun testThrowOnInvalidNoteDurationFormat() {
        val path = createTempSongFile("""
            44100 8 120
            sin | C4 abc
        """)
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        // Extract the message from the nested cause exception
        val nestedCause = exception.cause
        assertNotNull(nestedCause, "The parsing exception should be wrapped as the cause")

        val causeMessage = nestedCause?.message ?: ""
        assertTrue(causeMessage.contains("duration") && causeMessage.contains("abc"))
    }

    @Test
    fun testThrowOnIncompleteMusicalNotesPairs() {
        // Line 69: Uneven tokens block (Pitch without its corresponding duration value)[cite: 7]
        val path = createTempSongFile("""
            44100 8 120
            sin | C4 1 E4
        """)
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        val causeMessage = exception.cause?.message ?: ""
        assertTrue(causeMessage.contains("Incomplete musical notes data block"))
    }

    @Test
    fun testThrowOnVolumeEffectMissingLevel() {
        // Line 95: 'vol' token missing the second part after a '$' splitting delimiter[cite: 7]
        val path = createTempSongFile("""
            44100 8 120
            sin vol | C4 1
        """)
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        val causeMessage = exception.cause?.message ?: ""
        assertTrue(causeMessage.contains("missing level property"))
    }

    @Test
    fun testThrowOnVolumeEffectMalformedLevel() {
        // Line 97: Volume text cannot be parsed into a Double precision number[cite: 7]
        val path = createTempSongFile("""
            44100 8 120
            sin vol${'$'}notANumber | C4 1
        """)
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        val causeMessage = exception.cause?.message ?: ""
        assertTrue(causeMessage.contains("is not a valid number"))
    }

    @Test
    fun testThrowOnEnvelopeIncompleteParameters() {
        // Line 103: 'ads' parameter count is less than 4 (e.g. ads${'$'}0.1${'$'}0.2)[cite: 7]
        val path = createTempSongFile("""
            44100 8 120
            sin ads${'$'}0.1${'$'}0.2 | C4 1
        """)
        val strategy = TextFileSongLoadingStrategy()

        val exception = assertThrows<SongParsingException> { strategy.load(path) }

        val causeMessage = exception.cause?.message ?: ""
        assertTrue(causeMessage.contains("requires explicitly formatted parameters"))
    }
}