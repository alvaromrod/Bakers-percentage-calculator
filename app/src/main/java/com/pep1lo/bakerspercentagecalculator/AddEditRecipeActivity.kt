package com.pep1lo.bakerspercentagecalculator

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.pep1lo.bakerspercentagecalculator.databinding.ActivityAddEditRecipeBinding
import kotlinx.coroutines.launch
import java.util.Collections

class AddEditRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditRecipeBinding
    private lateinit var dataSource: RecipeDataSource
    private var existingRecipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataSource = RecipeDataSource.getInstance(this)

        // Setup the unit dropdown
        val units = resources.getStringArray(R.array.units)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        binding.autoCompleteTextViewUnit.setAdapter(adapter)

        if (intent.hasExtra("EDIT_RECIPE")) {
            existingRecipe = intent.getParcelableExtra("EDIT_RECIPE")
            existingRecipe?.let { populateFields(it) }
        } else {
            // Pre-populate with Flour/Harina
            addIngredientView(getString(R.string.flour), 100.0, false)
            binding.autoCompleteTextViewUnit.setText(units[0], false) // Default to first unit
        }

        binding.buttonAddIngredient.setOnClickListener { addIngredientView(null, null, true) }
        binding.buttonSaveRecipe.setOnClickListener { saveRecipe() }
    }

    private fun populateFields(recipe: Recipe) {
        binding.editTextRecipeName.setText(recipe.name)
        binding.autoCompleteTextViewUnit.setText(recipe.unit, false)
        binding.editTextNotes.setText(recipe.notes)
        binding.editTextOven.setText(recipe.ovenTempTime)
        for (ingredient in recipe.ingredients) {
            addIngredientView(ingredient.name, ingredient.weight, false)
        }
    }

    private fun addIngredientView(name: String?, weight: Double?, animate: Boolean) {
        val ingredientView = layoutInflater.inflate(R.layout.item_ingredient, null, false)
        val editTextIngredientName = ingredientView.findViewById<TextInputEditText>(R.id.editTextIngredientName)
        val editTextPercentage = ingredientView.findViewById<TextInputEditText>(R.id.editTextPercentage)
        val buttonRemoveIngredient = ingredientView.findViewById<ImageButton>(R.id.buttonRemoveIngredient)

        if (name != null) {
            editTextIngredientName.setText(name)
        }
        if (weight != null) {
            editTextPercentage.setText(weight.toString())
        }

        buttonRemoveIngredient.setOnClickListener {
            val flourString = getString(R.string.flour)
            if (binding.linearLayoutIngredients.childCount > 1 || editTextIngredientName.text.toString() != flourString) {
                removeIngredientView(ingredientView)
            } else {
                Toast.makeText(this, R.string.at_least_one_ingredient, Toast.LENGTH_SHORT).show()
            }
        }

        binding.linearLayoutIngredients.addView(ingredientView)
        if (animate) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
            ingredientView.startAnimation(animation)
        }
    }

    private fun removeIngredientView(view: View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                binding.linearLayoutIngredients.removeView(view)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        view.startAnimation(animation)
    }

    private fun saveRecipe() {
        val recipeName = binding.editTextRecipeName.text.toString().trim()
        if (recipeName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_recipe_name, Toast.LENGTH_SHORT).show()
            return
        }

        val recipe = Recipe()
        recipe.name = recipeName
        recipe.unit = binding.autoCompleteTextViewUnit.text.toString()
        recipe.notes = binding.editTextNotes.text.toString().trim()
        recipe.ovenTempTime = binding.editTextOven.text.toString().trim()

        val ingredients = ArrayList<Ingredient>()

        for (i in 0 until binding.linearLayoutIngredients.childCount) {
            val ingredientView = binding.linearLayoutIngredients.getChildAt(i)
            val editTextIngredientName = ingredientView.findViewById<EditText>(R.id.editTextIngredientName)
            val editTextPercentage = ingredientView.findViewById<EditText>(R.id.editTextPercentage)

            val ingredientName = editTextIngredientName.text.toString().trim()
            val percentageStr = editTextPercentage.text.toString().trim()

            if (ingredientName.isNotEmpty() && percentageStr.isNotEmpty()) {
                try {
                    val weight = percentageStr.toDouble()
                    ingredients.add(Ingredient(ingredientName, weight))
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, getString(R.string.invalid_weight_for, ingredientName), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, R.string.add_at_least_one_ingredient, Toast.LENGTH_SHORT).show()
            return
        }

        val flourString = getString(R.string.flour)
        Collections.sort(ingredients) { o1, o2 ->
            if (o1.name.equals(flourString, ignoreCase = true)) {
                -1
            } else if (o2.name.equals(flourString, ignoreCase = true)) {
                1
            } else {
                0
            }
        }

        recipe.ingredients = ingredients

        lifecycleScope.launch {
            if (existingRecipe != null) {
                recipe.id = existingRecipe!!.id
                dataSource.updateRecipe(recipe)
            } else {
                dataSource.createRecipe(recipe)
            }

            setResult(RESULT_OK)
            finish()
        }
    }
}
