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
import com.malikyasir.landlawassist.Adapters.LawyerAdapter;
import com.malikyasir.landlawassist.Models.Lawyer;
import com.malikyasir.landlawassist.R;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

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
    private List<Lawyer> lawyers;

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

        // Initialize Firebase and load data
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserData();
        loadUserProfile();
        setupLawyerSlider();

        // Setup Quick Actions
        view.findViewById(R.id.findLawyersCard).setOnClickListener(v -> {
            // Navigate to Find Lawyers section
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_legal_resources);
        });

        view.findViewById(R.id.viewCasesCard).setOnClickListener(v -> {
            // Navigate to Case Management section
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_case_management);
        });

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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        
        // Add sample lawyers
        lawyers.add(new Lawyer(
            "Adv. Yasir Ramzan",
            "Property Law Specialist",
            4.5f,
            "15 years experience",
            ""
        ));
        
        lawyers.add(new Lawyer(
            "Adv. Butt Sahab",
            "Real Estate Law Expert",
            4.8f,
            "20 years experience",
            ""
        ));

        lawyers.add(new Lawyer(
            "Adv. Sohail Ahmed",
            "Land Law Expert",
            4.7f,
            "18 years experience",
            ""
        ));

        LawyerAdapter adapter = new LawyerAdapter(lawyers);
        lawyerViewPager.setAdapter(adapter);
        
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
