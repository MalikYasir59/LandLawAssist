package com.malikyasir.landlawassist.Home;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.CompositePageTransformer;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.malikyasir.landlawassist.Adapter.LawyerAdapter;
import com.malikyasir.landlawassist.Adapters.ClientAdapter;
import com.malikyasir.landlawassist.Models.Lawyer;
import com.malikyasir.landlawassist.Modelss.LawyerRequest;
import com.malikyasir.landlawassist.Modelss.User;
import com.malikyasir.landlawassist.R;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import android.content.SharedPreferences;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Random;

import android.widget.EditText;

public class Homefragment extends Fragment {
    private TextView userName;
    private CircleImageView profileImage;
    private TextView notificationBadge;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ViewPager2 lawyerViewPager;
    private RecyclerView clientsRecyclerView;
    private TextView noClientsText;
    private View featuredLawyersSection;
    private String userType;
    private ClientAdapter clientAdapter;
    private List<User> clientUsers = new ArrayList<>();
    private ListView cityListView;
    private List<String> cityList = new ArrayList<>();
    private RecyclerView myLawyersRecyclerView;
    private List<User> myLawyers = new ArrayList<>();
    private LawyerAdapter myLawyersAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Initialize views
        userName = view.findViewById(R.id.userName);
        
        profileImage = view.findViewById(R.id.profileImage);

