package com.chrisarsene.notesapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onArchiveClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnPin: ImageButton = view.findViewById(R.id.btnPin)
        val btnArchive: ImageButton = view.findViewById(R.id.btnArchive)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val pinIndicator: View = view.findViewById(R.id.pinIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        holder.tvTitle.text = note.title.ifBlank { "Untitled" }
        holder.tvContent.text = note.content
        holder.tvCategory.text = note.category
        holder.tvDate.text = fmt.format(Date(note.updatedAt))
        holder.pinIndicator.visibility = if (note.isPinned) View.VISIBLE else View.GONE

        holder.btnPin.setImageResource(
            if (note.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin
        )

        holder.btnArchive.setImageResource(
            if (note.isArchived) R.drawable.ic_unarchive else R.drawable.ic_archive
        )

        holder.itemView.setOnClickListener { onNoteClick(note) }
        holder.btnPin.setOnClickListener { onPinClick(note) }
        holder.btnArchive.setOnClickListener { onArchiveClick(note) }
        holder.btnDelete.setOnClickListener { onDeleteClick(note) }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(a: Note, b: Note) = a.id == b.id
        override fun areContentsTheSame(a: Note, b: Note) = a == b
    }
}
