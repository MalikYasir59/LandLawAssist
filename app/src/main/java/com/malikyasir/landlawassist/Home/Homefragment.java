package com.malikyasir.landlawassist.Home;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.malikyasir.landlawassist.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class Homefragment extends Fragment {
    private TextView userNameText;
    private CircleImageView profileImage;
    private TextView notificationBadge;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar profileProgress;
    private View profileCompletionCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Initialize views
        userNameText = view.findViewById(R.id.userName);
        profileImage = view.findViewById(R.id.profileImage);
        notificationBadge = view.findViewById(R.id.notificationBadge);
        profileProgress = view.findViewById(R.id.profileProgress);
        profileCompletionCard = view.findViewById(R.id.profileCompletionCard);

        // Initialize Firebase and load data
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserData();
        loadUserProfile();

        return view;
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String fullName = document.getString("fullName");
                            String imageUrl = document.getString("profileImage");

                            userNameText.setText(fullName);

                            if (imageUrl != null) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.profileuser)
                                        .into(profileImage);
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
                                        // Hide the card when update is successful
                                        profileCompletionCard.animate()
                                                .alpha(0f)
                                                .setStartDelay(1500)
                                                .setDuration(500)
                                                .withEndAction(() -> profileCompletionCard.setVisibility(View.GONE))
                                                .start();
                                    });
                        } else {
                            progress = 50;
                            // Update Firestore with 50% completion
                            document.getReference().update("profileCompletion", 50);
                            profileCompletionCard.setVisibility(View.VISIBLE);
                            profileCompletionCard.setAlpha(1f);
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
}
