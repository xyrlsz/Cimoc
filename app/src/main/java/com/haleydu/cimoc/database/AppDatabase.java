package com.haleydu.cimoc.database;

import static com.haleydu.cimoc.Constants.DATABASE_NAME;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;


import com.haleydu.cimoc.model.Chapter;

import com.haleydu.cimoc.model.ChapterDao;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ComicDao;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.ImageUrlDao;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceDao;
import com.haleydu.cimoc.model.Tag;
import com.haleydu.cimoc.model.TagDao;
import com.haleydu.cimoc.model.TagRef;
import com.haleydu.cimoc.model.TagRefDao;
import com.haleydu.cimoc.model.Task;
import com.haleydu.cimoc.model.TaskDao;

@Database(entities = {Chapter.class, Comic.class, ImageUrl.class, Source.class, Tag.class, TagRef.class, Task.class}, version = 1, exportSchema = false)
@TypeConverters(StringArrayConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract ChapterDao chapterDao();
    public abstract ComicDao comicDao();
    public abstract ImageUrlDao imageUrlDao();
    public abstract SourceDao sourceDao();
    public abstract TagDao tagDao();
    public abstract TagRefDao tagRefDao();
    public abstract TaskDao taskDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}