import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("[ERROR] Usage: Provide a song file path as an argument.")
        exitProcess(1)
    }

    val songPath = args[0]

    try {
        // 1. Initialize engine with structural parsing strategies
        val strategy = TextFileSongLoadingStrategy()
        val synth = Synthesizer(loadingStrategy = strategy)

        println("[INFO] Loading track configurations from: $songPath")
        synth.loadSong(songPath)

        // 2. Compute the dynamic active-normalized mix buffer
        println("[INFO] Mixing channels into master buffer...")
        val masterBuffer = synth.generateMixedChannels()

        // 3. Play back via the hardware driver stream
        if (masterBuffer.isNotEmpty()) {
            println("[INFO] Initializing real-time playback device...")

            // Wrap playback inside its own block to ensure any driver exceptions are caught
            LiveAudioPlayer(sampleRate = synth.sampleRate.toFloat()).use { player ->
                player.start()
                println("[INFO] Streaming master mix live...")
                player.play(masterBuffer)
                player.drain()
            }
            println("[SUCCESS] Playback complete.")
        } else {
            println("[WARN] Master buffer is empty. Nothing to play.")
        }

    } catch (e: SongParsingException) {
        // Catches specific formatting, missing pipe tokens, or bad headers
        System.err.println("\n[FATAL PARSE ERROR] The song file is malformed.")
        System.err.println("Details: ${e.message}")
        exitProcess(1)
    } catch (e: java.io.IOException) {
        // Catches file access, permission issues, or missing files
        System.err.println("\n[FATAL I/O ERROR] Could not read file from target path.")
        System.err.println("Details: ${e.localizedMessage}")
        exitProcess(1)
    } catch (e: Exception) {
        // Catch-all for underlying audio playback runtime failures or audio hardware snags
        System.err.println("\n[FATAL RUNTIME ERROR] A systemic exception interrupted playback.")
        System.err.println("Details: ${e.localizedMessage ?: e.javaClass.simpleName}")
        exitProcess(1)
    }
}