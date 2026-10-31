package de.soeren.dhbwplan.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")

val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
val SHORT_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

fun weekdayFull(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN).uppercase(Locale.GERMAN)

fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2).uppercase(Locale.GERMAN)

private val ONLINE_RE = Regex("(?i)\\bonline\\b|bbb\\.dhbw\\.de")
private val URL_RE = Regex("https?://\\S+")
private val ONLINE_WORD_RE = Regex("(?i)\\bonline\\b")
private val LECTURER_RE = Regex("\\s+(Prof\\.|Herr |Frau |Dr\\.)")

/** Trennt "Datenbanksysteme II Prof. Dr. Schenk" in Fach und Dozent. */
fun splitTitle(title: String): Pair<String, String?> {
    val match = LECTURER_RE.find(title) ?: return title to null
    return title.substring(0, match.range.first).trim() to title.substring(match.range.first).trim()
}

data class Lecture(
    val uid: String,
    val title: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val allDay: Boolean,
    val location: String,
    val description: String,
) {
    val online: Boolean get() = ONLINE_RE.containsMatchIn("$location $description")
    val name: String get() = splitTitle(title).first
    val lecturer: String? get() = splitTitle(title).second
    val link: String? get() = URL_RE.find("$location $description")?.value
    val place: String?
        get() = location.replace(URL_RE, "").replace(ONLINE_WORD_RE, "").trim().ifEmpty { null }
    val date: LocalDate get() = start.toLocalDate()

    fun signature(): String = "$title|${start.toEpochSecond()}|${end.toEpochSecond()}|$location"
}

data class Subject(
    val title: String,
    val count: Int,
    val first: LocalDate,
    val last: LocalDate,
) {
    val name: String get() = splitTitle(title).first
    val lecturer: String? get() = splitTitle(title).second
}

/** Ein Fach ist ein Titel mit mindestens drei Terminen; alles andere gilt als Einmaltermin. */
fun subjectsOf(events: List<Lecture>): List<Subject> =
    events.filter { !it.allDay }
        .groupBy { it.title }
        .filter { it.value.size >= 3 }
        .map { (title, list) ->
            Subject(
                title = title,
                count = list.size,
                first = LocalDate.ofEpochDay(list.minOf { it.date.toEpochDay() }),
                last = LocalDate.ofEpochDay(list.maxOf { it.date.toEpochDay() }),
            )
        }
        .sortedBy { it.title.lowercase(Locale.GERMAN) }

fun visibleEvents(events: List<Lecture>, deselected: Set<String>): List<Lecture> {
    val subjectTitles = subjectsOf(events).map { it.title }.toSet()
    return events
        .filter { it.title !in subjectTitles || it.title !in deselected }
        .sortedBy { it.start.toEpochSecond() }
}
