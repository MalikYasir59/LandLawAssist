package com.malikyasir.landlawassist.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homefragment, container, false);

        // Initialize views
        userNameText = view.findViewById(R.id.userName);
        profileImage = view.findViewById(R.id.profileImage);
        notificationBadge = view.findViewById(R.id.notificationBadge);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load user data
        loadUserData();

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
}
