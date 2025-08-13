package com.pep1lo.bakerspercentagecalculator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "recipes.db";
    private static final int DATABASE_VERSION = 5; // IMPORTANT: Increment the version

    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LAST_WEIGHT = "last_weight";
    public static final String COLUMN_INGREDIENTS = "ingredients";
    public static final String COLUMN_UNIT = "unit";
    public static final String COLUMN_NOTES = "notes"; // New column
    public static final String COLUMN_OVEN_TEMP_TIME = "oven_temp_time"; // New column

    private static final String TABLE_CREATE_RECIPES =
            "CREATE TABLE " + TABLE_RECIPES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_LAST_WEIGHT + " REAL DEFAULT 0, " +
                    COLUMN_INGREDIENTS + " TEXT, " +
                    COLUMN_UNIT + " TEXT NOT NULL DEFAULT 'grams', " +
                    COLUMN_NOTES + " TEXT, " +
                    COLUMN_OVEN_TEMP_TIME + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_RECIPES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_LAST_WEIGHT + " REAL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_INGREDIENTS + " TEXT");
            db.execSQL("DROP TABLE IF EXISTS ingredients");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_UNIT + " TEXT NOT NULL DEFAULT 'grams'");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_NOTES + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_OVEN_TEMP_TIME + " TEXT");
        }
    }
}
