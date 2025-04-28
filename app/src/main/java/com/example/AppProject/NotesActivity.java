package com.example.AppProject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.Set;

public class NotesActivity extends AppCompatActivity {
    // Container for dynamically adding note views
    private LinearLayout notesContainer;
    // SharedPreferences for storing notes persistently
    private SharedPreferences sharedPreferences;
    private static final String NOTES_KEY = "notes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        notesContainer = findViewById(R.id.notesContainer);
        ImageView addNoteButton = findViewById(R.id.addNote);
        ImageView backButton = findViewById(R.id.backButton);
        sharedPreferences = getSharedPreferences("NotesApp", Context.MODE_PRIVATE);

        // Create new note
        addNoteButton.setOnClickListener(v -> openNoteEditor(""));
        // Go back to previous screen
        backButton.setOnClickListener(v -> finish());

        // Load existing notes
        loadNotes();
    }

    // Load all notes from SharedPreferences
    private void loadNotes() {
        notesContainer.removeAllViews();
        Set<String> notes = sharedPreferences.getStringSet(NOTES_KEY, new HashSet<>());
        for (String note : notes) addNoteToUI(note);
    }

    // Add a note visually to the UI
    private void addNoteToUI(String noteContent) {
        TextView noteView = new TextView(this);
        noteView.setText(noteContent);
        noteView.setPadding(20, 20, 20, 20);
        noteView.setTextColor(getResources().getColor(android.R.color.white));
        noteView.setBackground(getDrawable(R.drawable.rounded_rect_background));
        noteView.setTextSize(16);

        // Add margin between notes
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        noteView.setLayoutParams(params);

        // Edit on click
        noteView.setOnClickListener(v -> openNoteEditor(noteContent));

        // Delete on long click with confirmation
        noteView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Do you want to delete this note?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        deleteNote(noteContent);
                        loadNotes();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        notesContainer.addView(noteView);
    }

    // Remove note from storage
    private void deleteNote(String noteContent) {
        Set<String> notes = sharedPreferences.getStringSet(NOTES_KEY, new HashSet<>());
        notes.remove(noteContent);
        sharedPreferences.edit().putStringSet(NOTES_KEY, notes).apply();

        String pinned = sharedPreferences.getString("pinned_note", "");
        if (noteContent.equals(pinned)) {
            sharedPreferences.edit().remove("pinned_note").apply();
        }
    }

    // Open note editor with content (or empty string for new note)
    private void openNoteEditor(String content) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_content", content);
        startActivity(intent);
    }

    // Refresh notes when returning to activity
    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
}
