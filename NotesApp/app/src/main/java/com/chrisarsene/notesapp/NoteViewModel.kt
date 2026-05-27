package com.chrisarsene.notesapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NoteViewModel(private val repo: NoteRepository) : ViewModel() {

    private val searchQuery = MutableLiveData<String?>()

    val notes: LiveData<List<Note>> = searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) repo.allActive else repo.search(query)
    }

    val archived: LiveData<List<Note>> = repo.archived

    init {
        searchQuery.value = null
    }

    fun setQuery(q: String?) {
        searchQuery.value = q
    }

    fun insert(note: Note) = viewModelScope.launch { repo.insert(note) }

    fun update(note: Note) = viewModelScope.launch { repo.update(note) }

    fun delete(note: Note) = viewModelScope.launch { repo.delete(note) }

    fun togglePin(note: Note) = viewModelScope.launch { repo.togglePin(note) }

    fun toggleArchive(note: Note) = viewModelScope.launch { repo.toggleArchive(note) }

    suspend fun getAllForExport() = repo.getAllForExport()
}
