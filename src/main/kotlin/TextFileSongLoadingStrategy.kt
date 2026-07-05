import java.io.File

class SongParsingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class TextFileSongLoadingStrategy : SongLoadingStrategy {

    override fun load(source: String): SongData {
        val file = File(source)
        if (!file.exists()) throw SongParsingException("Song file not found: $source")

        return try {
            parseLines(file.readLines())
        } catch (e: Exception) {
            throw SongParsingException("Failed to read or parse song file '$source'", e)
        }
    }

    fun loadFromString(content: String): SongData {
        return parseLines(content.lines())
    }

    private fun parseLines(rawLines: List<String>): SongData {
        val lines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw SongParsingException("Parser Error: Song file contains no readable musical structure.")

        // Strict Header Validation
        val headerTokens = lines[0].split("\\s+".toRegex())
        if (headerTokens.size < 3) {
            throw SongParsingException("Malformed Header: Expected '[sampleRate] [beatsPerMeasure] [tempo]', found '${lines[0]}'")
        }

        val sampleRate = headerTokens[0].toIntOrNull()
            ?: throw SongParsingException("Invalid Header: Sample rate '${headerTokens[0]}' must be a valid integer.")
        val beatsPerMeasure = headerTokens[1].toIntOrNull()
            ?: throw SongParsingException("Invalid Header: Beats per measure '${headerTokens[1]}' must be a valid integer.")
        val tempo = headerTokens[2].toIntOrNull()
            ?: throw SongParsingException("Invalid Header: Tempo '${headerTokens[2]}' must be a valid integer.")

        val channels = mutableListOf<AudioChannel>()

        // Parse Channel Rows
        for (i in 1 until lines.size) {
            val line = lines[i]
            val lineNumber = i + 1

            val firstPipeIndex = line.indexOf('|')
            if (firstPipeIndex == -1) {
                throw SongParsingException("Line $lineNumber: Missing structural channel pipe separator '|'")
            }

            val configPart = line.substring(0, firstPipeIndex).trim()
            val notesPart = line.substring(firstPipeIndex)

            val configTokens = configPart.split("\\s+".toRegex())
            if (configTokens.isEmpty() || configTokens[0].isEmpty()) {
                throw SongParsingException("Line $lineNumber: Missing track oscillator configuration strategy.")
            }

            // Strict Waveform Evaluation
            val strategy = when (configTokens[0].lowercase()) {
                "saw" -> SawWaveStrategy()
                "square" -> SquareWaveStrategy()
                "whitenoise" -> WhiteNoiseStrategy()
                "sin" -> SineWaveStrategy()
                else -> throw SongParsingException("Line $lineNumber: Unknown waveform strategy type '${configTokens[0]}'")
            }

            // Parse Note Stream FIRST to pass immutable properties into effects
            val noteTokens = notesPart.replace("|", " ").trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (noteTokens.size % 2 != 0) {
                throw SongParsingException("Line $lineNumber: Incomplete musical notes data block. Expected alternating Pitch and Duration tokens.")
            }

            val notes = mutableListOf<Note>()
            val noteDurations = mutableListOf<Double>()
            val isNoteList = mutableListOf<Boolean>()

            for (k in noteTokens.indices step 2) {
                val pitch = noteTokens[k]
                val durationStr = noteTokens[k + 1]
                val duration = durationStr.toDoubleOrNull()
                    ?: throw SongParsingException("Line $lineNumber: Note duration '$durationStr' must be a valid numeric float.")

                notes.add(Note(pitch, duration))
                noteDurations.add(duration)

                // If the token is "-", it's a rest (false), otherwise it's a playable note (true)
                isNoteList.add(pitch != "-")
            }

            // Explicit Decorator Construction Path
            val effects = mutableListOf<(AudioStream) -> AudioStream>()

            // Create an unmodifiable structural snapshot of lengths for safe decorator closure injection
            val cleanDurationsSnapshot = noteDurations.toList()
            val cleanIsNoteSnapshot = isNoteList.toList()

            for (j in 1 until configTokens.size) {
                val effectToken = configTokens[j]
                val parts = effectToken.split("$")
                when (val effectName = parts[0].lowercase()) {
                    "vol" -> {
                        val levelStr = parts.getOrNull(1)
                            ?: throw SongParsingException("Line $lineNumber: Effect '$effectToken' missing level property.")
                        val level = levelStr.toDoubleOrNull()
                            ?: throw SongParsingException("Line $lineNumber: Volume property '$levelStr' is not a valid number.")

                        effects.add { VolumeDecorator(it, level) }
                    }
                    "ads" -> {
                        if (parts.size < 4) {
                            throw SongParsingException($$"Line $$lineNumber: Envelope '$$effectToken' requires explicitly formatted parameters: ads$attack$decay$sustain")
                        }
                        val attack = parts[1].toDoubleOrNull() ?: throw SongParsingException("Line $lineNumber: Invalid attack: ${parts[1]}")
                        val decay = parts[2].toDoubleOrNull() ?: throw SongParsingException("Line $lineNumber: Invalid decay: ${parts[2]}")
                        val sustain = parts[3].toDoubleOrNull() ?: throw SongParsingException("Line $lineNumber: Invalid sustain: ${parts[3]}")

                        effects.add {
                            ADSDecorator(
                                it,
                                attack,
                                decay,
                                sustain,
                                cleanDurationsSnapshot,
                                cleanIsNoteSnapshot
                            )
                        }
                    }
                    "tanh" -> {
                        val driveStr = parts.getOrNull(1) ?: throw SongParsingException("Line $lineNumber: Tanh missing drive profile.")
                        val drive = driveStr.toDoubleOrNull() ?: throw SongParsingException("Line $lineNumber: Invalid Tanh drive parameter: $driveStr")

                        effects.add { TanhDistortionDecorator(it, drive) }
                    }
                    "clip" -> {
                        val thresholdStr = parts.getOrNull(1) ?: throw SongParsingException("Line $lineNumber: Clip missing parameter threshold.")
                        val threshold = thresholdStr.toDoubleOrNull() ?: throw SongParsingException("Line $lineNumber: Invalid Clip threshold value: $thresholdStr")

                        effects.add { ClipDistortionDecorator(it, threshold) }
                    }
                    else -> throw SongParsingException("Line $lineNumber: Encountered unsupported processing processor component moniker: '$effectName'")
                }
            }

            channels.add(AudioChannel(notes, strategy, effects))
        }

        return SongData(sampleRate, beatsPerMeasure, tempo, channels)
    }
}