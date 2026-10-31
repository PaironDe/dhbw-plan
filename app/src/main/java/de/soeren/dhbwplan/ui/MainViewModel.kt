package de.soeren.dhbwplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.soeren.dhbwplan.data.IcsParser
import de.soeren.dhbwplan.data.Lecture
import de.soeren.dhbwplan.data.PlanRepository
import de.soeren.dhbwplan.data.Subject
import de.soeren.dhbwplan.data.subjectsOf
import de.soeren.dhbwplan.data.visibleEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val course: String? = null,
    val showCoursePicker: Boolean = false,
    val events: List<Lecture> = emptyList(),
    val deselected: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val lastSync: Long = 0L,
    val changes: String? = null,
) {
    val subjects: List<Subject> by lazy { subjectsOf(events) }
    val visible: List<Lecture> by lazy { visibleEvents(events, deselected) }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PlanRepository(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val course = repo.course
        if (course != null) {
            val cached = repo.cached(course)?.let { runCatching { IcsParser.parse(it) }.getOrNull() }
            _state.value = UiState(
                course = course,
                events = cached ?: emptyList(),
                deselected = repo.deselected(course),
                lastSync = repo.lastSync,
            )
            load(course, switching = false)
        }
    }

    fun refresh() {
        val course = _state.value.course ?: return
        load(course, switching = false)
    }

    fun openCoursePicker() = _state.update { it.copy(showCoursePicker = true, error = null) }

    fun closeCoursePicker() = _state.update { it.copy(showCoursePicker = false, error = null) }

    fun setCourse(input: String) {
        val course = input.trim().lowercase().replace(" ", "")
        if (course.isEmpty()) return
        load(course, switching = true)
    }

    fun dismissChanges() = _state.update { it.copy(changes = null) }

    fun toggle(title: String) {
        val course = _state.value.course ?: return
        val current = _state.value.deselected
        val next = if (title in current) current - title else current + title
        repo.setDeselected(course, next)
        _state.update { it.copy(deselected = next) }
    }

    fun selectAll() {
        val course = _state.value.course ?: return
        repo.setDeselected(course, emptySet())
        _state.update { it.copy(deselected = emptySet()) }
    }

    fun selectNone() {
        val course = _state.value.course ?: return
        val all = _state.value.subjects.map { it.title }.toSet()
        repo.setDeselected(course, all)
        _state.update { it.copy(deselected = all) }
    }

    private fun load(course: String, switching: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val events = IcsParser.parse(repo.download(course))
                val deselected = repo.deselected(course)
                val changes = repo.checkChanges(course, events, deselected)
                repo.course = course
                _state.update {
                    it.copy(
                        course = course,
                        showCoursePicker = false,
                        events = events,
                        deselected = deselected,
                        loading = false,
                        error = null,
                        lastSync = repo.lastSync,
                        changes = changes ?: it.changes,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = if (switching) {
                            "Kurs ${course.uppercase()} nicht gefunden oder keine Verbindung."
                        } else {
                            "OFFLINE · GESPEICHERTER STAND"
                        },
                    )
                }
            }
        }
    }
}
