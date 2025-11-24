package com.pep1lo.bakerspercentagecalculator

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE_RECIPES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COLUMN_LAST_WEIGHT REAL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COLUMN_INGREDIENTS TEXT")
            db.execSQL("DROP TABLE IF EXISTS ingredients")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COLUMN_UNIT TEXT NOT NULL DEFAULT 'grams'")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COLUMN_NOTES TEXT")
            db.execSQL("ALTER TABLE $TABLE_RECIPES ADD COLUMN $COLUMN_OVEN_TEMP_TIME TEXT")
        }
    }

    companion object {
        private const val DATABASE_NAME = "recipes.db"
        private const val DATABASE_VERSION = 5

        const val TABLE_RECIPES = "recipes"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_LAST_WEIGHT = "last_weight"
        const val COLUMN_INGREDIENTS = "ingredients"
        const val COLUMN_UNIT = "unit"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_OVEN_TEMP_TIME = "oven_temp_time"

        private const val TABLE_CREATE_RECIPES = "CREATE TABLE " + TABLE_RECIPES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_LAST_WEIGHT + " REAL DEFAULT 0, " +
                COLUMN_INGREDIENTS + " TEXT, " +
                COLUMN_UNIT + " TEXT NOT NULL DEFAULT 'grams', " +
                COLUMN_NOTES + " TEXT, " +
                COLUMN_OVEN_TEMP_TIME + " TEXT);"
    }
}
