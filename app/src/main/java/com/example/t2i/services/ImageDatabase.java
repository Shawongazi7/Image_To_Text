package com.example.t2i.services;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ImageEntity.class}, version = 1)
public abstract class ImageDatabase extends RoomDatabase {

    public abstract ImageDao imageDao();
}