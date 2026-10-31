package de.soeren.dhbwplan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.soeren.dhbwplan.data.BERLIN
import de.soeren.dhbwplan.data.DATE
import de.soeren.dhbwplan.data.Lecture
import de.soeren.dhbwplan.data.SHORT_DATE
import de.soeren.dhbwplan.data.SHORT_DATE_TIME
import de.soeren.dhbwplan.data.Subject
import de.soeren.dhbwplan.data.TIME
import de.soeren.dhbwplan.data.weekdayFull
import de.soeren.dhbwplan.data.weekdayShort
import de.soeren.dhbwplan.ui.theme.Mono
import de.soeren.dhbwplan.ui.theme.Neon
import de.soeren.dhbwplan.ui.theme.NeonTheme
import de.soeren.dhbwplan.ui.theme.Orbitron
import de.soeren.dhbwplan.ui.theme.Rajdhani
import de.soeren.dhbwplan.ui.theme.chamfer
import de.soeren.dhbwplan.ui.theme.glow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class Tab(val label: String) { HEUTE("HEUTE"), PLAN("PLAN"), FAECHER("FÄCHER") }

@Composable
fun DhbwPlanApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    RequestNotificationPermission()
    NeonTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Neon.Bg)
                .scanlines()
        ) {
            if (state.course == null || state.showCoursePicker) {
                CourseScreen(state, vm)
            } else {
                MainScreen(state, vm)
            }
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

// ---------- Kurswahl ----------

@Composable
private fun CourseScreen(state: UiState, vm: MainViewModel) {
    var input by rememberSaveable { mutableStateOf(state.course?.uppercase() ?: "") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Text(
            "DHBW//PLAN",
            fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 30.sp,
            color = Neon.Magenta, style = glow(Neon.Magenta),
        )
        Text(
            "Gib deinen Kurs ein, z. B. TIF24A. Der Plan kommt direkt von der DHBW Lörrach.",
            fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Neon.Soft,
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.uppercase(Locale.GERMAN) },
            label = { Text("KURS", fontFamily = Orbitron, fontSize = 12.sp) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Neon.Text),
            shape = chamfer(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon.Cyan,
                unfocusedBorderColor = Neon.Border,
                focusedLabelColor = Neon.Cyan,
                unfocusedLabelColor = Neon.Muted,
                cursorColor = Neon.Magenta,
                focusedTextColor = Neon.Text,
                unfocusedTextColor = Neon.Text,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { vm.setCourse(input) }),
            modifier = Modifier.fillMaxWidth(),
        )
        NeonButton(
            text = if (state.loading) "LADE …" else "PLAN LADEN",
            onClick = { if (!state.loading) vm.setCourse(input) },
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(it, fontFamily = Mono, fontSize = 13.sp, color = Neon.Yellow)
        }
        if (state.course != null) {
            NeonButton(
                text = "ABBRECHEN",
                onClick = vm::closeCoursePicker,
                filled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- Hauptansicht ----------

@Composable
private fun MainScreen(state: UiState, vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HEUTE) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(state, onCourse = vm::openCoursePicker, onRefresh = vm::refresh)

        state.changes?.let { changes ->
            NeonCard(border = Neon.Yellow, onClick = vm::dismissChanges) {
                Text("PLANÄNDERUNG", fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, color = Neon.Yellow)
                Text(changes, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Tippen zum Ausblenden", fontFamily = Mono, fontSize = 11.sp, color = Neon.Muted)
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (tab) {
                Tab.HEUTE -> DayView(state)
                Tab.PLAN -> PlanList(state)
                Tab.FAECHER -> SubjectsView(state, vm)
            }
        }

        SyncLine(state)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tab.entries.forEach { t ->
                NeonButton(
                    text = t.label,
                    onClick = { tab = t },
                    filled = t == tab,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Header(state: UiState, onCourse: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "DHBW//PLAN",
            fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 22.sp,
            letterSpacing = 1.sp, color = Neon.Magenta, style = glow(Neon.Magenta),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClick = onRefresh)
                    .semantics { contentDescription = "Plan aktualisieren" },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (state.loading) "…" else "↻", fontSize = 22.sp, color = Neon.Cyan)
            }
            val shape = chamfer(10.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(Neon.Cyan)
                    .padding(1.dp)
                    .clip(shape)
                    .background(Neon.Bg)
                    .clickable(role = Role.Button, onClick = onCourse)
                    .semantics { contentDescription = "Kurs wechseln" }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    "${state.course?.uppercase().orEmpty()} ▾",
                    fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Neon.Cyan,
                )
            }
        }
    }
}

@Composable
private fun SyncLine(state: UiState) {
    val time = if (state.lastSync > 0) {
        Instant.ofEpochMilli(state.lastSync).atZone(BERLIN).format(SHORT_DATE_TIME)
    } else {
        "–"
    }
    val text = when {
        state.loading -> "SYNC LÄUFT …"
        state.error != null -> "${state.error} · $time"
        else -> "SYNC $time"
    }
    Text(
        text,
        fontFamily = Mono, fontSize = 11.sp,
        color = if (state.error != null) Neon.Yellow else Neon.Muted,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// ---------- HEUTE ----------

@Composable
private fun DayView(state: UiState) {
    val today = LocalDate.now(BERLIN)
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    val date = LocalDate.parse(selected)
    val now = ZonedDateTime.now(BERLIN)
    val visible = state.visible
    val dayEvents = visible.filter { it.date == date }
    val highlight = if (date == today) dayEvents.firstOrNull { it.end.isAfter(now) } else dayEvents.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(weekdayFull(date), fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 2.sp)
            val rooms = dayEvents.mapNotNull { it.place }.distinct()
            val parts = buildList {
                add(date.format(DATE))
                add(if (dayEvents.size == 1) "1 TERMIN" else "${dayEvents.size} TERMINE")
                if (rooms.size == 1) add("RAUM ${rooms.first()}")
            }
            Text(parts.joinToString(" · "), fontFamily = Mono, fontSize = 13.sp, color = Neon.Muted)
        }

        if (dayEvents.isEmpty()) {
            val next = visible.firstOrNull { it.date > date }
            NeonCard(border = Neon.Cyan) {
                Text("FREI", fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Neon.Cyan)
                Text(
                    next?.let { "Nächster Termin: ${weekdayShort(it.date)} ${it.start.format(SHORT_DATE_TIME)} · ${it.name}" }
                        ?: "Keine weiteren Termine im Plan.",
                    fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Neon.Soft,
                )
            }
        }

        dayEvents.forEach { ev ->
            val tag = when {
                ev != highlight || date != today -> null
                !ev.start.isAfter(now) -> "JETZT"
                else -> "ALS NÄCHSTES"
            }
            LectureCard(ev, highlighted = ev == highlight, tag = tag)
        }

        WeekStrip(date = date, today = today, visible = visible, onSelect = { selected = it.toString() })
        NextOnline(visible, now)
    }
}

@Composable
private fun LectureCard(ev: Lecture, highlighted: Boolean, tag: String?) {
    val uriHandler = LocalUriHandler.current
    val link = ev.link
    NeonCard(
        border = if (highlighted) Neon.Magenta else Neon.Border,
        fill = if (highlighted) Neon.Surface else Neon.Surface2,
        onClick = if (link != null) {
            { runCatching { uriHandler.openUri(link) }; Unit }
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (ev.allDay) "GANZTÄGIG" else "${ev.start.format(TIME)} – ${ev.end.format(TIME)}",
                fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Neon.Cyan,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ev.online) Tag("ONLINE", Neon.Cyan)
                if (tag != null) Tag(tag, Neon.Magenta)
            }
        }
        Text(
            ev.name.uppercase(Locale.GERMAN),
            fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 25.sp,
        )
        val meta = listOfNotNull(ev.lecturer, ev.place).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Neon.Soft)
        }
        if (link != null) {
            Text("Tippen öffnet den Online-Raum", fontFamily = Mono, fontSize = 11.sp, color = Neon.Muted)
        }
    }
}

