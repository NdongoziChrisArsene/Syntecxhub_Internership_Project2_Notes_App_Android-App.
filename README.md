# Notes App

A clean, offline-first Android notes app built with Room, LiveData, and ViewModel. Notes are searchable, categorised, pinnable, and exportable — with a smooth, diff-driven UI that only redraws what actually changed.

---

## Architecture

The app follows standard Android MVVM with a Room-backed repository layer.

```
UI (Activity / Fragment)
    └── NoteViewModel          ← LiveData + switchMap
        └── NoteDao            ← Room queries
            └── NoteDatabase   ← SQLite via Room
```

---

## Core Files

### `Note.kt` — Data Model

The `Note` entity has 8 fields:

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Auto-generated primary key |
| `title` | `String` | — |
| `content` | `String` | — |
| `category` | `String` | Defaults to `"General"` |
| `isPinned` | `Boolean` | — |
| `isArchived` | `Boolean` | — |
| `createdAt` | `Long` | — |
| `updatedAt` | `Long` | Drives sort order — recently edited notes float to the top |

---

### `NoteDao.kt` — Database Access

- **`getAllActive()`** returns `LiveData<List<Note>>` so the UI updates automatically whenever the underlying data changes — no manual refresh required.
- **Search** uses SQL `LIKE` with `%` wildcards on both `title` and `content`, so a single query matches partial text in either field.
- **`getAllForExport()`** is a `suspend` function that returns a plain `List<Note>` — appropriate for the JSON export flow where you need a one-shot snapshot rather than an observable stream.

---

### `NoteDatabase.kt` — Room Database

- **`MIGRATION_1_2`** adds the `category` and `updatedAt` columns to existing installations without wiping user data.
- The singleton `instance` is marked `@Volatile` and created inside a `synchronized` block to prevent two threads from accidentally instantiating two separate databases simultaneously.

---

### `NoteViewModel.kt` — UI State

Uses `switchMap` on a `MutableLiveData<String?>` search query:

- When the query is **`null`** → observes `allActive` (all non-archived notes).
- When the query **has a value** → switches to `search(query)`.

The UI never has to manually swap between data sources; it simply observes a single `LiveData` that routes itself.

---

### `NoteAdapter.kt` — RecyclerView

Uses `ListAdapter` with `DiffUtil` instead of a plain `RecyclerView.Adapter`.

`DiffUtil` compares old and new lists on a background thread and only animates the items that actually changed — noticeably smoother than blanket `notifyDataSetChanged()` calls, especially for large lists.

---

## Export / Import

| | Detail |
|---|---|
| **Format** | Pretty-printed JSON |
| **Export** | `exportToUri()` serialises all notes and writes to a user-chosen location |
| **Import** | `importFromUri()` reads JSON back and inserts each note |
| **Threading** | Both operations run on `Dispatchers.IO` — the main thread is never blocked |
| **File picker** | Uses `ActivityResultContracts` (the modern replacement for `startActivityForResult`) |

---

## Requirements

- Android Studio Hedgehog or later
- Min SDK: 26
- Kotlin 1.9+
- Room 2.x

---

## Getting Started

1. Clone the repository.
2. Open in Android Studio.
3. Build and run on a device or emulator (API 26+).

No additional configuration needed — the database is created automatically on first launch.
