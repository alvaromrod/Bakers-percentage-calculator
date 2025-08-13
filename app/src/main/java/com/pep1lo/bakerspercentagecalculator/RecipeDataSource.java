package com.pep1lo.bakerspercentagecalculator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecipeDataSource {

    private static RecipeDataSource instance;
    private SQLiteDatabase database;
    private final DatabaseHelper dbHelper;
    private final String[] allColumns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_NAME,
            DatabaseHelper.COLUMN_INGREDIENTS,
            DatabaseHelper.COLUMN_LAST_WEIGHT,
            DatabaseHelper.COLUMN_UNIT,
            DatabaseHelper.COLUMN_NOTES,
            DatabaseHelper.COLUMN_OVEN_TEMP_TIME
    };
    private final Gson gson = new Gson();

    private RecipeDataSource(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public static synchronized RecipeDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeDataSource(context.getApplicationContext());
        }
        return instance;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }


    public void close() {
        dbHelper.close();
    }

    public Recipe createRecipe(Recipe recipe) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, recipe.getName());
        values.put(DatabaseHelper.COLUMN_UNIT, recipe.getUnit());
        values.put(DatabaseHelper.COLUMN_NOTES, recipe.getNotes());
        values.put(DatabaseHelper.COLUMN_OVEN_TEMP_TIME, recipe.getOvenTempTime());
        String ingredientsJson = gson.toJson(recipe.getIngredients());
        values.put(DatabaseHelper.COLUMN_INGREDIENTS, ingredientsJson);

        long insertId = database.insert(DatabaseHelper.TABLE_RECIPES, null, values);

        if (insertId == -1) {
            return null; // Insert failed
        }

        Cursor cursor = database.query(DatabaseHelper.TABLE_RECIPES,
                allColumns, DatabaseHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Recipe newRecipe = cursorToRecipe(cursor);
            cursor.close();
            return newRecipe;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public void updateRecipe(Recipe recipe) {
        long id = recipe.getId();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, recipe.getName());
        values.put(DatabaseHelper.COLUMN_UNIT, recipe.getUnit());
        values.put(DatabaseHelper.COLUMN_LAST_WEIGHT, recipe.getLastTotalWeight());
        values.put(DatabaseHelper.COLUMN_NOTES, recipe.getNotes());
        values.put(DatabaseHelper.COLUMN_OVEN_TEMP_TIME, recipe.getOvenTempTime());
        String ingredientsJson = gson.toJson(recipe.getIngredients());
        values.put(DatabaseHelper.COLUMN_INGREDIENTS, ingredientsJson);

        database.update(DatabaseHelper.TABLE_RECIPES, values,
                DatabaseHelper.COLUMN_ID + " = " + id, null);
    }

    public void deleteRecipe(long id) {
        database.delete(DatabaseHelper.TABLE_RECIPES,
                DatabaseHelper.COLUMN_ID + " = " + id, null);
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_RECIPES,
                allColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Recipe recipe = cursorToRecipe(cursor);
            recipes.add(recipe);
            cursor.moveToNext();
        }
        cursor.close();
        return recipes;
    }

    private Recipe cursorToRecipe(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
        recipe.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UNIT)));
        recipe.setLastTotalWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_WEIGHT)));
        recipe.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTES)));
        recipe.setOvenTempTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OVEN_TEMP_TIME)));
        String ingredientsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INGREDIENTS));

        if (ingredientsJson != null && !ingredientsJson.isEmpty()) {
            Type listType = new TypeToken<ArrayList<Ingredient>>(){}.getType();
            List<Ingredient> ingredients = gson.fromJson(ingredientsJson, listType);
            recipe.setIngredients(ingredients);
        } else {
            recipe.setIngredients(new ArrayList<>());
        }

        return recipe;
    }
    public void deleteAllRecipes() {
        database.delete(DatabaseHelper.TABLE_RECIPES, null, null);
    }
    public Recipe getRecipe(long recipeId) {
        Cursor cursor = database.query(DatabaseHelper.TABLE_RECIPES, allColumns,
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(recipeId)},
                null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                Recipe recipe = cursorToRecipe(cursor);
                cursor.close();
                return recipe;
            }
            cursor.close();
        }
        return null;
    }
}