@Composable
private fun WeekStrip(date: LocalDate, today: LocalDate, visible: List<Lecture>, onSelect: (LocalDate) -> Unit) {
    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L until 5L).map { monday.plusDays(it) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "WOCHE ${monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}",
                fontFamily = Orbitron, fontSize = 12.sp, letterSpacing = 3.sp, color = Neon.Muted,
            )
            Row {
                WeekArrow("‹", "Vorherige Woche") { onSelect(date.minusWeeks(1)) }
                WeekArrow("•", "Heute") { onSelect(today) }
                WeekArrow("›", "Nächste Woche") { onSelect(date.plusWeeks(1)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day ->
                val count = visible.count { it.date == day }
                val isSelected = day == date
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) Neon.Magenta else Neon.Bg)
                        .border(1.dp, if (isSelected) Neon.Magenta else if (day == today) Neon.Cyan else Neon.Border)
                        .clickable(role = Role.Button) { onSelect(day) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val color = if (isSelected) Neon.Bg else Neon.Text
                    Text(weekdayShort(day), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                    Text(day.format(SHORT_DATE), fontFamily = Mono, fontSize = 11.sp, color = if (isSelected) Neon.Bg else Neon.Muted)
                    Text(if (count == 0) "–" else "$count", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
                }
            }
        }
        Text("Zahl = Termine pro Tag", fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Neon.Muted)
    }
}

