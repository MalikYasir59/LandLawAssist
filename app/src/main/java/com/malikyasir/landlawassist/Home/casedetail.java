package com.malikyasir.landlawassist.Home;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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
import com.malikyasir.landlawassist.utils.StorageConfig;

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
    private TextView statusChip;
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
            Toast.makeText(this, "Error: Case ID not found", LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up toolbar - using the toolbar from layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Case Details");
            }
        }

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
                Toast.makeText(this, "Next hearing date updated", LENGTH_SHORT).show())
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update hearing date: " + e.getMessage(), 
                    LENGTH_SHORT).show());
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
                Toast.makeText(this, "Status updated successfully", LENGTH_SHORT).show();
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
                        LENGTH_SHORT).show();
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
        
        statusChip.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
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
                    
                    // Load status
                    String status = documentSnapshot.getString("status");
                    if (status != null) {
                        currentStatus = status;
                        updateStatusChip();
                    }
                    
                    // Load other case details...
                    TextView caseTitleView = findViewById(R.id.caseTitleText);
                    TextView courtView = findViewById(R.id.courtText);
                    
                    caseTitleView.setText(documentSnapshot.getString("title"));
                    courtView.setText(documentSnapshot.getString("court"));
                } else {
                    Toast.makeText(this, "Case not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading case details: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                finish();
            });
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
                    Toast.makeText(this, "Note added successfully", LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadNotes();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                    Toast.makeText(this, "Error adding note: " + e.getMessage(), 
                        LENGTH_SHORT).show();
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
                    LENGTH_SHORT).show());
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
            Toast.makeText(this, "Please login to upload documents", LENGTH_SHORT).show();
            return;
        }

        // Verify user is authenticated and token is valid
        currentUser.getIdToken(true)
            .addOnSuccessListener(getTokenResult -> {
                // Proceed with upload since token is valid
                proceedWithUpload(fileUri, currentUser);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Authentication error: " + e.getMessage(), LENGTH_SHORT).show();
                Log.e("CaseDetail", "Token refresh failed", e);
            });
    }

    private void proceedWithUpload(Uri fileUri, FirebaseUser currentUser) {
        String fileType = getContentResolver().getType(fileUri);
        // Check if file type is supported
        if (fileType == null || (!fileType.startsWith("application/pdf") && !fileType.startsWith("image/"))) {
            Toast.makeText(this, "Only PDF and image files are supported", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String fileName = getFileName(fileUri);
        
        try {
            // Set metadata for the file with minimal info
            StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(fileType)
                .build();
            
            // Use StorageConfig to get the reference
            String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
            StorageReference fileRef = StorageConfig.getCaseDocumentsReference(caseId)
                .child(uniqueFileName);

            // Upload file with metadata
            fileRef.putFile(fileUri, metadata)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    // Update progress if needed
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL after successful upload
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                        // Create a document object
                        Document document = new Document(
                            caseId, 
                            fileName, 
                            downloadUrl.toString(), 
                            currentUser.getUid(),
                            fileType
                        );

                        // Save document metadata to Firestore
                        db.collection("cases").document(caseId)
                            .collection("documents")
                            .add(document)
                            .addOnSuccessListener(documentReference -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Document uploaded successfully", LENGTH_SHORT).show();
                                loadDocuments();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to save document details: " + e.getMessage(), LENGTH_SHORT).show();
                                
                                // Log the error for debugging
                                Log.e("CaseDetail", "Firestore error: " + e.getMessage(), e);
                            });
                    }).addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), LENGTH_SHORT).show();
                        
                        // Log the error for debugging
                        Log.e("CaseDetail", "Download URL error: " + e.getMessage(), e);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    String errorMsg = "Upload failed: " + e.getMessage();
                    
                    // Show a more specific error message based on the error type
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("permission")) {
                            errorMsg = "Permission denied. Please check your access rights.";
                        } else if (e.getMessage().contains("network")) {
                            errorMsg = "Network error. Please check your connection.";
                        } else if (e.getMessage().contains("canceled")) {
                            errorMsg = "Upload was canceled.";
                        } else if (e.getMessage().contains("quota")) {
                            errorMsg = "Storage quota exceeded.";
                        } else if (e.getMessage().contains("412")) {
                            errorMsg = "Service account issue (Error 412). Check Firebase console.";
                        } else if (e.getMessage().contains("403")) {
                            errorMsg = "Access forbidden (Error 403). Check your Firebase rules.";
                        } else if (e.getMessage().contains("404")) {
                            errorMsg = "Resource not found (Error 404). Check storage path.";
                        }
                    }
                    
                    // Log extra details that might help debugging
                    Log.e("CaseDetail", "Upload error full details:", e);
                    Log.e("CaseDetail", "Upload path: " + fileRef.getPath());
                    Log.e("CaseDetail", "Content type: " + fileType);
                    Log.e("CaseDetail", "Auth UID: " + (currentUser != null ? currentUser.getUid() : "null"));
                    
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), LENGTH_SHORT).show();
            Log.e("CaseDetail", "Upload exception: " + e.getMessage(), e);
        }
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
                    LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}