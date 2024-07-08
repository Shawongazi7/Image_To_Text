package com.example.t2i.screens;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.t2i.R;
import com.example.t2i.adapters.ImageAdapter;
import com.example.t2i.services.ImageDatabase;
import com.example.t2i.services.ImageEntity;

import java.util.List;

public class HistoryActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener {

    private RecyclerView recyclerView;
    private ImageDatabase imageDatabase;
    private ImageAdapter imageAdapter;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_history);

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(R.color.black);

            recyclerView = findViewById(R.id.recyclerViewEntries);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            imageDatabase = Room.databaseBuilder(getApplicationContext(),
                    ImageDatabase.class, "imageDB").build();

            imageAdapter = new ImageAdapter(this, imageDatabase, this);
            recyclerView.setAdapter(imageAdapter);

            loadImagesFromDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the error, perhaps by showing a dialog to the user
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error")
                    .setMessage("An unexpected error occurred. Please try again.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
        }
    }

    private void loadImagesFromDatabase() {
        new Thread(() -> {
            List<ImageEntity> imageList = imageDatabase.imageDao().getAllTexts();
            runOnUiThread(() -> {
                imageAdapter.setImageList(imageList);
                imageAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onShareClick(ImageEntity image) {
        shareText(image.getRecognizedText());
    }

    @Override
    public void onDeleteClick(ImageEntity image) {
        deleteImage(image);
    }

    @Override
    public void onClick(ImageEntity image, View itemView) {
        imageAdapter.showEditDialog(image, itemView);
    }

    @Override
    public void onImageUpdated() {
        loadImagesFromDatabase();
    }

    private void shareText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void deleteImage(ImageEntity image) {
        new Thread(() -> {
            imageDatabase.imageDao().deleteImage(image);
            runOnUiThread(() -> {
                Toast.makeText(this, "Text deleted", Toast.LENGTH_SHORT).show();
                loadImagesFromDatabase();
            });
        }).start();
    }
}