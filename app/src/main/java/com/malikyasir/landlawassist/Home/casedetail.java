package com.malikyasir.landlawassist.Home;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.malikyasir.landlawassist.Modelss.Document;
import com.malikyasir.landlawassist.Modelss.Note;
import com.malikyasir.landlawassist.R;
import com.malikyasir.landlawassist.Adapters.DocumentAdapter;
import com.malikyasir.landlawassist.Adapters.NotesAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class casedetail extends AppCompatActivity {

    private TextView nextHearingTextView;
    private Calendar selectedDate;
    private String caseId;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    private com.google.android.material.chip.Chip statusChip;
    private MaterialButton editStatusButton;
    private String currentStatus = "ACTIVE";
    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private RecyclerView documentsRecyclerView;
    private DocumentAdapter documentAdapter;
    private FirebaseStorage storage;
    private static final int PICK_FILE_REQUEST = 1;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_casedetail);

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        
        // Initialize date formatter
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Get case ID from intent
        caseId = getIntent().getStringExtra("caseId");
        if (caseId == null) {
            Toast.makeText(this, "Error: Case ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Case Details");

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        nextHearingTextView = findViewById(R.id.nextHearing);
        MaterialButton addHearingButton = findViewById(R.id.addHearingButton);
        statusChip = findViewById(R.id.statusChip);
        editStatusButton = findViewById(R.id.editStatusButton);

        // Initialize calendar
        selectedDate = Calendar.getInstance();

        // Set up add hearing button click listener
        addHearingButton.setOnClickListener(v -> showDatePickerDialog());
        editStatusButton.setOnClickListener(v -> showStatusDialog());

        // Load case details
        loadCaseDetails();

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesAdapter = new NotesAdapter(new ArrayList<>(), caseId);
        notesRecyclerView.setAdapter(notesAdapter);

        MaterialButton addNoteButton = findViewById(R.id.addNoteButton);
        addNoteButton.setOnClickListener(v -> showAddNoteDialog());

        // Load notes
        loadNotes();

        storage = FirebaseStorage.getInstance();
        documentsRecyclerView = findViewById(R.id.documentsRecyclerView);
        documentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        documentAdapter = new DocumentAdapter(this, new ArrayList<>(), caseId);
        documentsRecyclerView.setAdapter(documentAdapter);

        MaterialButton uploadDocButton = findViewById(R.id.uploadDocumentButton);
        uploadDocButton.setOnClickListener(v -> pickFile());

        // Load documents
        loadDocuments();
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateHearingDate();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateHearingDate() {
        String formattedDate = dateFormat.format(selectedDate.getTime());
        
        // Update UI
        nextHearingTextView.setText(formattedDate);

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("nextHearingDate", selectedDate.getTimeInMillis());

        db.collection("cases").document(caseId)
            .update(updates)
            .addOnSuccessListener(aVoid -> 
                Toast.makeText(this, "Next hearing date updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update hearing date: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
    }

    private void showStatusDialog() {
        String[] statuses = {"ACTIVE", "PENDING", "CLOSED"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Status")
               .setItems(statuses, (dialog, which) -> {
                   String newStatus = statuses[which];
                   updateCaseStatus(newStatus);
               });
        builder.create().show();
    }

    private void updateCaseStatus(String newStatus) {
        if (caseId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        // Show progress indicator
        View progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("cases").document(caseId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                currentStatus = newStatus;
                updateStatusChip();
                Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show();
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                    Toast.makeText(this, 
                        "You don't have permission to update the status", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, 
                        "Failed to update status: " + errorMessage, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateStatusChip() {
        statusChip.setText(currentStatus);
        int backgroundColor;
        int textColor = getResources().getColor(android.R.color.white);
        
        switch (currentStatus) {
            case "ACTIVE":
                backgroundColor = getResources().getColor(R.color.status_active);
                break;
            case "PENDING":
                backgroundColor = getResources().getColor(R.color.status_pending);
                break;
            case "CLOSED":
                backgroundColor = getResources().getColor(R.color.status_closed);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.status_default);
        }
        
        statusChip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        statusChip.setTextColor(textColor);
    }

    private void loadCaseDetails() {
        db.collection("cases").document(caseId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Load next hearing date if exists
                    Long nextHearingTimestamp = documentSnapshot.getLong("nextHearingDate");
                    if (nextHearingTimestamp != null) {
                        selectedDate.setTimeInMillis(nextHearingTimestamp);
                        nextHearingTextView.setText(dateFormat.format(selectedDate.getTime()));
                    }
                    
                    // Load other case details...
                    TextView caseNumberView = findViewById(R.id.caseNumber);
                    TextView courtView = findViewById(R.id.court);
                    TextView descriptionView = findViewById(R.id.description);
                    
                    caseNumberView.setText(documentSnapshot.getString("caseNumber"));
                    courtView.setText(documentSnapshot.getString("court"));
                    descriptionView.setText(documentSnapshot.getString("description"));
                    
                    // Load status
                    currentStatus = documentSnapshot.getString("status");
                    if (currentStatus == null) {
                        currentStatus = "ACTIVE"; // Default status
                    }
                    updateStatusChip();
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error loading case details: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
    }

    private void showAddNoteDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_note, null);
        TextInputEditText noteInput = view.findViewById(R.id.noteInput);
        MaterialButton submitButton = view.findViewById(R.id.submitButton);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .create();

        submitButton.setOnClickListener(v -> {
            String content = noteInput.getText().toString().trim();
            if (content.isEmpty()) {
                noteInput.setError("Note cannot be empty");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Note note = new Note(caseId, content, userId);

            db.collection("cases").document(caseId)
                .collection("notes")
                .add(note)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Note added successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadNotes();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Error adding note: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        });

        dialog.show();
    }

    private void loadNotes() {
        db.collection("cases").document(caseId)
            .collection("notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Note> notes = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Note note = doc.toObject(Note.class);
                    note.setId(doc.getId());
                    notes.add(note);
                }
                notesAdapter.updateNotes(notes);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error loading notes: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
    }

    private void pickFile() {
        // Show file type info dialog
        new AlertDialog.Builder(this)
            .setTitle("Upload Document")
            .setMessage("Only PDF and PNG files are supported")
            .setPositiveButton("Choose File", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/*|image/png");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "application/pdf",
                    "image/png"
                });
                startActivityForResult(intent, PICK_FILE_REQUEST);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadFile(data.getData());
        }
    }

    private void uploadFile(Uri fileUri) {
        if (fileUri == null) return;

        // Check authentication first
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to upload documents", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileType = getContentResolver().getType(fileUri);
        // Check if file type is supported
        if (fileType == null || (!fileType.equals("application/pdf") && !fileType.equals("image/png"))) {
            Toast.makeText(this, "Only PDF and PNG files are supported", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String fileName = getFileName(fileUri);
        
        // Simplified storage path
        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
            .child("documents")
            .child(System.currentTimeMillis() + "_" + fileName);

        // Upload file
        fileRef.putFile(fileUri)
            .addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                // Update progress if needed
            })
            .addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                    Document document = new Document(
                        caseId, 
                        fileName, 
                        downloadUrl.toString(), 
                        currentUser.getUid(),
                        fileType
                    );

                    db.collection("cases").document(caseId)
                        .collection("documents")
                        .add(document)
                        .addOnSuccessListener(documentReference -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show();
                            loadDocuments();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Failed to save document details: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        });
                });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("permission")) {
                    Toast.makeText(this, "Permission denied. Please check your access rights.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void loadDocuments() {
        db.collection("cases").document(caseId)
            .collection("documents")
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Document> documents = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Document document = doc.toObject(Document.class);
                    document.setId(doc.getId());
                    documents.add(document);
                }
                documentAdapter.updateDocuments(documents);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error loading documents: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
    }
}