package no.nav.sikkerhetstjenesten.felles.utils.extensions

import java.time.Clock
import java.time.Duration.between
import java.time.Instant
import java.time.Instant.now
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.Period.ofYears
import java.time.ZoneId
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatter.ofPattern
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

object TimeExtensions {
    val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

    val ALLTID get() = LocalDate.now().plusYears(100)
    val IMORGEN get() = LocalDate.now().plusDays(1)
    val IGÅR get() = LocalDate.now().minusDays(1)
    fun java.time.Duration.format() = this.toKotlinDuration().format()

    fun Duration.format(): String {
        val days = inWholeDays
        val hours = inWholeHours % 24
        val minutes = inWholeMinutes % 60
        val seconds = inWholeSeconds % 60

        return buildString {
            if (days > 0) append("$days ${if (days == 1L) "dag" else "dager"} ")
            if (hours > 0) append("$hours ${if (hours == 1L) "time" else "timer"} ")
            if (minutes > 0) append("$minutes ${if (minutes == 1L) "minutt" else "minutter"} ")
            if (seconds > 0) append("$seconds ${if (seconds == 1L) "sekund" else "sekunder"}")
        }.trim()
    }

    fun Instant.isBeforeNow(clock: Clock) =
        isBefore(now(clock))

    fun Instant.diffFromNow(clock: Clock ) =
        between(now(clock), this).toKotlinDuration().format()

    fun LocalDate.toInstant(zone: ZoneId = systemDefault()) =
        atStartOfDay(zone).toInstant()

    fun Long.local(fmt: String = "yyyy-MM-dd HH:mm:ss") = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(this),
        OSLO,
    ).format(ofPattern(fmt))


    fun LocalDate.månederSidenIdag(clock: Clock): Int {
        val today = LocalDate.now(clock)
        require(!isAfter(today)) { "Datoen $this er etter dagens dato $today" }
        val p = Period.between(this, today)
        val rundetOpp = if (today.dayOfMonth > dayOfMonth) 1 else 0
        return (p.years * 12 + p.months + rundetOpp).coerceAtLeast(1)
    }



    fun LocalDate.isBetween(start: LocalDate, end: LocalDate) = this in start..end

    private fun component(value: Long, singular: String, plural: String) =
        value.takeIf { it > 0 }?.let { "$it ${if (it == 1L) singular else plural}" }


    val Int.år: Period get() = ofYears(this)

}
