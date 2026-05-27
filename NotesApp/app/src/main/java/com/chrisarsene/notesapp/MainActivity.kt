package com.chrisarsene.notesapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: NoteViewModel
    private lateinit var adapter: NoteAdapter
    private var showingArchive = false

    private val newNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val title = data.getStringExtra("title") ?: ""
            val content = data.getStringExtra("content") ?: ""
            val category = data.getStringExtra("category") ?: "General"
            val noteId = data.getIntExtra("noteId", -1)

            if (noteId == -1) {
                viewModel.insert(Note(title = title, content = content, category = category))
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val existing = viewModel.getAllForExport().firstOrNull { it.id == noteId }
                    existing?.let {
                        viewModel.update(it.copy(
                            title = title,
                            content = content,
                            category = category,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }
            }
        }
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { exportToUri(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repo = NoteRepository(this)
        val factory = NoteViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        adapter = NoteAdapter(
            onNoteClick = { note ->
                val intent = Intent(this, AddEditNoteActivity::class.java)
                intent.putExtra("noteId", note.id)
                intent.putExtra("title", note.title)
                intent.putExtra("content", note.content)
                intent.putExtra("category", note.category)
                newNoteLauncher.launch(intent)
            },
            onPinClick = { viewModel.togglePin(it) },
            onArchiveClick = { viewModel.toggleArchive(it) },
            onDeleteClick = { note ->
                AlertDialog.Builder(this)
                    .setTitle("Delete note")
                    .setMessage("Are you sure you want to delete \"${note.title}\"?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.delete(note) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        viewModel.notes.observe(this) { notes ->
            if (!showingArchive) adapter.submitList(notes)
        }

        viewModel.archived.observe(this) { notes ->
            if (showingArchive) adapter.submitList(notes)
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            newNoteLauncher.launch(Intent(this, AddEditNoteActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search notes…"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                viewModel.setQuery(q)
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_archive -> {
                showingArchive = !showingArchive
                item.title = if (showingArchive) "Active notes" else "Archived"
                if (showingArchive) {
                    viewModel.archived.value?.let { adapter.submitList(it) }
                } else {
                    viewModel.notes.value?.let { adapter.submitList(it) }
                }
                true
            }
            R.id.action_export -> {
                exportLauncher.launch("notes_export.json")
                true
            }
            R.id.action_import -> {
                importLauncher.launch(arrayOf("application/json"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportToUri(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val notes = viewModel.getAllForExport()
            val array = JSONArray()
            notes.forEach { note ->
                val obj = JSONObject()
                obj.put("id", note.id)
                obj.put("title", note.title)
                obj.put("content", note.content)
                obj.put("category", note.category)
                obj.put("isPinned", note.isPinned)
                obj.put("isArchived", note.isArchived)
                obj.put("createdAt", note.createdAt)
                obj.put("updatedAt", note.updatedAt)
                array.put(obj)
            }

            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(array.toString(2).toByteArray())
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${notes.size} notes exported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stream = contentResolver.openInputStream(uri) ?: return@launch
                val text = BufferedReader(InputStreamReader(stream)).readText()
                val array = JSONArray(text)
                var count = 0

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val note = Note(
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        category = obj.optString("category", "General"),
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    viewModel.insert(note)
                    count++
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "$count notes imported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
