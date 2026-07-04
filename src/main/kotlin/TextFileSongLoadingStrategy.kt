import java.io.File

class TextFileSongStrategy : SongLoadingStrategy {

    // 1. Production entry point (unchanged from the original file contract)
    override fun load(source: String): SongData {
        val lines = File(source).readLines()
        return parseLines(lines)
    }

    // 2. Integration Test entry point (accepts raw string text blocks)
    fun loadFromString(content: String): SongData {
        val lines = content.lines()
        return parseLines(lines)
    }

    // 3. Centralized execution pipeline that operates entirely on standard lists
    private fun parseLines(rawLines: List<String>): SongData {
        val lines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw IllegalArgumentException("Song file is empty")

        // Parse Header: sampleRate bitDepth tempo
        val headerTokens = lines[0].split("\\s+".toRegex())
        val tempo = headerTokens.getOrNull(2)?.toIntOrNull() ?: 120

        val channels = mutableListOf<AudioChannel>()

        // Parse Channel Rows
        for (i in 1 until lines.size) {
            val line = lines[i]

            // Split configuration metadata from the musical bars
            val firstPipeIndex = line.indexOf('|')
            val configPart = if (firstPipeIndex != -1) line.substring(0, firstPipeIndex).trim() else line
            val notesPart = if (firstPipeIndex != -1) line.substring(firstPipeIndex) else ""

            val configTokens = configPart.split("\\s+".toRegex())
            if (configTokens[0].isEmpty()) continue

            // A. Determine Waveform Strategy
            val strategy = when (configTokens[0].lowercase()) {
                "saw" -> SawWaveStrategy()
                "square" -> SquareWaveStrategy()
                "whitenoise" -> WhiteNoiseStrategy()
                else -> SineWaveStrategy() // Default fallback
            }

            // B. Parse Decorator Factory Closures
            val effects = mutableListOf<(AudioStream) -> AudioStream>()
            val noteDurations = mutableListOf<Double>()
            for (j in 1 until configTokens.size) {
                val effectToken = configTokens[j]
                val parts = effectToken.split("$")
                when (parts[0].lowercase()) {
                    "vol" -> {
                        effects.add { stream ->
                            VolumeDecorator(
                                decoratedStream = stream,
                                level = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                            )
                        }
                    }
                    "ads" -> {
                        effects.add { stream ->
                            ADSDecorator(
                                decoratedStream = stream,
                                attackEnd = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
                                decayEnd = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                                sustain = parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0,
                                noteDurations = noteDurations
                            )
                        }
                    }
                    "tanh" -> {
                        effects.add { stream ->
                            TanhDistortionDecorator(
                                decoratedStream = stream,
                                drive = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                            )
                        }
                    }
                    "clip" -> {
                        effects.add { stream ->
                            ClipDistortionDecorator(
                                decoratedStream = stream,
                                threshold = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                            )
                        }
                    }
                }
            }

            // C. Parse Note Stream (Ignoring measure bar dividers '|')
            val noteTokens = notesPart.replace("|", " ").trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val notes = mutableListOf<Note>()

            for (k in noteTokens.indices step 2) {
                val pitch = noteTokens.getOrNull(k) ?: break
                val durationStr = noteTokens.getOrNull(k + 1) ?: "1"
                val duration = durationStr.toDoubleOrNull() ?: 1.0

                notes.add(Note(pitch, duration))
                noteDurations.add(duration)
            }

            // Assemble into our verified structural format
            channels.add(AudioChannel(notes, strategy, effects))
        }

        return SongData(tempo, channels)
    }
}