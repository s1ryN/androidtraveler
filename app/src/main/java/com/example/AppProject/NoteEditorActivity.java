package com.example.AppProject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.Set;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText noteEditor; // The EditText where the user types or edits their note
    private SharedPreferences sharedPreferences; // Storage for notes and pinned note
    private String originalNote; // The note's original content (before edits)
    private boolean isExistingNote = false; // Flag: is this an existing note?
    private boolean isPinned = false; // Flag: is this note pinned?
    private ImageView pinButton; // Button used to pin or unpin the note

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Initialize views
        noteEditor = findViewById(R.id.noteEditor);
        ImageView backButton = findViewById(R.id.backButton);
        ImageView deleteButton = findViewById(R.id.deleteButton);
        pinButton = findViewById(R.id.pinButton);

        sharedPreferences = getSharedPreferences("NotesApp", Context.MODE_PRIVATE);
        originalNote = getIntent().getStringExtra("note_content"); // Load note passed in intent

        // If editing an existing note, set text and flag
        if (originalNote != null && !originalNote.isEmpty()) {
            noteEditor.setText(originalNote);
            isExistingNote = true;
        }

        // Check if this note is the pinned one and update UI accordingly
        String pinnedNote = sharedPreferences.getString("pinned_note", "");
        if (originalNote != null && originalNote.equals(pinnedNote)) {
            isPinned = true;
            pinButton.setImageResource(R.drawable.ic_pin_filled);
        }

        // Autosave the note when user types
        noteEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoSaveNote(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Handle button clicks
        backButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDelete());
        pinButton.setOnClickListener(v -> togglePin());
    }

    // Save the current note text to SharedPreferences
    private void autoSaveNote(String text) {
        Set<String> notes = sharedPreferences.getStringSet("notes", new HashSet<>());
        if (isExistingNote) notes.remove(originalNote); // Remove old version
        if (!text.isEmpty()) notes.add(text); // Add new version
        sharedPreferences.edit().putStringSet("notes", notes).apply();
        originalNote = text;
        isExistingNote = true;

        // Also update pinned note if this one is pinned
        if (isPinned) {
            sharedPreferences.edit().putString("pinned_note", text).apply();
        }
    }

    // Toggle the pinned state of the note
    private void togglePin() {
        String currentText = noteEditor.getText().toString().trim();
        if (currentText.isEmpty()) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isPinned) {
            editor.remove("pinned_note");
            pinButton.setImageResource(R.drawable.ic_pin_outline);
        } else {
            editor.putString("pinned_note", currentText);
            pinButton.setImageResource(R.drawable.ic_pin_filled);
        }

        isPinned = !isPinned;
        editor.apply();
    }

    // Ask user to confirm deletion of this note
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", (dialog, which) -> deleteNote())
                .setNegativeButton("No", null)
                .show();
    }

    // Remove this note from storage (and unpin it if pinned)
    private void deleteNote() {
        Set<String> notes = sharedPreferences.getStringSet("notes", new HashSet<>());
        notes.remove(originalNote);
        sharedPreferences.edit().putStringSet("notes", notes).apply();

        String pinned = sharedPreferences.getString("pinned_note", "");
        if (originalNote.equals(pinned)) {
            sharedPreferences.edit().remove("pinned_note").apply();
        }

        finish();
    }
}
