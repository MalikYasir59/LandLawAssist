package com.malikyasir.landlawassist.usersidework;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.AutoCompleteTextView;

import com.malikyasir.landlawassist.Modelss.Case;
import com.malikyasir.landlawassist.R;

import java.util.Calendar;

public class addcases extends BottomSheetDialogFragment {

    private Calendar filingDate;
    private TextInputEditText titleInput, descriptionInput, caseNumberInput, filingDateInput;
    private AutoCompleteTextView courtSpinner;
    private MaterialButton submitButton;
    private View progressBar;

    public interface CaseAddedListener {
        void onCaseAdded();
    }

    private CaseAddedListener caseAddedListener;

    public void setCaseAddedListener(CaseAddedListener listener) {
        this.caseAddedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_addcases, container, false);

        // Initialize views
        titleInput = view.findViewById(R.id.titleInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        caseNumberInput = view.findViewById(R.id.caseNumberInput);
        courtSpinner = view.findViewById(R.id.courtSpinner);
        filingDateInput = view.findViewById(R.id.filingDateInput);
        submitButton = view.findViewById(R.id.submitButton);
        progressBar = view.findViewById(R.id.progressBar);

        setupCourtSpinner();
        setupDatePicker();

        submitButton.setOnClickListener(v -> submitCase());

        return view;
    }

    private void setupCourtSpinner() {
        String[] courts = {"District Court", "High Court", "Supreme Court", "Civil Court", "Revenue Court"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, courts);
        courtSpinner.setAdapter(adapter);
    }

    private void setupDatePicker() {
        filingDate = Calendar.getInstance();
        filingDateInput.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                filingDate.set(Calendar.YEAR, year);
                filingDate.set(Calendar.MONTH, month);
                filingDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateText();
            }, filingDate.get(Calendar.YEAR), filingDate.get(Calendar.MONTH), filingDate.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private void updateDateText() {
        filingDateInput.setText(String.format("%02d/%02d/%d",
                filingDate.get(Calendar.DAY_OF_MONTH),
                filingDate.get(Calendar.MONTH) + 1,
                filingDate.get(Calendar.YEAR)));
    }

    private void submitCase() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String caseNumber = caseNumberInput.getText().toString().trim();
        String court = courtSpinner.getText().toString();

        if (!validateInput(title, description, caseNumber, court)) return;

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            showToastAndReset("Please log in to submit a case");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // Check the user's role
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String role = documentSnapshot.getString("userType");
                if (role == null) {
                    showToastAndReset("Error: User role not found");
                    return;
                }
                
                if ("User".equalsIgnoreCase(role)) {
                    saveCaseToFirestore(db, userId, title, description, caseNumber, court);
                } else {
                    showToastAndReset("Only regular users can submit cases");
                }
            })
            .addOnFailureListener(e -> {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                    showToastAndReset("You don't have permission to perform this action");
                } else {
                    showToastAndReset("Error checking user role: " + errorMessage);
                }
            });
    }

    private void saveCaseToFirestore(FirebaseFirestore db, String userId, String title, 
                                    String description, String caseNumber, String court) {
        Case newCase = new Case(userId, title, description, court);
        newCase.setCaseNumber(caseNumber);
        newCase.setFilingDate(filingDate.getTimeInMillis());
        newCase.setStatus("ACTIVE");

        db.collection("cases").add(newCase)
            .addOnSuccessListener(documentReference -> {
                newCase.setId(documentReference.getId());
                Toast.makeText(getContext(), "Case added successfully", Toast.LENGTH_SHORT).show();
                if (caseAddedListener != null) {
                    caseAddedListener.onCaseAdded();
                }
                dismiss();
            })
            .addOnFailureListener(e -> {
                showToastAndReset("Error adding case: " + e.getMessage());
            });
    }

    private boolean validateInput(String title, String description, String caseNumber, String court) {
        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            return false;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Description is required");
            return false;
        }
        if (caseNumber.isEmpty()) {
            caseNumberInput.setError("Case number is required");
            return false;
        }
        if (court.isEmpty()) {
            courtSpinner.setError("Please select a court");
            return false;
        }
        if (filingDateInput.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please select filing date", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showToastAndReset(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
    }
}
