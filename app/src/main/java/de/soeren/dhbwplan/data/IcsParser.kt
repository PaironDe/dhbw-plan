package de.soeren.dhbwplan.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Minimaler iCalendar-Parser für den DHBW-Kursplan (VEVENTs mit DTSTART/DTEND/SUMMARY/LOCATION). */
object IcsParser {
    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE_ONLY = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val FOLD = Regex("\n[ \t]")

    private class Prop(val params: Map<String, String>, val value: String)

    fun parse(text: String): List<Lecture> {
        val unfolded = text.replace("\r\n", "\n").replace("\r", "\n").replace(FOLD, "")
        val events = mutableListOf<Lecture>()
        var current: MutableMap<String, Prop>? = null

        for (line in unfolded.split("\n")) {
            when {
                line == "BEGIN:VEVENT" -> current = mutableMapOf()
                line == "END:VEVENT" -> {
                    current?.let { props -> toLecture(props)?.let { events.add(it) } }
                    current = null
                }
                current != null -> {
                    val colon = line.indexOf(':')
                    if (colon <= 0) continue
                    val head = line.substring(0, colon).split(';')
                    val name = head[0].uppercase()
                    val params = head.drop(1).mapNotNull { part ->
                        val eq = part.indexOf('=')
                        if (eq <= 0) null else part.substring(0, eq).uppercase() to part.substring(eq + 1).trim('"')
                    }.toMap()
                    current[name] = Prop(params, line.substring(colon + 1))
                }
            }
        }
        return events
    }

    private fun toLecture(props: Map<String, Prop>): Lecture? {
        val dtStart = props["DTSTART"] ?: return null
        val (start, allDay) = parseDate(dtStart) ?: return null
        val end = props["DTEND"]?.let { parseDate(it)?.first }
            ?: if (allDay) start.plusDays(1) else start.plusHours(1)
        val title = unescape(props["SUMMARY"]?.value.orEmpty()).trim()
        val location = unescape(props["LOCATION"]?.value.orEmpty()).trim()
        val description = unescape(props["DESCRIPTION"]?.value.orEmpty()).trim()
        val uid = props["UID"]?.value?.trim().orEmpty().ifEmpty { "$title-${start.toEpochSecond()}" }
        return Lecture(uid, title, start, end, allDay, location, description)
    }

    private fun parseDate(prop: Prop): Pair<ZonedDateTime, Boolean>? = try {
        val raw = prop.value.trim()
        when {
            prop.params["VALUE"] == "DATE" || (raw.length == 8 && raw.all { it.isDigit() }) ->
                LocalDate.parse(raw.take(8), DATE_ONLY).atStartOfDay(BERLIN) to true
            raw.endsWith("Z") ->
                LocalDateTime.parse(raw.dropLast(1).take(15), DATE_TIME)
                    .atZone(ZoneOffset.UTC).withZoneSameInstant(BERLIN) to false
            else -> {
                val zone = prop.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: BERLIN
                LocalDateTime.parse(raw.take(15), DATE_TIME).atZone(zone).withZoneSameInstant(BERLIN) to false
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun unescape(value: String): String =
        value.replace("\\n", "\n").replace("\\N", "\n")
            .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")
}