        lawyerViewPager = view.findViewById(R.id.lawyerViewPager);
        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView);
        noClientsText = view.findViewById(R.id.noClientsText);
        featuredLawyersSection = view.findViewById(R.id.featuredLawyersSection);
        cityListView = view.findViewById(R.id.cityListView);
        myLawyersRecyclerView = view.findViewById(R.id.myLawyersRecyclerView);

        // Initialize Firebase and load data
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserData();
        loadUserProfile();

        // Get userType from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        userType = prefs.getString("user_type", "User");

        // Hide profile completion card for all users
        View searchCard = view.findViewById(R.id.searchCard);
        if (searchCard != null) {
            searchCard.setVisibility(View.VISIBLE);
        }
        
        // Setup search functionality
        EditText searchInput = view.findViewById(R.id.searchLawyersInput);
        if (searchInput != null) {
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchLawyersByName(query);
                    }
                    return true;
                }
                return false;
            });
        }
        
        // Setup message button
        View messageButton = view.findViewById(R.id.messageButton);
        if (messageButton != null) {
            messageButton.setOnClickListener(v -> {
                // Launch MessagingActivity instead of dialogs
                Intent intent = new Intent(getActivity(), com.malikyasir.landlawassist.Activity.MessagingActivity.class);
                startActivity(intent);
            });
        }
        
        // Find Lawyers card removed from layout

        if ("Lawyer".equalsIgnoreCase(userType)) {
            // Hide featured lawyers and show messages
            if (featuredLawyersSection != null) featuredLawyersSection.setVisibility(View.GONE);
            // Show accepted clients
            clientAdapter = new ClientAdapter(getContext(), clientUsers, client -> {
                // Navigate to client detail or chat
                // TODO: implement client click action
            });
            if (clientsRecyclerView != null) {
                clientsRecyclerView.setAdapter(clientAdapter);
                clientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                clientsRecyclerView.setVisibility(View.VISIBLE);
            }
            loadAcceptedClients();
        } else {
            // User: show featured lawyers, hide clients
            if (featuredLawyersSection != null) featuredLawyersSection.setVisibility(View.VISIBLE);
            if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.GONE);
            if (noClientsText != null) noClientsText.setVisibility(View.GONE);
            
            setupLawyerSlider();
        }



        // Fetch unique cities from Firestore
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Set<String> uniqueCities = new HashSet<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String city = doc.getString("city");
                    if (city != null && !city.isEmpty()) uniqueCities.add(city);
                }
                cityList.clear();
                cityList.add("All Cities");
                cityList.addAll(uniqueCities);
                ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, cityList);
                cityListView.setAdapter(cityAdapter);
            });

        cityListView.setOnItemClickListener((parent, v, position, id) -> {
            String selectedCity = cityList.get(position);
            if (selectedCity.equals("All Cities")) {
                loadAllLawyers();
            } else {
                loadLawyersByCity(selectedCity);
            }
        });

        setupCityFilter();
        loadAllLawyers();

        myLawyersAdapter = new LawyerAdapter(getContext(), myLawyers);
        myLawyersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        myLawyersRecyclerView.setAdapter(myLawyersAdapter);
        loadAcceptedLawyersForClient();

        return view;
    }

    private void loadUserData() {
        if (!isAdded()) return;

        Context context = getContext();
        if (context == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!isAdded() || context == null) return;

                    if (document.exists()) {
                        String imageUrl = document.getString("profileImage");
                        if (imageUrl != null && !imageUrl.isEmpty() && profileImage != null) {
                            try {
                                Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.profileuser)
                                    .error(R.drawable.profileuser)
                                    .into(profileImage);
                            } catch (Exception e) {
                                Log.e("HomeFragment", "Error loading image", e);
                            }
                        }

                        // Update user name with improved logging
                        String fullName = document.getString("fullName");
                        if (userName != null && fullName != null) {
                            userName.setText(fullName);
                            Log.d("HomeFragment", "Set user name to: " + fullName);
                        } else {
                            Log.e("HomeFragment", "Could not set user name. userName: " + 
                                   (userName != null ? "not null" : "null") + 
                                   ", fullName: " + (fullName != null ? fullName : "null"));
                            
                            // Fallback: try to find userName view by ID if userName is null
                            if (userName == null && getView() != null) {
                                TextView userNameView = getView().findViewById(R.id.userName);
                                if (userNameView != null && fullName != null) {
                                    userNameView.setText(fullName);
                                    Log.d("HomeFragment", "Set userName (fallback) to: " + fullName);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading user data", e);
                });
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in case
            return;
        }
        
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                // No UI update for profile completion anymore
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupLawyerSlider() {
        Log.d("LawyerSlider", "Setting up lawyer slider with sample data");
        
        // Initialize the lawyers list if it's null
        List<com.malikyasir.landlawassist.Models.Lawyer> featuredLawyers = new ArrayList<>();
        
        // Add sample lawyers 
        com.malikyasir.landlawassist.Models.Lawyer yasir = new com.malikyasir.landlawassist.Models.Lawyer(
            "1", 
            "Adv. Yasir Ramzan", 
            "yasir@example.com",
            "Lahore", 
            "Property Law Specialist", 
            "15", 
            "");
        yasir.setRating(4.5f);
        yasir.setCases(32);
        featuredLawyers.add(yasir);

        com.malikyasir.landlawassist.Models.Lawyer butt = new com.malikyasir.landlawassist.Models.Lawyer(
            "2", 
            "Adv. Butt Sahab", 
            "butt@example.com",
            "Karachi", 
            "Real Estate Law Expert", 
            "20", 
            "");
        butt.setRating(4.8f);
        butt.setCases(47);
        featuredLawyers.add(butt);

        com.malikyasir.landlawassist.Models.Lawyer sohail = new com.malikyasir.landlawassist.Models.Lawyer(
            "3", 
            "Adv. Sohail Ahmed", 
            "sohail@example.com",
            "Islamabad", 
            "Land Law Expert", 
            "18", 
            "");
        sohail.setRating(4.7f);
        sohail.setCases(38);
        featuredLawyers.add(sohail);

        Log.d("LawyerSlider", "Added " + featuredLawyers.size() + " sample lawyers");
        
        // Check if lawyerViewPager is null
        if (lawyerViewPager == null) {
            Log.e("LawyerSlider", "lawyerViewPager is null! Cannot set adapter");
            return;
        }
        
        // Initialize the adapter
        try {
            com.malikyasir.landlawassist.Adapters.LawyerAdapter lawyerAdapter = 
                new com.malikyasir.landlawassist.Adapters.LawyerAdapter(getContext(), featuredLawyers, lawyerViewPager);
            lawyerViewPager.setAdapter(lawyerAdapter);
            
            // Set orientation to horizontal
            lawyerViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
    
            // Add padding for showing part of next/previous cards
            int padding = getResources().getDimensionPixelOffset(R.dimen.viewpager_padding);
            lawyerViewPager.setPadding(padding, 0, padding, 0);
            lawyerViewPager.setClipToPadding(false);
            lawyerViewPager.setClipChildren(false);
            
            // This might throw an exception if ViewPager2 has no children
            try {
                lawyerViewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
            } catch (Exception e) {
                Log.e("LawyerSlider", "Failed to set over scroll mode", e);
            }
    
            // Set page transformer
            CompositePageTransformer transformer = new CompositePageTransformer();
            transformer.addTransformer((page, position) -> {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.85f + r * 0.15f);
            });
            lawyerViewPager.setPageTransformer(transformer);
            
            Log.d("LawyerSlider", "Lawyer ViewPager setup complete successfully");
        } catch (Exception e) {
            Log.e("LawyerSlider", "Error setting up lawyer ViewPager", e);
        }
    }

    private void loadAcceptedClients() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in case
            if (noClientsText != null) {
                noClientsText.setVisibility(View.VISIBLE);
                noClientsText.setText("Please log in to view your clients");
            }
            return;
        }
        
        String lawyerId = currentUser.getUid();
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

                // Debugging log
                Log.d("LawyerDashboard", "Found clients: " + clientIds.size());

                if (clientIds.isEmpty()) {
                    if (noClientsText != null) noClientsText.setVisibility(View.VISIBLE);
                    clientUsers.clear();
                    if (clientAdapter != null) clientAdapter.updateClients(clientUsers);
                } else {
                    if (noClientsText != null) noClientsText.setVisibility(View.GONE);
                    // Fetch user info for each client
                    clientUsers.clear();
                    for (String clientId : clientIds) {
                        db.collection("users").document(clientId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                User user = userDoc.toObject(User.class);
                                if (user != null) {
                                    user.setId(userDoc.getId());
                                    clientUsers.add(user);
                                    // Debugging log
                                    Log.d("LawyerDashboard", "Loaded client: " + user.getFullName());
                                    if (clientAdapter != null) clientAdapter.updateClients(clientUsers);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LawyerDashboard", "Error loading client: " + e.getMessage());
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e("LawyerDashboard", "Error fetching requests: " + e.getMessage());
            });
    }

    private void setupCityFilter() {
        // Commenting out the city filter logic for now
        /*
        cityList.clear();
        cityList.add("All Cities");
        cityList.add("Lahore");
        cityList.add("Karachi");
        cityList.add("Islamabad");
        cityList.add("Multan");
        cityList.add("Peshawar");
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, cityList);
        cityListView.setAdapter(cityAdapter);
        cityListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = cityList.get(position);
            if (selectedCity.equals("All Cities")) {
                loadAllLawyers();
            } else {
                loadLawyersByCity(selectedCity);
            }
        });
        */
    }

    private void loadAllLawyers() {
        Log.d("LawyerLoading", "Starting to load all lawyers from Firestore");
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d("LawyerLoading", "Query successful, found " + querySnapshot.size() + " lawyers");
                List<com.malikyasir.landlawassist.Models.Lawyer> loadedLawyers = new ArrayList<>();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        String lawyerId = doc.getId();
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String city = doc.getString("city");
                        String specialization = doc.getString("specialization");
                        String experience = doc.getString("experience");
                        String profileImage = doc.getString("profileImage");
                        Double rating = doc.getDouble("rating");
                        Long casesLong = doc.getLong("totalCases");
                        
                        com.malikyasir.landlawassist.Models.Lawyer lawyer = 
                            new com.malikyasir.landlawassist.Models.Lawyer(
                                lawyerId, 
                                fullName != null ? fullName : "Unknown Lawyer", 
                                email != null ? email : "",
                                city != null ? city : "Unknown Location", 
                                specialization != null ? specialization : "General Practice", 
                                experience != null ? experience : "N/A", 
                                profileImage != null ? profileImage : "");
                        
                        if (rating != null) {
                            lawyer.setRating(rating.floatValue());
                        } else {
                            lawyer.setRating(4.0f + new Random().nextFloat());
                        }
                        
                        if (casesLong != null) {
                            lawyer.setCases(casesLong.intValue());
                        } else {
                            lawyer.setCases(10 + new Random().nextInt(40));
                        }
                        
                        loadedLawyers.add(lawyer);
                        Log.d("LawyerLoading", "Added lawyer: " + lawyer.getName() + ", ID: " + lawyer.getId());
                    } catch (Exception e) {
                        Log.e("LawyerLoading", "Error converting document to Lawyer: " + doc.getId(), e);
                    }
                }
                
                Log.d("LawyerLoading", "Total lawyers loaded: " + loadedLawyers.size());
                
                if (lawyerViewPager != null) {
                    try {
                        com.malikyasir.landlawassist.Adapters.LawyerAdapter adapter = 
                            new com.malikyasir.landlawassist.Adapters.LawyerAdapter(getContext(), loadedLawyers, lawyerViewPager);
                        lawyerViewPager.setAdapter(adapter);
                        Log.d("LawyerLoading", "Lawyer adapter updated successfully");
                    } catch (Exception e) {
                        Log.e("LawyerLoading", "Error setting adapter", e);
                    }
                } else {
                    Log.e("LawyerLoading", "lawyerViewPager is null, cannot update");
                }
            })
            .addOnFailureListener(e -> {
                Log.e("LawyerLoading", "Failed to load lawyers: " + e.getMessage(), e);
            });
    }

    private void loadLawyersByCity(String city) {
        Log.d("LawyerLoading", "Loading lawyers for city: " + city);
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .whereEqualTo("city", city)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d("LawyerLoading", "Found " + querySnapshot.size() + " lawyers in " + city);
                List<com.malikyasir.landlawassist.Models.Lawyer> cityLawyers = new ArrayList<>();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    try {
                        String lawyerId = doc.getId();
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        String specialization = doc.getString("specialization");
                        String experience = doc.getString("experience");
                        String profileImage = doc.getString("profileImage");
                        Double rating = doc.getDouble("rating");
                        Long casesLong = doc.getLong("totalCases");
                        
                        com.malikyasir.landlawassist.Models.Lawyer lawyer = 
                            new com.malikyasir.landlawassist.Models.Lawyer(
                                lawyerId, 
                                fullName != null ? fullName : "Unknown Lawyer", 
                                email != null ? email : "",
                                city, 
                                specialization != null ? specialization : "General Practice", 
                                experience != null ? experience : "N/A", 
                                profileImage != null ? profileImage : "");
                        
                        if (rating != null) {
                            lawyer.setRating(rating.floatValue());
                        } else {
                            lawyer.setRating(4.0f + new Random().nextFloat());
                        }
                        
                        if (casesLong != null) {
                            lawyer.setCases(casesLong.intValue());
                        } else {
                            lawyer.setCases(10 + new Random().nextInt(40));
                        }
                        
                        cityLawyers.add(lawyer);
                        Log.d("LawyerLoading", "Added lawyer from " + city + ": " + lawyer.getName());
                    } catch (Exception e) {
                        Log.e("LawyerLoading", "Error converting document to Lawyer: " + doc.getId(), e);
                    }
                }
                
                if (lawyerViewPager != null) {
                    try {
                        com.malikyasir.landlawassist.Adapters.LawyerAdapter adapter = 
                            new com.malikyasir.landlawassist.Adapters.LawyerAdapter(getContext(), cityLawyers, lawyerViewPager);
                        lawyerViewPager.setAdapter(adapter);
                        Log.d("LawyerLoading", "Lawyer adapter updated with " + cityLawyers.size() + " lawyers from " + city);
                    } catch (Exception e) {
                        Log.e("LawyerLoading", "Error setting adapter for city lawyers", e);
                    }
                } else {
                    Log.e("LawyerLoading", "lawyerViewPager is null, cannot update city lawyers");
                }
            })
            .addOnFailureListener(e -> {
                Log.e("LawyerLoading", "Failed to load lawyers for city " + city + ": " + e.getMessage(), e);
            });
    }

    private void showAcceptedLawyersDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to view your lawyers", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        db.collection("lawyerRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> lawyerIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String lawyerId = doc.getString("lawyerId");
                    if (lawyerId != null) lawyerIds.add(lawyerId);
                }
                if (lawyerIds.isEmpty()) {
                    Toast.makeText(getContext(), "No accepted lawyers yet", Toast.LENGTH_SHORT).show();
                } else {
                    // Fetch lawyer profiles
                    List<User> acceptedLawyers = new ArrayList<>();
                    for (String lawyerId : lawyerIds) {
                        db.collection("users").document(lawyerId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                User lawyer = userDoc.toObject(User.class);
                                if (lawyer != null) {
                                    lawyer.setId(userDoc.getId());
                                    acceptedLawyers.add(lawyer);
                                    if (acceptedLawyers.size() == lawyerIds.size()) {
                                        showLawyersDialog(acceptedLawyers);
                                    }
                                }
                            });
                    }
                }
            });
    }

    private void showLawyersDialog(List<User> lawyers) {
        if (getContext() == null) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("My Lawyers");
        String[] lawyerNames = new String[lawyers.size()];
        for (int i = 0; i < lawyers.size(); i++) {
            lawyerNames[i] = lawyers.get(i).getFullName() + " (" + lawyers.get(i).getSpecialization() + ")";
        }
        builder.setItems(lawyerNames, (dialog, which) -> {
            // On lawyer click, open chat
            User lawyer = lawyers.get(which);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "Please log in to chat with lawyers", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String clientId = currentUser.getUid();
            String lawyerId = lawyer.getId();
            
            if (lawyerId == null) {
                Toast.makeText(getContext(), "Invalid lawyer information", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d("HomeFragment", "Starting chat - clientId: " + clientId + ", lawyerId: " + lawyerId);
            
            Intent intent = new Intent(getContext(), com.malikyasir.landlawassist.Activity.ChatActivity.class);
            intent.putExtra("userId", clientId);               // Client ID (current user)
            intent.putExtra("lawyerId", lawyerId);             // Lawyer ID
            intent.putExtra("lawyerName", lawyer.getFullName()); // Lawyer name
            intent.putExtra("userName", "Client");             // We're the client
            startActivity(intent);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void loadAcceptedLawyersForClient() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in case
            myLawyers.clear();
            myLawyersAdapter.notifyDataSetChanged();
            return;
        }
        
        String userId = currentUser.getUid();
        db.collection("lawyerRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> lawyerIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String lawyerId = doc.getString("lawyerId");
                    if (lawyerId != null) lawyerIds.add(lawyerId);
                }
                if (lawyerIds.isEmpty()) {
                    myLawyers.clear();
                    myLawyersAdapter.notifyDataSetChanged();
                } else {
                    myLawyers.clear();
                    for (String lawyerId : lawyerIds) {
                        db.collection("users").document(lawyerId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                User lawyer = userDoc.toObject(User.class);
                                if (lawyer != null) {
                                    lawyer.setId(userDoc.getId());
                                    myLawyers.add(lawyer);
                                    myLawyersAdapter.notifyDataSetChanged();
                                }
                            });
                    }
                }
            });
    }

    private void searchLawyersByName(String query) {
        Log.d("HomeFragment", "Searching for lawyers with name: " + query);
        
        // Create and show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Searching for lawyers...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                progressDialog.dismiss();
                List<com.malikyasir.landlawassist.Models.Lawyer> matchingLawyers = new ArrayList<>();
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String fullName = doc.getString("fullName");
                    if (fullName != null && fullName.toLowerCase().contains(query.toLowerCase())) {
                        try {
                            String lawyerId = doc.getId();
                            String email = doc.getString("email");
                            String city = doc.getString("city");
                            String specialization = doc.getString("specialization");
                            String experience = doc.getString("experience");
                            String profileImage = doc.getString("profileImage");
                            Double rating = doc.getDouble("rating");
                            Long casesLong = doc.getLong("totalCases");
                            
                            com.malikyasir.landlawassist.Models.Lawyer lawyer = 
                                new com.malikyasir.landlawassist.Models.Lawyer(
                                    lawyerId, 
                                    fullName, 
                                    email != null ? email : "",
                                    city != null ? city : "Unknown Location", 
                                    specialization != null ? specialization : "General Practice", 
                                    experience != null ? experience : "N/A", 
                                    profileImage != null ? profileImage : "");
                            
                            if (rating != null) {
                                lawyer.setRating(rating.floatValue());
                            }
                            
                            if (casesLong != null) {
                                lawyer.setCases(casesLong.intValue());
                            }
                            
                            matchingLawyers.add(lawyer);
                            Log.d("LawyerSearch", "Found matching lawyer: " + fullName);
                        } catch (Exception e) {
                            Log.e("LawyerSearch", "Error creating lawyer object: " + e.getMessage());
                        }
                    }
                }
                
                if (matchingLawyers.isEmpty()) {
                    Toast.makeText(getContext(), "No lawyers found matching '" + query + "'", Toast.LENGTH_SHORT).show();
                } else {
                    // Open FindLawyersActivity with search results
                    Intent intent = new Intent(getActivity(), com.malikyasir.landlawassist.Activity.FindLawyersActivity.class);
                    intent.putExtra("SEARCH_QUERY", query);
                    startActivity(intent);
                    
                    Toast.makeText(getContext(), 
                        "Found " + matchingLawyers.size() + " lawyers matching '" + query + "'", 
                        Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e("LawyerSearch", "Error searching lawyers: " + e.getMessage());
                Toast.makeText(getContext(), "Error searching: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showClientsToMessage() {
        if (clientUsers.isEmpty()) {
            Toast.makeText(getContext(), "You have no clients to message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] clientNames = new String[clientUsers.size()];
        for (int i = 0; i < clientUsers.size(); i++) {
            clientNames[i] = clientUsers.get(i).getFullName();
        }
        
        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Message Client")
            .setItems(clientNames, (dialog, which) -> {
                User client = clientUsers.get(which);
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(getContext(), "Please log in to chat with clients", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String lawyerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String clientUid = client.getId();
                
                if (clientUid == null || clientUid.isEmpty()) {
                    Toast.makeText(getContext(), "Invalid client information", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(getContext(), com.malikyasir.landlawassist.Activity.ChatActivity.class);
                intent.putExtra("userId", clientUid);       // Client ID
                intent.putExtra("lawyerId", lawyerUid);     // Lawyer ID
                intent.putExtra("lawyerName", "Lawyer");    // We're the lawyer
                intent.putExtra("userName", client.getFullName());  // Client name
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showAcceptedLawyersToMessage() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to view your lawyers", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        db.collection("lawyerRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACCEPTED")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> lawyerIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String lawyerId = doc.getString("lawyerId");
                    if (lawyerId != null) lawyerIds.add(lawyerId);
                }
                
                if (lawyerIds.isEmpty()) {
                    Toast.makeText(getContext(), "You have no lawyers to message", Toast.LENGTH_SHORT).show();
                } else {
                    // Fetch lawyer profiles for messaging
                    List<User> acceptedLawyers = new ArrayList<>();
                    final int[] count = {0};
                    
                    for (String lawyerId : lawyerIds) {
                        db.collection("users").document(lawyerId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                count[0]++;
                                User lawyer = userDoc.toObject(User.class);
                                if (lawyer != null) {
                                    lawyer.setId(userDoc.getId());
                                    acceptedLawyers.add(lawyer);
                                }
                                
                                // When all lawyers have been loaded
                                if (count[0] == lawyerIds.size()) {
                                    showLawyersMessageDialog(acceptedLawyers);
                                }
                            })
                            .addOnFailureListener(e -> {
                                count[0]++;
                                Log.e("HomeFragment", "Error loading lawyer: " + e.getMessage());
                                
                                // When all lawyers have been attempted to load
                                if (count[0] == lawyerIds.size()) {
                                    showLawyersMessageDialog(acceptedLawyers);
                                }
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading lawyers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showLawyersMessageDialog(List<User> lawyers) {
        if (lawyers.isEmpty()) {
            Toast.makeText(getContext(), "No lawyers available to message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] lawyerNames = new String[lawyers.size()];
        for (int i = 0; i < lawyers.size(); i++) {
            lawyerNames[i] = lawyers.get(i).getFullName();
        }
        
        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Message Lawyer")
            .setItems(lawyerNames, (dialog, which) -> {
                User lawyer = lawyers.get(which);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(getContext(), "Please log in to chat with lawyers", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String clientId = currentUser.getUid();
                String lawyerId = lawyer.getId();
                
                Intent intent = new Intent(getContext(), com.malikyasir.landlawassist.Activity.ChatActivity.class);
                intent.putExtra("userId", clientId);               // Client ID (current user)
                intent.putExtra("lawyerId", lawyerId);             // Lawyer ID
                intent.putExtra("lawyerName", lawyer.getFullName()); // Lawyer name
                intent.putExtra("userName", "Client");             // We're the client
                startActivity(intent);
            })
            .setNegativeButton("Close", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearGlideResources();
    }

    @Override
    public void onStop() {
        super.onStop();
        clearGlideResources();
    }

    private void clearGlideResources() {
        if (profileImage == null || !isAdded()) return;

        try {
            // Get context safely
            Context context = getContext();
            if (context != null) {
                Glide.with(context).clear(profileImage);
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Error clearing Glide resources", e);
        }
    }
}
