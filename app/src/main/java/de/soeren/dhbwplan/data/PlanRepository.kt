package de.soeren.dhbwplan.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZonedDateTime

class PlanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("plan", Context.MODE_PRIVATE)

    var course: String?
        get() = prefs.getString("course", null)
        set(value) = prefs.edit().putString("course", value).apply()

    val lastSync: Long get() = prefs.getLong("last_sync", 0L)

    fun deselected(course: String): Set<String> =
        prefs.getStringSet("deselected_$course", emptySet())?.toSet() ?: emptySet()

    fun setDeselected(course: String, titles: Set<String>) =
        prefs.edit().putStringSet("deselected_$course", titles).apply()

    private fun cacheFile(course: String) = File(appContext.filesDir, "plan_$course.ics")

    fun cached(course: String): String? = cacheFile(course).takeIf { it.exists() }?.readText()

    /** Lädt den Kursplan genau wie die offizielle Campus-App (Weiterleitung auf stash.dhbw-loerrach.de). */
    suspend fun download(course: String): String = withContext(Dispatchers.IO) {
        val alias = COURSE_ALIASES[course] ?: course
        val url = URL("https://webmail.dhbw-loerrach.de/owa/calendar/kal-$alias@dhbw-loerrach.de/Kalender/calendar.ics")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "DHBW-PLAN/1.0")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (!text.trimStart().startsWith("BEGIN:VCALENDAR")) throw IOException("Kein Kalender")
            cacheFile(course).writeText(text)
            prefs.edit().putLong("last_sync", System.currentTimeMillis()).apply()
            text
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Vergleicht kommende Termine mit dem letzten Stand und liefert eine lesbare Zusammenfassung
     * der Änderungen, die ausgewählte Fächer bzw. Einmaltermine betreffen. Null = keine Änderung.
     */
    fun checkChanges(course: String, events: List<Lecture>, deselected: Set<String>): String? {
        val now = ZonedDateTime.now(BERLIN)
        val nowSec = now.toEpochSecond()
        val subjectTitles = subjectsOf(events).map { it.title }.toSet()
        fun isVisible(title: String) = title !in subjectTitles || title !in deselected

        val upcoming = events.filter { it.end.isAfter(now) }
        val newMap = upcoming.associate { it.uid to it.signature() }
        val key = "snapshot_$course"
        val oldRaw = prefs.getString(key, null)
        prefs.edit().putString(key, newMap.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()
        if (oldRaw.isNullOrEmpty()) return null

        val oldMap = oldRaw.lines().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab < 0) null else line.substring(0, tab) to line.substring(tab + 1)
        }.toMap().filter { (_, sig) ->
            (sig.split('|').getOrNull(2)?.toLongOrNull() ?: 0L) > nowSec
        }

        val byUid = upcoming.associateBy { it.uid }
        val lines = mutableListOf<String>()
        for ((uid, sig) in newMap) {
            val ev = byUid[uid] ?: continue
            if (!isVisible(ev.title)) continue
            val old = oldMap[uid]
            val label = "${ev.name} · ${weekdayShort(ev.date)} ${ev.start.format(SHORT_DATE_TIME)}"
            if (old == null) lines += "NEU: $label" else if (old != sig) lines += "GEÄNDERT: $label"
        }
        for ((uid, sig) in oldMap) {
            if (uid in newMap) continue
            val parts = sig.split('|')
            val title = parts.getOrNull(0) ?: continue
            if (!isVisible(title)) continue
            val start = parts.getOrNull(1)?.toLongOrNull()
                ?.let { Instant.ofEpochSecond(it).atZone(BERLIN) } ?: continue
            lines += "ENTFÄLLT: ${splitTitle(title).first} · ${weekdayShort(start.toLocalDate())} ${start.format(SHORT_DATE_TIME)}"
        }
        if (lines.isEmpty()) return null
        val extra = if (lines.size > 6) "\n+${lines.size - 6} weitere" else ""
        return lines.take(6).joinToString("\n") + extra
    }

    companion object {
        /** Aus der Campus-App übernommen: manche Kurse heißen im Kalender anders. */
        val COURSE_ALIASES = mapOf(
            "wwi23a" to "wwi23a-am",
            "wwi23b" to "wwi23b-am",
            "wwi25a" to "wwi25a-am",
            "wwi25b" to "wwi25b-am",
        )
    }
}
