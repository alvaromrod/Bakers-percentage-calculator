package com.pep1lo.bakerspercentagecalculator

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeDataSource private constructor(context: Context) {

    private var database: SQLiteDatabase? = null
    private val dbHelper: DatabaseHelper = DatabaseHelper(context)
    private val allColumns = arrayOf(
        DatabaseHelper.COLUMN_ID,
        DatabaseHelper.COLUMN_NAME,
        DatabaseHelper.COLUMN_INGREDIENTS,
        DatabaseHelper.COLUMN_LAST_WEIGHT,
        DatabaseHelper.COLUMN_UNIT,
        DatabaseHelper.COLUMN_NOTES,
        DatabaseHelper.COLUMN_OVEN_TEMP_TIME
    )
    private val gson = Gson()

    @Throws(SQLException::class)
    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    suspend fun createRecipe(recipe: Recipe): Recipe? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NAME, recipe.name)
            put(DatabaseHelper.COLUMN_UNIT, recipe.unit)
            put(DatabaseHelper.COLUMN_NOTES, recipe.notes)
            put(DatabaseHelper.COLUMN_OVEN_TEMP_TIME, recipe.ovenTempTime)
            val ingredientsJson = gson.toJson(recipe.ingredients)
            put(DatabaseHelper.COLUMN_INGREDIENTS, ingredientsJson)
        }

        val insertId = database!!.insert(DatabaseHelper.TABLE_RECIPES, null, values)

        if (insertId == -1L) {
            return@withContext null
        }

        val cursor = database!!.query(
            DatabaseHelper.TABLE_RECIPES,
            allColumns, DatabaseHelper.COLUMN_ID + " = " + insertId, null,
            null, null, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return@withContext cursorToRecipe(it)
            }
        }
        return@withContext null
    }

    suspend fun updateRecipe(recipe: Recipe) = withContext(Dispatchers.IO) {
        val id = recipe.id
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NAME, recipe.name)
            put(DatabaseHelper.COLUMN_UNIT, recipe.unit)
            put(DatabaseHelper.COLUMN_LAST_WEIGHT, recipe.lastTotalWeight)
            put(DatabaseHelper.COLUMN_NOTES, recipe.notes)
            put(DatabaseHelper.COLUMN_OVEN_TEMP_TIME, recipe.ovenTempTime)
            val ingredientsJson = gson.toJson(recipe.ingredients)
            put(DatabaseHelper.COLUMN_INGREDIENTS, ingredientsJson)
        }

        database!!.update(
            DatabaseHelper.TABLE_RECIPES, values,
            DatabaseHelper.COLUMN_ID + " = " + id, null
        )
    }

    suspend fun deleteRecipe(id: Long) = withContext(Dispatchers.IO) {
        database!!.delete(
            DatabaseHelper.TABLE_RECIPES,
            DatabaseHelper.COLUMN_ID + " = " + id, null
        )
    }

    suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        val recipes = ArrayList<Recipe>()
        val cursor = database!!.query(
            DatabaseHelper.TABLE_RECIPES,
            allColumns, null, null, null, null, null
        )

        cursor.use {
            it.moveToFirst()
            while (!it.isAfterLast) {
                val recipe = cursorToRecipe(it)
                recipes.add(recipe)
                it.moveToNext()
            }
        }
        return@withContext recipes
    }

    private fun cursorToRecipe(cursor: Cursor): Recipe {
        val recipe = Recipe()
        recipe.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
        recipe.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
        recipe.unit = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UNIT))
        recipe.lastTotalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_WEIGHT))
        recipe.notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTES))
        recipe.ovenTempTime = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_OVEN_TEMP_TIME))
        val ingredientsJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INGREDIENTS))

        if (!ingredientsJson.isNullOrEmpty()) {
            val listType = object : TypeToken<ArrayList<Ingredient>>() {}.type
            val ingredients: MutableList<Ingredient> = gson.fromJson(ingredientsJson, listType)
            recipe.ingredients = ingredients
        } else {
            recipe.ingredients = ArrayList()
        }

        return recipe
    }

    suspend fun deleteAllRecipes() = withContext(Dispatchers.IO) {
        database!!.delete(DatabaseHelper.TABLE_RECIPES, null, null)
    }

    suspend fun getRecipe(recipeId: Long): Recipe? = withContext(Dispatchers.IO) {
        val cursor = database!!.query(
            DatabaseHelper.TABLE_RECIPES, allColumns,
            DatabaseHelper.COLUMN_ID + " = ?", arrayOf(recipeId.toString()),
            null, null, null
        )

        cursor?.use {
            it.moveToFirst()
            if (!it.isAfterLast) {
                return@withContext cursorToRecipe(it)
            }
        }
        return@withContext null
    }

    companion object {
        @Volatile
        private var instance: RecipeDataSource? = null

        fun getInstance(context: Context): RecipeDataSource {
            return instance ?: synchronized(this) {
                instance ?: RecipeDataSource(context.applicationContext).also { instance = it }
            }
        }
    }
}
