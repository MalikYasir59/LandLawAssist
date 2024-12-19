package com.malikyasir.landlawassist.usersidework;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.malikyasir.landlawassist.Modelss.Case;
import com.malikyasir.landlawassist.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.AutoCompleteTextView;

import java.util.Calendar;

public class addcases extends BottomSheetDialogFragment {

    private Calendar filingDate;
    private TextInputEditText titleInput, descriptionInput, caseNumberInput, filingDateInput;
    private AutoCompleteTextView courtSpinner;
    private MaterialButton submitButton;
    private View progressBar;

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
        String[] courts = new String[]{"District Court", "High Court", "Supreme Court", "Civil Court", "Revenue Court"};
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
        filingDateInput.setText(String.format("%d/%d/%d", filingDate.get(Calendar.DAY_OF_MONTH), filingDate.get(Calendar.MONTH) + 1, filingDate.get(Calendar.YEAR)));
    }

    private void submitCase() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String caseNumber = caseNumberInput.getText().toString().trim();
        String court = courtSpinner.getText().toString();

        if (validateInput(title, description, caseNumber)) {
            progressBar.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            // Check if the user is authenticated
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Please log in to submit a case", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                submitButton.setEnabled(true);
                return;
            }

            // User is authenticated, continue with role check
            String userId = mAuth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            Log.d("UserRole", "User Role: " + role);

                            // Check if the user is a regular user (not admin)
                            if (role != null && role.equals("User")) {
                                Case newCase = new Case(userId, title, description, court);
                                newCase.setCaseNumber(caseNumber);
                                newCase.setFilingDate(filingDate.getTimeInMillis());

                                // Save case data to Firestore
                                FirebaseFirestore.getInstance().collection("cases").add(newCase)
                                        .addOnSuccessListener(documentReference -> {
                                            Toast.makeText(getContext(), "Case added successfully", Toast.LENGTH_SHORT).show();
                                            dismiss(); // Close the bottom sheet
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                            submitButton.setEnabled(true);
                                        });
                            } else {
                                // User is an admin, prevent case submission
                                Toast.makeText(getContext(), "Admins cannot submit cases", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                submitButton.setEnabled(true);
                            }
                        } else {
                            // Handle case where user document does not exist
                            Toast.makeText(getContext(), "User data not found. Please check your account.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            submitButton.setEnabled(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure in fetching user data
                        Toast.makeText(getContext(), "Error retrieving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        submitButton.setEnabled(true);
                    });
        }
    }

    private boolean validateInput(String title, String description, String caseNumber) {
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
        if (filingDateInput.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please select filing date", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
