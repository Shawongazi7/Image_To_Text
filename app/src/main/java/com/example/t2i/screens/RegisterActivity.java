package com.example.t2i.screens;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.t2i.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText v_signinfullname, v_signinemail, v_signinpassword;
    private AppCompatButton v_signup;
    private TextView v_gotologin;
    private ImageView v_profileImage;
    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        v_signinfullname = findViewById(R.id.signinfullname);
        v_signinemail = findViewById(R.id.signinemail);
        v_signinpassword = findViewById(R.id.signinpassword);
        v_signup = findViewById(R.id.signup);
        v_gotologin = findViewById(R.id.gotologin);
        v_profileImage = findViewById(R.id.profile_image);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        v_gotologin.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        v_signup.setOnClickListener(view -> {
            String fullName = v_signinfullname.getText().toString().trim();
            String mail = v_signinemail.getText().toString().trim();
            String pass = v_signinpassword.getText().toString().trim();

            if (fullName.isEmpty() || mail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Fill All the Fields", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                Toast.makeText(getApplicationContext(), "Invalid email address", Toast.LENGTH_SHORT).show();
            } else if (pass.length() < 8) {
                Toast.makeText(getApplicationContext(), "Password should be at least 8 characters", Toast.LENGTH_SHORT).show();
            } else {
                progressDialog.setMessage("Creating user...");
                progressDialog.show();
                firebaseAuth.createUserWithEmailAndPassword(mail, pass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uploadProfileImage(fullName, mail);
                    } else {
                        progressDialog.dismiss();
                        Exception exception = task.getException();
                        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                        Toast.makeText(getApplicationContext(), "Failed to Register: " + errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("RegisterActivity", "Registration failed", exception);
                    }
                });
            }
        });
    }

    public void chooseProfileImage(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Log.e("RegisterActivity", "Failed to take persistable URI permission", e);
            }
            v_profileImage.setImageURI(imageUri);
        }
    }

    private void uploadProfileImage(String fullName, String mail) {
        if (imageUri == null) {
            saveUserInfo(fullName, mail, null);
            return;
        }
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("RegisterActivity", "Current user is null");
            Toast.makeText(getApplicationContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        StorageReference filePath = storageReference.child("profile_images").child(userId + ".jpg");

        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        filePath.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    progressDialog.setMessage("Getting download URL...");
                    filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                        progressDialog.dismiss();
                        saveUserInfo(fullName, mail, uri.toString());
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.e("RegisterActivity", "Failed to get download URL", e);
                        Toast.makeText(getApplicationContext(), "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveUserInfo(fullName, mail, null);
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("RegisterActivity", "Failed to upload image", e);
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("does not have access")) {
                        Toast.makeText(getApplicationContext(), "Permission denied. Please check your Firebase Storage rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to upload image: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                    saveUserInfo(fullName, mail, null);
                });
    }

    private void saveUserInfo(String fullName, String mail, String profileImageUrl) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("RegisterActivity", "Current user is null");
            Toast.makeText(getApplicationContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", mail);
        if (profileImageUrl != null) {
            user.put("profileImageUrl", profileImageUrl);
        }

        firestore.collection("users").document(userId).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getApplicationContext(), "Registered Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Exception exception = task.getException();
                String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                Toast.makeText(getApplicationContext(), "Error saving user info: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e("RegisterActivity", "Failed to save user info", exception);
            }
        });
    }
}