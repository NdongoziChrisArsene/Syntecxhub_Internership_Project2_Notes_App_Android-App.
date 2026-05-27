package com.chrisarsene.notesapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class AddEditNoteActivity : AppCompatActivity() {

    private var noteId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_note)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etContent = findViewById<EditText>(R.id.etContent)
        val acCategory = findViewById<AutoCompleteTextView>(R.id.acCategory)
        val btnSave = findViewById<ImageButton>(R.id.btnSave)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val categories = listOf("General", "Work", "Personal", "Ideas", "Shopping", "Study")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        acCategory.setAdapter(catAdapter)

        noteId = intent.getIntExtra("noteId", -1)
        if (noteId != -1) {
            etTitle.setText(intent.getStringExtra("title"))
            etContent.setText(intent.getStringExtra("content"))
            acCategory.setText(intent.getStringExtra("category"), false)
        } else {
            acCategory.setText("General", false)
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            val category = acCategory.text.toString().trim().ifBlank { "General" }

            if (title.isBlank() && content.isBlank()) {
                finish()
                return@setOnClickListener
            }

            val result = Intent()
            result.putExtra("title", title)
            result.putExtra("content", content)
            result.putExtra("category", category)
            result.putExtra("noteId", noteId)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}
