package com.example.t2i.screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.t2i.R;
import com.example.t2i.services.ImageDatabase;
import com.example.t2i.services.ImageEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;

    private ImageView imageView;
    private EditText editText;
    private LinearLayout topButtonsLayout;
    private TextRecognizer recognizer;
    private ImageDatabase imageDatabase;
    private Uri photoUri;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));

        imageView = findViewById(R.id.imageView);
        editText = findViewById(R.id.editText);
        ImageView imageAccount = findViewById(R.id.icon_account);
        topButtonsLayout = findViewById(R.id.topButtonsLayout);
        Button buttonSave = findViewById(R.id.button_save);
        Button buttonCancel = findViewById(R.id.button_cancel);

        imageDatabase = Room.databaseBuilder(getApplicationContext(), ImageDatabase.class, "imageDB").fallbackToDestructiveMigration().build();

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Load profile image into icon_account using Glide
        loadProfileImageIntoIconAccount(imageAccount);

        imageView.setOnClickListener(v -> showImageOptionsDialog());

        imageAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        FloatingActionButton buttonHistory = findViewById(R.id.button_history);
        buttonHistory.setOnClickListener(v -> openHistoryActivity());

        buttonSave.setOnClickListener(v -> {
            String recognizedText = editText.getText().toString();
            saveRecognizedText(recognizedText);
        });

        buttonCancel.setOnClickListener(v -> resetViews());
    }

    private void loadProfileImageIntoIconAccount(ImageView imageView) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            firestore.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(MainActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_account) // Placeholder image
                                        .error(R.drawable.ic_account) // Error image
                                        .circleCrop() // Optional: Circle crop the image
                                        .into(imageView);
                            } else {
                                // Handle case where profile image URL is empty or null
                                Log.d("MainActivity", "Profile image URL is empty or null");
                            }
                        } else {
                            // Handle case where document doesn't exist
                            Log.d("MainActivity", "No such document");
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure to fetch profile image URL
                        Log.e("MainActivity", "Error fetching profile image URL", e);
                    });
        }
    }

    private void openHistoryActivity() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    private void showImageOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout pickImageOption = dialogView.findViewById(R.id.pick_image_option);
        LinearLayout captureImageOption = dialogView.findViewById(R.id.capture_image_option);

        pickImageOption.setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndPickImage();
        });

        captureImageOption.setOnClickListener(v -> {
            dialog.dismiss();
            checkPermissionAndCaptureImage();
        });

        dialog.show();
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            pickImage();
        }
    }

    private void checkPermissionAndCaptureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        } else {
            captureImage();
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create a file to save the image
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the file was successfully created
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.example.t2i.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
                recognizeText(bitmap);
                showOtherViews();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            // Use the photoUri to load the full-resolution image
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    recognizeText(bitmap);
                    showOtherViews();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showOtherViews() {
        topButtonsLayout.setVisibility(View.VISIBLE);
        editText.setVisibility(View.VISIBLE);
    }

    private void resetViews() {
        topButtonsLayout.setVisibility(View.GONE);
        editText.setVisibility(View.GONE);
        imageView.setImageResource(R.drawable.image2text);
        editText.setText("");
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(this::processTextRecognitionResult)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Text recognition failed.", Toast.LENGTH_SHORT).show();
                });
    }

    private void processTextRecognitionResult(Text text) {
        String recognizedText = text.getText();
        editText.setText(recognizedText);
    }

    private void saveRecognizedText(String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                String userEmail = currentUser != null ? currentUser.getEmail() : "Unknown";

                // Create a new ImageEntity with the recognized text and user email
                ImageEntity imageEntity = new ImageEntity(text, userEmail);

                // Insert the new entity into the database
                long id = imageDatabase.imageDao().insertText(imageEntity);

                runOnUiThread(() -> {
                    if (id != -1) {
                        Toast.makeText(MainActivity.this, "Text saved to database", Toast.LENGTH_SHORT).show();
                        resetViews();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to save text", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error saving text: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    captureImage();
                } else if (permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    pickImage();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}









