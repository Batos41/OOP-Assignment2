fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("[ERROR] Please provide a song file path.")
        return
    }

    val songPath = args[0]
    val sampleRate = 44100

    // 1. Initialize your engine with the desired strategy
    val strategy = TextFileSongLoadingStrategy()
    val synth = Synthesizer(sampleRate = sampleRate, loadingStrategy = strategy)

    println("[INFO] Loading track configurations...")
    synth.loadSong(songPath)

    // 2. Compute the digital mix buffer
    println("[INFO] Mixing channels into master buffer...")
    val masterBuffer = synth.generateMixedChannels()

    // 3. Feed the independent audio device
    if (masterBuffer.isNotEmpty()) {
        println("[INFO] Initializing real-time playback device...")
        LiveAudioPlayer(sampleRate = sampleRate.toFloat()).use { player ->
            player.start()
            println("[INFO] Streaming master mix live...")
            player.play(masterBuffer)
            player.drain()
        }
        println("[SUCCESS] Playback complete.")
    } else {
        println("[WARN] Master buffer is empty. Nothing to play.")
    }
}