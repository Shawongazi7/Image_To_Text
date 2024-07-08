package com.example.t2i.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;


import com.bumptech.glide.Glide;
import com.example.t2i.R;
import com.example.t2i.services.ImageDatabase;
import com.example.t2i.services.ImageEntity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private Button logoutButton, backupButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ImageDatabase imageDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        logoutButton = findViewById(R.id.logoutButton);
        backupButton = findViewById(R.id.backupButton);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        imageDatabase = Room.databaseBuilder(getApplicationContext(), ImageDatabase.class, "imageDB").build();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            fetchUserData(currentUser);
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        }

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backupAndRestoreData();
            }
        });
    }

    private void fetchUserData(final FirebaseUser user) {
        String userId = user.getUid();
        firestore.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()) {
                        // User exists in Firestore, use the stored data
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        updateUI(fullName, email, profileImageUrl);
                    } else {
                        // User doesn't exist in Firestore (likely Google Sign-In), use Firebase User data
                        String email = user.getEmail();
                        String name = extractNameFromEmail(email);
                        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

                        updateUI(name, email, photoUrl);

                        // Save this data to Firestore for future use
                        saveUserToFirestore(user.getUid(), name, email, photoUrl);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(String name, String email, String profileImageUrl) {
        profileName.setText(name);
        profileEmail.setText(email);

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_account);
        }
    }

    private String extractNameFromEmail(String email) {
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "User";
    }

    private void saveUserToFirestore(String userId, String name, String email, String profileImageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .set(new User(name, email, profileImageUrl))
                .addOnSuccessListener(aVoid -> {
                    // Data saved successfully
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }

    private void logout() {
        LoginActivity.logout(this, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void backupAndRestoreData() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        CollectionReference userDataRef = firestore.collection("users").document(userId).collection("userData");

        // First, backup local data to Firestore
        new Thread(() -> {
            List<ImageEntity> localData = imageDatabase.imageDao().getAllTexts();
            for (ImageEntity entity : localData) {
                userDataRef.document(String.valueOf(entity.getId())).set(entity)
                        .addOnSuccessListener(documentReference -> {
                            // Local data backed up successfully
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure
                            runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        });
            }

            // After backup, retrieve data from Firestore to ensure we have the latest data
            userDataRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<ImageEntity> firestoreData = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ImageEntity entity = document.toObject(ImageEntity.class);
                        firestoreData.add(entity);
                    }

                    // Update local database with Firestore data
                    new Thread(() -> {
                        imageDatabase.imageDao().deleteAllTexts(); // Clear existing data
                        imageDatabase.imageDao().insertAll(firestoreData);
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Backup and restore completed", Toast.LENGTH_SHORT).show());
                    }).start();
                } else {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Restore failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }).start();
    }
}

class User {
    public String fullName;
    public String email;
    public String profileImageUrl;

    public User(String fullName, String email, String profileImageUrl) {
        this.fullName = fullName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }

    // Empty constructor for Firestore
    public User() {}
}