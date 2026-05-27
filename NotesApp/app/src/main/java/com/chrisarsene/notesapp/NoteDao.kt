package com.chrisarsene.notesapp

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActive(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchived(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun search(q: String): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllForExport(): List<Note>
}
