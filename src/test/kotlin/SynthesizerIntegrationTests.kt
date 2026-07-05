import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SynthesizerIntegrationTests {
    private val strategy = TextFileSongLoadingStrategy()

    @Test
    fun testMultiChannelWithDecoratorsIntegration() {
        // 1. Grab your complex wave file data straight from the string snippet
        val trackContent = """
            44100 4 130
            saw tanh${'$'}8 clip${'$'}0.6|E3 .5 E3 .5 E3 .5 G3 .5 E3 .5 A3 .5 G3 .5 E3 .5|
            square vol${'$'}0.4 ads${'$'}.01${'$'}.3${'$'}.6|E2 4|
        """.trimIndent()

        // 2. Use the new in-memory loader method
        val strategy = TextFileSongLoadingStrategy()
        val songData = strategy.loadFromString(trackContent)

        // 3. Feed it to your existing Synthesizer structure to test the full pipeline
        val synth = Synthesizer(loadingStrategy = strategy)

        // (If your synthesizer needs to internally call load() via sourceIdentifier,
        // you can subclass or pass a dummy file identifier, but executing synth.play()
        // with pre-loaded data or using the strategy output directly is the goal!)

        assertNotNull(songData)
        assertEquals(130, songData.tempo)
        assertEquals(2, songData.channels.size)
    }

    /**
     * Reads a file from src/main/resources/ by its filename verbatim.
     */
    private fun readResourceFile(filename: String): String {
        val inputStream = javaClass.classLoader.getResourceAsStream(filename)
            ?: throw IllegalArgumentException("Could not find resource file: $filename")

        return inputStream.bufferedReader().use { it.readText() }
    }

    @Test
    fun testFxClipIntegration() {
        // References "fx_clip.txt" by its name verbatim
        val fileContent = readResourceFile("fx_clip.txt")
        val songData = strategy.loadFromString(fileContent)

        assertNotNull(songData)
        assertEquals(120, songData.tempo)
        assertEquals(1, songData.channels.size)
    }

    @Test
    fun testAllFilesLoadWithoutThrowing() {
        val files = listOf(
            "fx_ads_pad.txt",
            "fx_ads_pluck.txt",
            "fx_clip.txt",
            "fx_tanh.txt",
            "fx_tanh_clip_stack.txt",
            "fx_volume.txt",
            "wave_noise.txt",
            "wave_saw.txt",
            "wave_sine.txt",
            "wave_square.txt"
        )

        for (fileName in files) {
            val fileContent = readResourceFile(fileName)
            val songData = strategy.loadFromString(fileContent)

            // Includes a helpful lazy message if a specific file breaks
            assertNotNull(songData) { "Failed to load configuration file: $fileName" }
        }
    }

    @Test
    fun testComplexStackIntegration() {
        // References "fx_tanh_clip_stack.txt" by its name verbatim
        val fileContent = readResourceFile("fx_tanh_clip_stack.txt")
        val songData = strategy.loadFromString(fileContent)

        assertNotNull(songData)
        assertEquals(130, songData.tempo)
        // Verifies both channels are parsed out of the stacked text document
        assertEquals(2, songData.channels.size)
    }
}