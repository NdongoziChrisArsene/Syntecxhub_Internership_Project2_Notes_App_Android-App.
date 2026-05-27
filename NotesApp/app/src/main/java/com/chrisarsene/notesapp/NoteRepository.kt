package com.chrisarsene.notesapp

import android.content.Context
import androidx.lifecycle.LiveData

class NoteRepository(context: Context) {

    private val dao = NoteDatabase.getInstance(context).noteDao()

    val allActive: LiveData<List<Note>> = dao.getAllActive()
    val archived: LiveData<List<Note>> = dao.getArchived()

    fun search(query: String): LiveData<List<Note>> = dao.search(query)

    suspend fun insert(note: Note) = dao.insert(note)

    suspend fun update(note: Note) = dao.update(note)

    suspend fun delete(note: Note) = dao.delete(note)

    suspend fun getById(id: Int) = dao.getById(id)

    suspend fun getAllForExport() = dao.getAllForExport()

    suspend fun togglePin(note: Note) = dao.update(note.copy(isPinned = !note.isPinned))

    suspend fun toggleArchive(note: Note) = dao.update(note.copy(isArchived = !note.isArchived))
}
