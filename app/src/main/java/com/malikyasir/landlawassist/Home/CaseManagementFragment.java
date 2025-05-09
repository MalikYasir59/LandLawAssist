package com.malikyasir.landlawassist.Home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.malikyasir.landlawassist.Activity.ClientCasesActivity;
import com.malikyasir.landlawassist.Adapters.CaseAdapter;
import com.malikyasir.landlawassist.R;
import com.malikyasir.landlawassist.usersidework.addcases;
import com.malikyasir.landlawassist.Modelss.Case;
import com.malikyasir.landlawassist.Modelss.LawyerRequest;
import com.malikyasir.landlawassist.Modelss.User;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import com.malikyasir.landlawassist.Adapters.ClientAdapter;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;

public class
CaseManagementFragment extends Fragment implements addcases.CaseAddedListener {
    private static final String TAG = "CaseManagementFragment";
    private RecyclerView casesRecyclerView;
    private LinearLayout emptyStateLayout;
    private View progressBar;
    private CaseAdapter caseAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentStatus = "ACTIVE"; // Default status
    private com.google.android.material.chip.ChipGroup statusChipGroup;
    private RecyclerView clientsRecyclerView;
    private TextView noClientsText;
    private String userType;
    private List<LawyerRequest> acceptedClients = new ArrayList<>();
    private ClientAdapter clientAdapter;
    private List<User> clientUsers = new ArrayList<>();
    private String selectedClientId = null;
    private TextView clientSectionTitle;
    private View casesContainer;

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
        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView);
        noClientsText = view.findViewById(R.id.noClientsText);
        clientSectionTitle = view.findViewById(R.id.clientSectionTitle);
        casesContainer = view.findViewById(R.id.casesContainer);

        // Initialize ChipGroup
        statusChipGroup = view.findViewById(R.id.statusChipGroup);
        setupStatusChips();

        // Setup Cases RecyclerView
        casesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        caseAdapter = new CaseAdapter(new ArrayList<>(), new CaseAdapter.OnCaseClickListener() {
            @Override
            public void onCaseClick(Case caseItem) {
                onCaseClicked(caseItem);
            }

            @Override
            public void onCaseDelete(Case caseItem, int position) {
                showDeleteCaseDialog(caseItem);
            }
        });
        casesRecyclerView.setAdapter(caseAdapter);

        // Get userType from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", getContext().MODE_PRIVATE);
        userType = prefs.getString("user_type", "User");
        
        if ("Lawyer".equalsIgnoreCase(userType)) {
            // Setup for lawyer: Show list of clients first
            setupForLawyer(view);
        } else {
            // Setup for client: Show their cases directly
            setupForClient();
        }

        addCaseFab.setOnClickListener(v -> {
            addcases fragment = new addcases();
            fragment.setCaseAddedListener(this);
            fragment.show(getChildFragmentManager(), fragment.getTag());
        });

        return view;
    }

    private void setupForLawyer(View view) {
        Log.d(TAG, "Setting up view for lawyer");
        
        // Show clients section
        if (clientSectionTitle != null) clientSectionTitle.setVisibility(View.VISIBLE);
        if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.VISIBLE);
        
        // Hide cases initially until a client is selected
        if (casesContainer != null) casesContainer.setVisibility(View.GONE);
        
        // Setup clients RecyclerView
        clientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        clientAdapter = new ClientAdapter(getContext(), clientUsers, client -> {
            // When a client is clicked, navigate to ClientCasesActivity
            Intent intent = new Intent(getContext(), ClientCasesActivity.class);
            intent.putExtra("clientId", client.getId());
            intent.putExtra("clientName", client.getFullName());
            startActivity(intent);
        });
        clientsRecyclerView.setAdapter(clientAdapter);
        
        // Load clients
        loadAcceptedClientsForCaseManagement();
    }

    private void setupForClient() {
        Log.d(TAG, "Setting up view for client");
        
        // Hide clients section
        if (clientSectionTitle != null) clientSectionTitle.setVisibility(View.GONE);
        if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.GONE);
        if (noClientsText != null) noClientsText.setVisibility(View.GONE);
        
        // Show cases
        if (casesContainer != null) casesContainer.setVisibility(View.VISIBLE);
        
        // Load client's cases
        checkUserRoleAndLoadCases();
    }

    private void setupStatusChips() {
        statusChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Clear current cases before loading new ones
            if (caseAdapter != null) {
                caseAdapter.updateCases(new ArrayList<>());
            }

            if (checkedId == R.id.chipActive) {
                currentStatus = "ACTIVE";
            } else if (checkedId == R.id.chipPending) {
                currentStatus = "PENDING";
            } else if (checkedId == R.id.chipClosed) {
                currentStatus = "CLOSED";
            }
            
            // If a client is selected (lawyer view) or we're a client
            if (selectedClientId != null) {
                loadClientCases(selectedClientId);
            } else {
                checkUserRoleAndLoadCases();
            }
        });

        // Set default selection
        statusChipGroup.check(R.id.chipActive);
    }

    @Override
    public void onCaseAdded() {
        // Refresh the cases list when a new case is added
        if (selectedClientId != null) {
            loadClientCases(selectedClientId);
        } else {
            checkUserRoleAndLoadCases();
        }
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
        // Clear previous cases when switching status
        if (caseAdapter != null) {
            caseAdapter.updateCases(new ArrayList<>());
        }

        db.collection("cases")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", currentStatus)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                List<Case> cases = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Case caseItem = doc.toObject(Case.class);
                    if (caseItem != null) {
                        caseItem.setId(doc.getId());
                        cases.add(caseItem);
                    }
                }
                updateCasesList(cases);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), 
                    "Error loading cases: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void loadAllCases() {
        // Clear previous cases when switching status
        if (caseAdapter != null) {
            caseAdapter.updateCases(new ArrayList<>());
        }

        db.collection("cases")
            .whereEqualTo("status", currentStatus)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                List<Case> cases = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Case caseItem = doc.toObject(Case.class);
                    if (caseItem != null) {
                        caseItem.setId(doc.getId());
                        cases.add(caseItem);
                    }
                }
                updateCasesList(cases);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), 
                    "Error loading cases: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
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

    private void showDeleteCaseDialog(Case caseItem) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Case")
            .setMessage("Are you sure you want to delete this case? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteCase(caseItem);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteCase(Case caseItem) {
        if (caseItem.getId() == null) return;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Delete all notes first
        db.collection("cases").document(caseItem.getId())
            .collection("notes")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                // Create a batch to delete all notes
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    batch.delete(document.getReference());
                }
                
                // Add case deletion to batch
                batch.delete(db.collection("cases").document(caseItem.getId()));

                // Commit the batch
                batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Case deleted successfully", Toast.LENGTH_SHORT).show();
                        if (selectedClientId != null) {
                            loadClientCases(selectedClientId);
                        } else {
                            checkUserRoleAndLoadCases();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), 
                            "Failed to delete case: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), 
                    "Failed to delete case notes: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void loadAcceptedClientsForCaseManagement() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in case
            if (noClientsText != null) {
                noClientsText.setVisibility(View.VISIBLE);
                noClientsText.setText("Please log in to view your clients");
            }
            if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.GONE);
            return;
        }
        
        String lawyerId = currentUser.getUid();
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("lawyerRequests")
            .whereEqualTo("lawyerId", lawyerId)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> clientIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String clientId = doc.getString("userId");
                    if (clientId != null) clientIds.add(clientId);
                }
                
                Log.d(TAG, "Found " + clientIds.size() + " accepted clients");
                
                if (clientIds.isEmpty()) {
                    if (noClientsText != null) noClientsText.setVisibility(View.VISIBLE);
                    if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.GONE);
                    clientUsers.clear();
                    if (clientAdapter != null) clientAdapter.updateClients(clientUsers);
                    progressBar.setVisibility(View.GONE);
                } else {
                    if (noClientsText != null) noClientsText.setVisibility(View.GONE);
                    if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Fetch user info for each client
                    clientUsers.clear();
                    final int[] clientsLoaded = {0};  // Counter to track when all clients are loaded
                    
                    for (String clientId : clientIds) {
                        db.collection("users").document(clientId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                clientsLoaded[0]++;
                                User user = userDoc.toObject(User.class);
                                if (user != null) {
                                    user.setId(userDoc.getId());
                                    clientUsers.add(user);
                                    Log.d(TAG, "Loaded client: " + user.getFullName());
                                }
                                
                                // Update adapter when all clients are loaded
                                if (clientsLoaded[0] == clientIds.size()) {
                                    if (clientAdapter != null) clientAdapter.updateClients(clientUsers);
                                    progressBar.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                clientsLoaded[0]++;
                                Log.e(TAG, "Error loading client: " + e.getMessage());
                                
                                // Update adapter when all clients are loaded (even with failures)
                                if (clientsLoaded[0] == clientIds.size()) {
                                    if (clientAdapter != null) clientAdapter.updateClients(clientUsers);
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching client requests: " + e.getMessage());
                Toast.makeText(getContext(), "Error loading clients: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadClientCases(String clientId) {
        if (clientId == null) {
            Toast.makeText(getContext(), "No client selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Only show cases for the selected client with the current status filter
        db.collection("cases")
            .whereEqualTo("userId", clientId)
            .whereEqualTo("status", currentStatus)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                List<Case> cases = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    Case caseItem = doc.toObject(Case.class);
                    if (caseItem != null) {
                        caseItem.setId(doc.getId());
                        cases.add(caseItem);
                    }
                }
                updateCasesList(cases);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error loading cases: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}