@Composable
private fun WeekArrow(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Neon.Cyan)
    }
}

@Composable
private fun NextOnline(visible: List<Lecture>, now: ZonedDateTime) {
    val ev = visible.firstOrNull { it.online && it.start.isAfter(now) } ?: return
    val uriHandler = LocalUriHandler.current
    val link = ev.link
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Neon.Cyan)
            .then(if (link != null) Modifier.clickable { runCatching { uriHandler.openUri(link) } } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("NÄCHSTER ONLINE-TERMIN", fontFamily = Orbitron, fontSize = 11.sp, letterSpacing = 2.sp, color = Neon.Cyan)
            Tag("ONLINE", Neon.Cyan)
        }
        Text(ev.name, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(
            "${weekdayShort(ev.date)} ${ev.start.format(SHORT_DATE_TIME)}",
            fontFamily = Mono, fontSize = 13.sp, color = Neon.Soft,
        )
    }
}

// ---------- PLAN ----------

@Composable
private fun PlanList(state: UiState) {
    val now = ZonedDateTime.now(BERLIN)
    val grouped = state.visible.filter { it.end.isAfter(now) }.groupBy { it.date }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (grouped.isEmpty()) {
            item {
                Text("Keine kommenden Termine.", fontFamily = Rajdhani, fontSize = 17.sp, color = Neon.Soft)
            }
        }
        grouped.forEach { (day, events) ->
            item {
                Text(
                    "${weekdayFull(day)} · ${day.format(SHORT_DATE)}",
                    fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp,
                    color = Neon.Muted, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
            }
            items(events) { ev -> PlanRow(ev) }
        }
    }
}

@Composable
private fun PlanRow(ev: Lecture) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Neon.Border, chamfer(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (ev.allDay) "TAG" else ev.start.format(TIME),
            fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Neon.Cyan,
            modifier = Modifier.width(52.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(ev.name, fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            val meta = listOfNotNull(
                if (ev.allDay) null else "bis ${ev.end.format(TIME)}",
                ev.place,
            ).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Neon.Soft)
        }
        if (ev.online) Tag("ONLINE", Neon.Cyan)
    }
}

// ---------- FÄCHER ----------

@Composable
private fun SubjectsView(state: UiState, vm: MainViewModel) {
    val subjects = state.subjects
    val active = subjects.count { it.title !in state.deselected }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FÄCHER", fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 2.sp)
                Text(
                    "$active / ${subjects.size} AKTIV · KURS ${state.course?.uppercase().orEmpty()}",
                    fontFamily = Mono, fontSize = 13.sp, color = Neon.Muted,
                )
                Text(
                    "Einmaltermine wie Prüfungen, Präsentationen und Einführungen werden immer angezeigt.",
                    fontFamily = Rajdhani, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Neon.Soft,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("ALLE", onClick = vm::selectAll, filled = false, modifier = Modifier.weight(1f))
                NeonButton("KEINE", onClick = vm::selectNone, filled = false, modifier = Modifier.weight(1f))
            }
        }
        if (subjects.isEmpty()) {
            item {
                Text("Noch keine Fächer geladen.", fontFamily = Rajdhani, fontSize = 17.sp, color = Neon.Soft)
            }
        }
        items(subjects) { subject ->
            SubjectRow(subject, checked = subject.title !in state.deselected, onToggle = { vm.toggle(subject.title) })
        }
    }
}

@Composable
private fun SubjectRow(subject: Subject, checked: Boolean, onToggle: () -> Unit) {
    val shape = chamfer(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, if (checked) Neon.Magenta else Neon.Border, shape)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeonCheckbox(checked)
        Column(Modifier.weight(1f)) {
            Text(
                subject.name.uppercase(Locale.GERMAN),
                fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                color = if (checked) Neon.Text else Neon.Muted,
            )
            val meta = listOfNotNull(
                subject.lecturer,
                "${subject.count} Termine",
                "${subject.first.format(SHORT_DATE)}–${subject.last.format(SHORT_DATE)}",
            ).joinToString(" · ")
            Text(meta, fontFamily = Mono, fontSize = 12.sp, color = Neon.Muted)
        }
    }
}
