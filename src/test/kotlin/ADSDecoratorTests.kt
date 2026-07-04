import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ADSDecoratorTests {

    private val sampleRate = 1000 // Low sample rate makes testing exact timestamps easier
    private val tempo = 120

    @Test
    fun testZeroLengthAttackAndDecayBranches() {
        // Arrange: Expand allocation to 300 so index 250 is safely within bounds
        val rawStream = AudioStream { _, _ -> DoubleArray(300) { 1.0 } }

        // Edge Case 1: Instant attack (attackEnd = 0.0)
        val instantAttack = ADSDecorator(rawStream, 0.0, 0.5, 0.5)
        val instantAttackSamples = instantAttack.getSamples(sampleRate, tempo)
        // t = 0.0 should instantly skip the attack fraction branch and be 1.0 or entering decay
        assertEquals(1.0, instantAttackSamples[0], 0.001)

        // Edge Case 2: Instant decay (decayEnd equals attackEnd)
        val instantDecay = ADSDecorator(rawStream, 0.2, 0.2, 0.3)
        val instantDecaySamples = instantDecay.getSamples(sampleRate, tempo)

        // At t = 0.25s (sample index 250), which is > decayEnd (0.2s),
        // it should cleanly be at the sustain level of 0.3
        assertEquals(0.3, instantDecaySamples[250], 0.001)
    }

    @Test
    fun testEnvelopePhases() {
        // Arrange: 1 second of constant 1.0 samples
        val rawStream = AudioStream { _, _ -> DoubleArray(1000) { 1.0 } }
        val adsStream = ADSDecorator(rawStream, 0.2, 0.5, 0.4)

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
        // Allocate 300 samples so index 250 is well within bounds
        val rawStream = AudioStream { _, _ -> DoubleArray(300) { 1.0 } }

        // Test 1: Instant Attack (attackEnd = 0.0) -> Skips attack branch completely
        val instantAttack = ADSDecorator(rawStream, 0.0, 0.5, 0.5)
        val instantAttackSamples = instantAttack.getSamples(sampleRate, tempo)
        assertEquals(1.0, instantAttackSamples[0], 0.001)

        // Test 2: Instant Decay (decayEnd = attackEnd = 0.2)
        val instantDecay = ADSDecorator(rawStream, 0.2, 0.2, 0.3)
        val instantDecaySamples = instantDecay.getSamples(sampleRate, tempo)

        // At t = 0.25s (index 250), we are past the thresholds,
        // so it must hold flat at the sustain level of 0.3
        assertEquals(0.3, instantDecaySamples[250], 0.001)
    }

    @Test
    fun testEnvelopeResetsOnSubsequentNotes() {
        // Arrange: Create a stream representing 2 notes, each lasting 0.5 seconds (500 samples each, 1000 total)
        val rawStream = AudioStream { _, _ -> DoubleArray(1000) { 1.0 } }

        // Attack ends at 0.1s, Decay ends at 0.3s, Sustain holds at 0.5
        val adsStream = ADSDecorator(rawStream, 0.1, 0.3, 0.5)

        // Act
        val samples = adsStream.getSamples(sampleRate, tempo)

        // --- Note 1 Verification ---
        assertEquals(0.0, samples[0], 0.001)   // Note 1: Start of attack (t = 0.0s)
        assertEquals(1.0, samples[100], 0.001) // Note 1: Peak of attack (t = 0.1s)
        assertEquals(0.5, samples[400], 0.001) // Note 1: Flat sustain phase (t = 0.4s)

        // --- Note 2 Verification (The Re-trigger Check) ---
        // If the envelope is resetting correctly, index 500 is the start of Note 2.
        // It should drop back down to 0.0 to begin the attack phase again!
        assertEquals(0.0, samples[500], 0.001) // Note 2: Start of attack (Should reset to 0.0)

        // Index 600 is 0.1 seconds into Note 2. It should reach peak amplitude again.
        assertEquals(1.0, samples[600], 0.001) // Note 2: Peak of attack
    }
}