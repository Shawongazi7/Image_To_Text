package com.example.t2i.services;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "images")
public class ImageEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "recognized_text")
    private String recognizedText;

    @ColumnInfo(name = "email")
    private String email;

    // Ignore the no-arg constructor
    @Ignore
    public ImageEntity() {
    }

    // Use this constructor to create instances of ImageEntity
    public ImageEntity(String recognizedText, String email) {
        this.recognizedText = recognizedText;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
