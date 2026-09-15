package no.nav.felles

import no.nav.felles.utils.extensions.TimeExtensions.format
import no.nav.felles.utils.extensions.TimeExtensions.local
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class LibrarySmokeTest {
    @Test
    fun `formats durations and epoch millis`() {
        val text = 5.seconds.format()
        assertEquals("5 sekunder", text)

        val instant = Instant.parse("2024-01-01T12:00:00Z")
        val local = instant.toEpochMilli().local("yyyy-MM-dd")
        assertEquals("2024-01-01", local)
    }
}
