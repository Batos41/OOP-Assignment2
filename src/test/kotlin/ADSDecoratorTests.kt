import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ADSDecoratorTests {

    private val sampleRate = 1000 // Low sample rate makes testing exact timestamps easier
    private val tempo = 120

    @Test
    fun testZeroLengthAttackAndDecayBranches() {
        // Arrange: Expand allocation to 300 so index 250 is safely within bounds
        val rawStream = AudioStream { _, _ -> DoubleArray(300) { 1.0 } }
        val singleNoteDuration = listOf(0.6)
        val isNoteList = listOf(true) // Marks the segment as a playable note

        // Edge Case 1: Instant attack (attackEnd = 0.0)
        val instantAttack = ADSDecorator(rawStream, 0.0, 0.5, 0.5, singleNoteDuration, isNoteList)
        val instantAttackSamples = instantAttack.getSamples(sampleRate, tempo)
        assertEquals(1.0, instantAttackSamples[0], 0.001)

        // Edge Case 2: Instant decay (decayEnd equals attackEnd)
        val instantDecay = ADSDecorator(rawStream, 0.2, 0.2, 0.3, singleNoteDuration, isNoteList)
        val instantDecaySamples = instantDecay.getSamples(sampleRate, tempo)

        assertEquals(0.3, instantDecaySamples[250], 0.001)
    }

    @Test
    fun testEnvelopePhases() {
        // Arrange: 1 second of constant 1.0 samples (1000 samples = 2.0 beats at 120 BPM)
        val rawStream = AudioStream { _, _ -> DoubleArray(1000) { 1.0 } }
        val singleNoteDuration = listOf(2.0)
        val isNoteList = listOf(true)
        val adsStream = ADSDecorator(rawStream, 0.2, 0.5, 0.4, singleNoteDuration, isNoteList)

        // Act
        val samples = adsStream.getSamples(sampleRate, tempo)

        // Assert
        assertEquals(0.0, samples[0], 0.001)   // Start of attack
        assertEquals(0.5, samples[100], 0.001) // Mid attack
        assertEquals(1.0, samples[200], 0.001) // Peak attack / Start decay
        assertEquals(0.7, samples[350], 0.001) // Mid decay
        assertEquals(0.4, samples[600], 0.001) // Sustain phase
    }

    @Test
    fun testSkippedPhasesCoverage() {
        val rawStream = AudioStream { _, _ -> DoubleArray(300) { 1.0 } }
        val singleNoteDuration = listOf(0.6)
        val isNoteList = listOf(true)

        // Test 1: Instant Attack (attackEnd = 0.0)
        val instantAttack = ADSDecorator(rawStream, 0.0, 0.5, 0.5, singleNoteDuration, isNoteList)
        val instantAttackSamples = instantAttack.getSamples(sampleRate, tempo)
        assertEquals(1.0, instantAttackSamples[0], 0.001)

        // Test 2: Instant Decay (decayEnd = attackEnd = 0.2)
        val instantDecay = ADSDecorator(rawStream, 0.2, 0.2, 0.3, singleNoteDuration, isNoteList)
        val instantDecaySamples = instantDecay.getSamples(sampleRate, tempo)

        assertEquals(0.3, instantDecaySamples[250], 0.001)
    }

    @Test
    fun testEnvelopeResetsOnSubsequentNotes() {
        val rawStream = AudioStream { _, _ -> DoubleArray(1000) { 1.0 } }
        val sequentialNotesDurations = listOf(1.0, 1.0)
        val isNoteList = listOf(true, true) // Both segments are active notes

        val adsStream = ADSDecorator(rawStream, 0.1, 0.3, 0.5, sequentialNotesDurations, isNoteList)
        val samples = adsStream.getSamples(sampleRate, tempo)

        // --- Note 1 ---
        assertEquals(0.0, samples[0], 0.001)
        assertEquals(1.0, samples[100], 0.001)
        assertEquals(0.5, samples[400], 0.001)

        // --- Note 2 ---
        assertEquals(0.0, samples[500], 0.001) // Confirms re-trigger reset drops to 0.0
        assertEquals(1.0, samples[600], 0.001)
    }

    @Test
    fun testEnvelopeBypassesRests() {
        // Arrange: 1500 samples total (3 segments of 500 samples each)
        val rawStream = AudioStream { _, _ -> DoubleArray(1500) { 1.0 } }

        // Sequence: Note (1.0 beat), Rest (1.0 beat), Note (1.0 beat)
        val trackDurations = listOf(1.0, 1.0, 1.0)
        val isNoteList = listOf(true, false, true) // Middle segment is a rest

        val adsStream = ADSDecorator(rawStream, 0.1, 0.3, 0.5, trackDurations, isNoteList)
        val samples = adsStream.getSamples(sampleRate, tempo)

        // Segment 1 (Note): Applies attack envelope
        assertEquals(0.0, samples[0], 0.001)
        assertEquals(1.0, samples[100], 0.001)

        // Segment 2 (Rest): Envelope logic is bypassed. Signal left unmodified (1.0)
        // (The mixer pipeline handles downstream silence, avoiding timeline drift)
        assertEquals(1.0, samples[500], 0.001)
        assertEquals(1.0, samples[750], 0.001)

        // Segment 3 (Note): Retriggers cleanly starting at sample index 1000
        assertEquals(0.0, samples[1000], 0.001)
        assertEquals(1.0, samples[1100], 0.001)
    }
}