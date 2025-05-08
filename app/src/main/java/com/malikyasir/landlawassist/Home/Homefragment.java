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

public class Homefragment extends Fragment {
    private TextView userNameText;
    private CircleImageView profileImage;
    private TextView notificationBadge;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar profileProgress;
    private View profileCompletionCard;
    private TextView profileStatusText;
    private TextView profileStatusDescription;
    private ViewPager2 lawyerViewPager;
    private List<User> lawyers;
    private RecyclerView clientsRecyclerView;
    private TextView noClientsText;
    private View findLawyersCard, messagesCard, featuredLawyersSection;
    private String userType;
    private ClientAdapter clientAdapter;
    private List<User> clientUsers = new ArrayList<>();
    private ListView cityListView;
    private List<String> cityList = new ArrayList<>();
    private LawyerAdapter lawyerAdapter;
    private List<User> featuredLawyers = new ArrayList<>();
    private RecyclerView myLawyersRecyclerView;
    private List<User> myLawyers = new ArrayList<>();
    private LawyerAdapter myLawyersAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Initialize views
        userNameText = view.findViewById(R.id.userName);
        profileImage = view.findViewById(R.id.profileImage);
        notificationBadge = view.findViewById(R.id.notificationBadge);
        profileProgress = view.findViewById(R.id.profileProgress);
        profileCompletionCard = view.findViewById(R.id.profileCompletionCard);
        profileStatusText = view.findViewById(R.id.profileStatusText);
        profileStatusDescription = view.findViewById(R.id.profileStatusDescription);
        lawyerViewPager = view.findViewById(R.id.lawyerViewPager);
        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView);
        noClientsText = view.findViewById(R.id.noClientsText);
        findLawyersCard = view.findViewById(R.id.findLawyersCard);
        messagesCard = view.findViewById(R.id.messagesCard);
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

        if ("Lawyer".equalsIgnoreCase(userType)) {
            // Hide featured lawyers and find lawyers, show messages
            if (featuredLawyersSection != null) featuredLawyersSection.setVisibility(View.GONE);
            if (findLawyersCard != null) findLawyersCard.setVisibility(View.GONE);
            if (messagesCard != null) messagesCard.setVisibility(View.VISIBLE);
            // Show accepted clients
            clientAdapter = new ClientAdapter(getContext(), clientUsers, client -> {
                // TODO: Navigate to client details/cases
                Toast.makeText(getContext(), "Clicked: " + client.getFullName(), Toast.LENGTH_SHORT).show();
            });
            clientsRecyclerView.setAdapter(clientAdapter);
            loadAcceptedClients();
        } else {
            // User: show featured lawyers, hide clients
            if (featuredLawyersSection != null) featuredLawyersSection.setVisibility(View.VISIBLE);
            if (findLawyersCard != null) findLawyersCard.setVisibility(View.VISIBLE);
            if (messagesCard != null) messagesCard.setVisibility(View.GONE);
            if (clientsRecyclerView != null) clientsRecyclerView.setVisibility(View.GONE);
            if (noClientsText != null) noClientsText.setVisibility(View.GONE);
            setupLawyerSlider();
        }

        // Setup Quick Actions
        view.findViewById(R.id.myLawyersCard).setOnClickListener(v -> {
            // Show accepted lawyers for the client
            showAcceptedLawyersDialog();
        });

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

        // Add a button to navigate to FindLawyersActivity
        view.findViewById(R.id.findLawyersCard).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.malikyasir.landlawassist.Activity.FindLawyersActivity.class);
            startActivity(intent);
        });

        setupCityFilter();
        loadAllLawyers();

        myLawyersAdapter = new LawyerAdapter(getContext(), myLawyers);
        myLawyersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        myLawyersRecyclerView.setAdapter(myLawyersAdapter);
        loadAcceptedLawyersForClient();

        if (messagesCard != null) {
            messagesCard.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(getContext(), "Please log in to view messages", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Rest of your click listener logic
                // ...
            });
        }

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

                        if (userNameText != null) {
                            String fullName = document.getString("fullName");
                            userNameText.setText(fullName);
                        }
                    }
                });
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle not logged in case
            if (profileStatusText != null) profileStatusText.setText("Please log in");
            if (profileStatusDescription != null) profileStatusDescription.setText("You need to log in to view your profile");
            return;
        }
        
        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String streetAddress = document.getString("streetAddress");
                    String country = document.getString("country");

                    // Calculate progress
                    int progress;

                    // Check if address is complete
                    boolean isAddressComplete = streetAddress != null && !streetAddress.isEmpty()
                        && country != null && !country.isEmpty();

                    if (isAddressComplete) {
                        progress = 100;
                        // Update Firestore with 100% completion
                        document.getReference().update("profileCompletion", 100)
                            .addOnSuccessListener(aVoid -> {
                                // Update text instead of hiding
                                profileStatusText.setText("Profile Completed");
                                profileStatusDescription.setText("Your profile is now complete and ready to use");
                            });
                    } else {
                        progress = 50;
                        // Update Firestore with 50% completion
                        document.getReference().update("profileCompletion", 50);
                        // Set default text
                        profileStatusText.setText("Complete your profile");
                        profileStatusDescription.setText("Completing your profile will make easier for you find the best lawyers");
                    }

                    // Animate progress bar
                    ObjectAnimator animation = ObjectAnimator.ofInt(
                        profileProgress,
                        "progress",
                        0,
                        progress
                    );
                    animation.setDuration(1000);
                    animation.setInterpolator(new DecelerateInterpolator());
                    animation.start();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupLawyerSlider() {
        lawyers = new ArrayList<>();
        // Add sample lawyers as User objects
        User yasir = new User();
        yasir.setFullName("Adv. Yasir Ramzan");
        yasir.setSpecialization("Property Law Specialist");
        yasir.setRating(4.5);
        yasir.setExperience("15 years experience");
        yasir.setProfileImage(""); // Set image URL or leave blank
        lawyers.add(yasir);

        User butt = new User();
        butt.setFullName("Adv. Butt Sahab");
        butt.setSpecialization("Real Estate Law Expert");
        butt.setRating(4.8);
        butt.setExperience("20 years experience");
        butt.setProfileImage("");
        lawyers.add(butt);

        User sohail = new User();
        sohail.setFullName("Adv. Sohail Ahmed");
        sohail.setSpecialization("Land Law Expert");
        sohail.setRating(4.7);
        sohail.setExperience("18 years experience");
        sohail.setProfileImage("");
        lawyers.add(sohail);

        // Initialize the field, not a local variable
        lawyerAdapter = new LawyerAdapter(getContext(), lawyers);
        lawyerViewPager.setAdapter(lawyerAdapter);

        // Set orientation to horizontal
        lawyerViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        // Add padding for showing part of next/previous cards
        int padding = getResources().getDimensionPixelOffset(R.dimen.viewpager_padding);
        lawyerViewPager.setPadding(padding, 0, padding, 0);
        lawyerViewPager.setClipToPadding(false);
        lawyerViewPager.setClipChildren(false);
        lawyerViewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Set page transformer
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });
        lawyerViewPager.setPageTransformer(transformer);
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
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                featuredLawyers.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    User lawyer = doc.toObject(User.class);
                    if (lawyer != null) {
                        lawyer.setId(doc.getId());
                        featuredLawyers.add(lawyer);
                    }
                }
                if (lawyerAdapter != null) {
                    lawyerAdapter.updateLawyers(featuredLawyers);
                }
            });
    }

    private void loadLawyersByCity(String city) {
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("userType", "Lawyer")
            .whereEqualTo("city", city)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                featuredLawyers.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    User lawyer = doc.toObject(User.class);
                    if (lawyer != null) {
                        lawyer.setId(doc.getId());
                        featuredLawyers.add(lawyer);
                    }
                }
                if (lawyerAdapter != null) {
                    lawyerAdapter.updateLawyers(featuredLawyers);
                }
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
