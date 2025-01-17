package com.haleydu.cimoc.helper;

import static com.haleydu.cimoc.Constants.DATABASE_NAME;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.utils.StringUtils;


public class DBOpenHelper {


    private static AppDatabase database;
    private String databaseName;
    private Context context ;

    public DBOpenHelper(Context context,String dbName){
        this.context = context;
        this.databaseName = dbName;
    }

    public DBOpenHelper(Context context){
        this.context = context;
        this.databaseName = DATABASE_NAME;
    }
    public  AppDatabase getDatabase(Context context,String databaseName) {
        if (database == null) {
            database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, databaseName)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build();
        }
        return database;
    }

    public  AppDatabase getDatabase(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, databaseName)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build();
        }
        return database;
    }
    public AppDatabase getDatabase() {
        if (database == null) {
            database = Room.databaseBuilder(this.context.getApplicationContext(), AppDatabase.class, databaseName)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build();
        }
        return database;
    }

    // Define migrations for each version upgrade
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 1, 2));
            // Create SOURCE table
            db.execSQL("CREATE TABLE IF NOT EXISTS `source` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INTEGER NOT NULL, `title` TEXT NOT NULL, `enable` INTEGER NOT NULL)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 2, 3));
            // Update HIGHLIGHT column in COMIC table
            db.execSQL("ALTER TABLE `comic` ADD COLUMN `highlight` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE `comic` SET `highlight` = 1, `favorite` = " + System.currentTimeMillis() + " WHERE `favorite` = " + 0xFFFFFFFFFFFL);
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 3, 4));
            // Create TASK table
            db.execSQL("CREATE TABLE IF NOT EXISTS `task` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `key` INTEGER NOT NULL, `path` TEXT NOT NULL, `title` TEXT NOT NULL, `progress` INTEGER NOT NULL, `max` INTEGER NOT NULL)");
            // Update DOWNLOAD column in COMIC table
            db.execSQL("ALTER TABLE `comic` ADD COLUMN `download` INTEGER");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 4, 5));
            // No changes needed for version 4 to 5
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 5, 6));
            // Recreate SOURCE table
            db.execSQL("DROP TABLE IF EXISTS `source`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `source` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` INTEGER NOT NULL, `title` TEXT NOT NULL, `enable` INTEGER NOT NULL)");
            // Create TAG and TAG_REF tables
            db.execSQL("CREATE TABLE IF NOT EXISTS `tag` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `title` TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `tag_ref` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `tid` INTEGER NOT NULL, `cid` INTEGER NOT NULL)");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 6, 7));
            // Update LOCAL column in COMIC table
            db.execSQL("ALTER TABLE `comic` ADD COLUMN `local` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 7, 8));
            // No changes needed for version 7 to 8
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 8, 9));
            // Update SOURCE table and add URL column to COMIC table
            db.execSQL("ALTER TABLE `source` ADD COLUMN `url` TEXT");
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 9, 10));
            // Update INTRO and AUTHOR columns in COMIC table
            db.execSQL("ALTER TABLE `comic` ADD COLUMN `intro` TEXT");
            db.execSQL("ALTER TABLE `comic` ADD COLUMN `author` TEXT");
            // Create CHAPTER and IMAGE_URL tables
            db.execSQL("CREATE TABLE IF NOT EXISTS `chapter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `source_comic` INTEGER NOT NULL, `title` TEXT NOT NULL, `path` TEXT NOT NULL, `count` INTEGER NOT NULL, `complete` INTEGER NOT NULL, `download` INTEGER NOT NULL, `tid` INTEGER NOT NULL, `source_group` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `image_url` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `comic_chapter` INTEGER NOT NULL, `num` INTEGER NOT NULL, `urls` TEXT, `chapter` TEXT, `state` INTEGER NOT NULL, `height` INTEGER NOT NULL, `width` INTEGER NOT NULL, `lazy` INTEGER NOT NULL, `loading` INTEGER NOT NULL, `success` INTEGER NOT NULL, `download` INTEGER NOT NULL)");
        }
    };

    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            Log.d("DB", StringUtils.format("DB V:%d,%d", 10, 11));
            // Update CHAPTER table
            db.execSQL("ALTER TABLE `chapter` RENAME TO `chapter2`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `chapter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `source_comic` INTEGER NOT NULL, `title` TEXT NOT NULL, `path` TEXT NOT NULL, `count` INTEGER NOT NULL, `complete` INTEGER NOT NULL, `download` INTEGER NOT NULL, `tid` INTEGER NOT NULL, `source_group` TEXT)");
            db.execSQL("INSERT INTO `chapter` (`id`, `source_comic`, `title`, `path`, `count`, `complete`, `download`, `tid`, `source_group`) SELECT `id`, `source_comic`, `title`, `path`, `count`, `complete`, `download`, `tid`, '' FROM `chapter2`");
            db.execSQL("DROP TABLE `chapter2`");
        }
    };
}