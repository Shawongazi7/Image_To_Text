package com.example.t2i.services;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ImageDao {

    @Insert
    long insertText(ImageEntity imageEntity);

    @Query("SELECT * FROM images")
    List<ImageEntity> getAllTexts();

    @Delete
    void deleteImage(ImageEntity image);

    @Update
    void updateImage(ImageEntity imageEntity);

    // New methods for backup and restore
    @Query("SELECT * FROM images WHERE email = :email")
    List<ImageEntity> getTextsForUser(String email);

    @Insert
    void insertAllTexts(List<ImageEntity> imageEntities);

    @Query("DELETE FROM images")
    void deleteAllTexts();

    @Insert
    void insertAll(List<ImageEntity> firestoreData);
}
