package com.malikyasir.landlawassist.Home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.malikyasir.landlawassist.Auth.LoginForm;
import com.malikyasir.landlawassist.R;
import de.hdodenhof.circleimageview.CircleImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CircleImageView userImageView;
    private TextView userNameText, userEmailText;
    private Uri userImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views first
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginForm.class));
            finish();
            return;
        }

        // Setup UI components
        setupNavigationDrawer();
        setupToolbar();
        setupBottomNavigationView();
        loadUserData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                loadUserData(); // Refresh user data when drawer opens
            }
        };
        
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Enable the drawer toggle for all fragments
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void setupNavigationDrawer() {
        View headerView = navigationView.getHeaderView(0);
        userImageView = headerView.findViewById(R.id.nav_user_image);
        userNameText = headerView.findViewById(R.id.nav_user_name);
        userEmailText = headerView.findViewById(R.id.nav_user_email);

        userImageView.setOnClickListener(v -> pickImage());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                showEditProfileDialog();
            } else if (id == R.id.nav_settings) {
                showSettingsDialog();
            } else if (id == R.id.nav_help) {
                showHelpDialog();
            } else if (id == R.id.nav_logout) {
                signOut();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fullName = document.getString("fullName");
                        String email = document.getString("email");
                        String imageUrl = document.getString("profileImage");

                        userNameText.setText(fullName);
                        userEmailText.setText(email);

                        if (imageUrl != null) {
                            Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.profileuser)
                                .into(userImageView);
                        }
                    }
                });
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            userImageUri = data.getData();
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (userImageUri != null) {
            String userId = mAuth.getCurrentUser().getUid();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images")
                .child(userId + ".jpg");

            storageRef.putFile(userImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfileImageUrl(uri.toString());
                        Glide.with(this)
                            .load(userImageUri)
                            .into(userImageView);
                    });
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateProfileImageUrl(String imageUrl) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
            .update("profileImage", imageUrl);
    }

    private void showEditProfileDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText streetAddressInput = view.findViewById(R.id.streetAddressInput);
        TextInputEditText countryInput = view.findViewById(R.id.countryInput);
        MaterialButton saveButton = view.findViewById(R.id.saveButton);

        // Load current values
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    streetAddressInput.setText(document.getString("streetAddress"));
                    countryInput.setText(document.getString("country"));
                }
            });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .create();

        saveButton.setOnClickListener(v -> {
            String streetAddress = streetAddressInput.getText().toString().trim();
            String country = countryInput.getText().toString().trim();

            if (streetAddress.isEmpty()) {
                streetAddressInput.setError("Street address is required");
                return;
            }
            if (country.isEmpty()) {
                countryInput.setError("Country is required");
                return;
            }

            // Update user profile
            Map<String, Object> updates = new HashMap<>();
            updates.put("streetAddress", streetAddress);
            updates.put("country", country);
            updates.put("profileCompletion", 100); // 100% when address is added

            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadUserData(); // Refresh UI
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    private void showSettingsDialog() {
        // Implement settings dialog with day/night mode
    }

    private void showHelpDialog() {
        // Implement help & support dialog
    }

    private void signOut() {
        new AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes", (dialog, which) -> {
                mAuth.signOut();
                startActivity(new Intent(this, LoginForm.class));
                finish();
            })
            .setNegativeButton("No", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (!isHomeFragment()) {
            // If not on home fragment, go to home
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only inflate menu for home fragment and clear for others
        if (isHomeFragment()) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        } else {
            menu.clear(); // Clear any existing menu items
            return true;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide all menu items when not on home fragment
        if (!isHomeFragment()) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Only handle options for home fragment
        if (isHomeFragment()) {
            if (item.getItemId() == android.R.id.home) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isHomeFragment() {
        Fragment currentFragment = getSupportFragmentManager()
            .findFragmentById(R.id.fragment_container);
        return currentFragment instanceof Homefragment;
    }

    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new Homefragment();
                getSupportActionBar().setTitle("");
                enableDrawer(true);
            } else if (item.getItemId() == R.id.nav_legal_resources) {
                selectedFragment = new LegalResourcesFragment();
                getSupportActionBar().setTitle("Legal Resources");
                enableDrawer(false);
            } else if (item.getItemId() == R.id.nav_case_management) {
                selectedFragment = new CaseManagementFragment();
                getSupportActionBar().setTitle("Case Management");
                enableDrawer(false);
            } else if (item.getItemId() == R.id.nav_ai_assistant) {
                selectedFragment = new AIAssistantFragment();
                getSupportActionBar().setTitle("AI Assistant");
                enableDrawer(false);
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // Set default fragment (Home)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void enableDrawer(boolean enable) {
        if (enable) {
            // For Home fragment
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                findViewById(R.id.toolbar),
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            
            // Force menu refresh
            invalidateOptionsMenu();
        } else {
            // For other fragments
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            
            // Force menu refresh and clear menu
            supportInvalidateOptionsMenu();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (!(fragment instanceof Homefragment)) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData(); // Refresh user data when activity resumes
    }
}
