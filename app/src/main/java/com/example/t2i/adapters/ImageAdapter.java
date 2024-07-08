package com.example.t2i.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.t2i.R;
import com.example.t2i.services.ImageDatabase;
import com.example.t2i.services.ImageEntity;

import java.util.List;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageEntity> imageList;
    private OnImageClickListener listener;
    private ImageDatabase imageDatabase;
    private Context context;
    private Activity activity;

    public ImageAdapter(OnImageClickListener listener, ImageDatabase imageDatabase, Context context) {
        this.listener = listener;
        this.imageDatabase = imageDatabase;
        this.context = context;
        this.activity = activity;
    }


    public void setImageList(List<ImageEntity> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_list_item, parent, false);
        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageEntity currentImage = imageList.get(position);
        String recognizedText = currentImage.getRecognizedText();
        String shortenedText = getShortenedText(recognizedText);
        holder.textViewImageText.setText(shortenedText);
    }

    private String getShortenedText(String text) {
        String[] words = text.split(" ");
        if (words.length > 3) {
            return words[0] + " " + words[1] + " " + words[2];
        } else {
            return text;
        }
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewImageText;
        ImageButton buttonShare;
        ImageButton buttonDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewImageText = itemView.findViewById(R.id.textViewImageText);
            buttonShare = itemView.findViewById(R.id.buttonShare);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);

            textViewImageText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        ImageEntity image = imageList.get(position);
                        listener.onClick(image, itemView);
                    }
                }
            });

            buttonShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onShareClick(imageList.get(position));
                    }
                }
            });

            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onDeleteClick(imageList.get(position));
                    }
                }
            });
        }
    }


    public void showEditDialog(ImageEntity image, View itemView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
        View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_edit_text, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(image.getRecognizedText());

        builder.setTitle("Edit Text");

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newText = editText.getText().toString();
                image.setRecognizedText(newText);

                // Update the image in the database
                new Thread(() -> {
//                    imageDatabase.imageDao().updateImage(image);
                    try {
                        imageDatabase.imageDao().updateImage(image);
                        listener.onImageUpdated(); // Notify the listener that the image has been updated

                        // Show a toast message to indicate that the edit was successful
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> {
                            Toast.makeText(itemView.getContext(), "Edit Texts saved", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        Log.e("ImageAdapter", "Error updating image in database", e);
                    }
                }).start();

                notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    public interface OnImageClickListener {
        void onShareClick(ImageEntity image);

        void onDeleteClick(ImageEntity image);

        void onClick(ImageEntity image, View itemView);

        void onImageUpdated();
    }
}


