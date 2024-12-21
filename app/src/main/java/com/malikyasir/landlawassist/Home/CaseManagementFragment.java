package com.malikyasir.landlawassist.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.malikyasir.landlawassist.Adapters.CaseAdapter;
import com.malikyasir.landlawassist.R;
import com.malikyasir.landlawassist.usersidework.addcases;
import com.malikyasir.landlawassist.Modelss.Case;
import java.util.ArrayList;
import java.util.List;

public class CaseManagementFragment extends Fragment implements addcases.CaseAddedListener {
    private RecyclerView casesRecyclerView;
    private LinearLayout emptyStateLayout;
    private View progressBar;
    private CaseAdapter caseAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_case_management, container, false);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        casesRecyclerView = view.findViewById(R.id.casesRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        progressBar = view.findViewById(R.id.progressBar);
        FloatingActionButton addCaseFab = view.findViewById(R.id.addCaseFab);

        // Setup RecyclerView
        casesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        caseAdapter = new CaseAdapter(new ArrayList<>(), new CaseAdapter.OnCaseClickListener() {
            @Override
            public void onCaseClick(Case caseItem) {
                onCaseClicked(caseItem);
            }
        });
        casesRecyclerView.setAdapter(caseAdapter);

        // Check user role and load cases
        checkUserRoleAndLoadCases();

        addCaseFab.setOnClickListener(v -> {
            addcases fragment = new addcases();
            fragment.setCaseAddedListener(this);
            fragment.show(getChildFragmentManager(), fragment.getTag());
        });

        return view;
    }

    @Override
    public void onCaseAdded() {
        // Refresh the cases list when a new case is added
        checkUserRoleAndLoadCases();
    }

    private void checkUserRoleAndLoadCases() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                String role = documentSnapshot.getString("userType");
                if (role == null) {
                    Toast.makeText(getContext(), "User role not found", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                
                if ("User".equalsIgnoreCase(role)) {
                    loadUserCases(userId);
                } else if ("Lawyer".equalsIgnoreCase(role)) {
                    loadAllCases();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Invalid user role", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadUserCases(String userId) {
        db.collection("cases")
            .whereEqualTo("userId", userId)
            .orderBy("filingDate", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                progressBar.setVisibility(View.GONE);
                if (error != null) {
                    String errorMessage = error.getMessage();
                    if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                        Toast.makeText(getContext(), 
                            "You don't have permission to access these cases", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), 
                            "Error loading cases: " + errorMessage, 
                            Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                if (value != null) {
                    List<Case> cases = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Case caseItem = doc.toObject(Case.class);
                        if (caseItem != null) {
                            caseItem.setId(doc.getId());
                            cases.add(caseItem);
                        }
                    }
                    updateCasesList(cases);
                }
            });
    }

    private void loadAllCases() {
        db.collection("cases")
            .orderBy("filingDate", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                progressBar.setVisibility(View.GONE);
                if (error != null) {
                    String errorMessage = error.getMessage();
                    if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                        Toast.makeText(getContext(), 
                            "You don't have permission to access all cases", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), 
                            "Error loading cases: " + errorMessage, 
                            Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                if (value != null) {
                    List<Case> cases = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Case caseItem = doc.toObject(Case.class);
                        if (caseItem != null) {
                            caseItem.setId(doc.getId());
                            cases.add(caseItem);
                        }
                    }
                    updateCasesList(cases);
                }
            });
    }

    private void updateCasesList(List<Case> cases) {
        if (cases.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            casesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            casesRecyclerView.setVisibility(View.VISIBLE);
            caseAdapter.updateCases(cases);
        }
    }

    private void onCaseClicked(Case caseItem) {
        Intent intent = new Intent(getActivity(), casedetail.class);
        intent.putExtra("caseId", caseItem.getId());
        startActivity(intent);
    }
}