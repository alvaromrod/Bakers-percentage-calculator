package com.pep1lo.bakerspercentagecalculator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pep1lo.bakerspercentagecalculator.databinding.ActivityCalculateBinding
import kotlinx.coroutines.launch
import java.util.Locale

class CalculateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalculateBinding
    private var recipe: Recipe? = null
    private lateinit var dataSource: RecipeDataSource
    private val ingredientWeightEditTexts = ArrayList<TextInputEditText>()
    private var isUpdating = false

    private val editRecipeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh the recipe from the database
            lifecycleScope.launch {
                recipe?.let {
                    val updatedRecipe = dataSource.getRecipe(it.id)
                    if (updatedRecipe != null) {
                        recipe = updatedRecipe
                        binding.textViewRecipeName.text = updatedRecipe.name
                        populateIngredientViews()
                        populateOptionalFields()
                        if (updatedRecipe.lastTotalWeight > 0) {
                            prepopulateAllFields(updatedRecipe.lastTotalWeight)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataSource = RecipeDataSource.getInstance(this)

        recipe = intent.getParcelableExtra("recipe")

        recipe?.let {
            binding.textViewRecipeName.text = it.name
            val unit = it.unit.ifEmpty { resources.getStringArray(R.array.units)[0] }
            binding.textInputLayoutTotalDoughWeight.hint = getString(R.string.total_dough_weight_unit, unit)
            populateIngredientViews()
            populateOptionalFields()
            if (it.lastTotalWeight > 0.0) {
                prepopulateAllFields(it.lastTotalWeight)
            }
        }

        binding.editTextTotalDoughWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!isUpdating) {
                    calculateFromTotalWeight(s.toString())
                }
            }
            override fun afterTextChanged(s: Editable) {}
        })

        binding.buttonEditRecipe.setOnClickListener {
            val intent = Intent(this@CalculateActivity, AddEditRecipeActivity::class.java)
            intent.putExtra("EDIT_RECIPE", recipe)
            editRecipeLauncher.launch(intent)
        }

        binding.buttonShareCalculated.setOnClickListener { shareCalculatedWeights() }
    }

    private fun shareCalculatedWeights() {
        if (recipe == null || ingredientWeightEditTexts.isEmpty()) {
            return
        }

        val unit = if (recipe!!.unit.isNotEmpty()) recipe!!.unit else resources.getStringArray(R.array.units)[0]
        val shareText = StringBuilder()
        shareText.append(getString(R.string.calculated_weights_for, recipe!!.name)).append("\n\n")

        for (i in recipe!!.ingredients.indices) {
            val ingredient = recipe!!.ingredients[i]
            val weight = ingredientWeightEditTexts[i].text.toString()
            if (weight.isNotEmpty()) {
                shareText.append("- ")
                    .append(ingredient.name)
                    .append(": ")
                    .append(weight)
                    .append(" ")
                    .append(unit)
                    .append("\n")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.calculated_weights_for, recipe!!.name))
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString())

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_recipe_via)))
    }

    private fun populateOptionalFields() {
        // Handle Notes
        val notes = recipe?.notes
        if (!notes.isNullOrEmpty() && notes.trim().isNotEmpty()) {
            binding.textViewNotesLabel.visibility = View.VISIBLE
            binding.textViewNotes.visibility = View.VISIBLE
            binding.textViewNotes.text = notes
        } else {
            binding.textViewNotesLabel.visibility = View.GONE
            binding.textViewNotes.visibility = View.GONE
        }

        // Handle Oven Instructions
        val ovenInfo = recipe?.ovenTempTime
        if (!ovenInfo.isNullOrEmpty() && ovenInfo.trim().isNotEmpty()) {
            binding.textViewOvenLabel.visibility = View.VISIBLE
            binding.textViewOven.visibility = View.VISIBLE
            binding.textViewOven.text = ovenInfo
        } else {
            binding.textViewOvenLabel.visibility = View.GONE
            binding.textViewOven.visibility = View.GONE
        }
    }

    private fun saveLastWeight(weight: Double) {
        recipe?.let {
            it.lastTotalWeight = weight
            lifecycleScope.launch {
                dataSource.updateRecipe(it)
            }
        }
    }

    private fun populateIngredientViews() {
        binding.linearLayoutResults.removeAllViews()
        ingredientWeightEditTexts.clear()
        val inflater = LayoutInflater.from(this)
        val unit = if (recipe!!.unit.isNotEmpty()) recipe!!.unit else resources.getStringArray(R.array.units)[0]

        for (ingredient in recipe!!.ingredients) {
            val ingredientView = inflater.inflate(R.layout.item_ingredient_result, binding.linearLayoutResults, false)
            val nameTextView = ingredientView.findViewById<TextView>(R.id.textViewIngredientName)
            val percentageTextView = ingredientView.findViewById<TextView>(R.id.textViewIngredientPercentage)
            val weightEditText = ingredientView.findViewById<TextInputEditText>(R.id.editTextWeight)
            val weightLayout = ingredientView.findViewById<TextInputLayout>(R.id.textInputLayoutWeight)

            nameTextView.text = ingredient.name
            percentageTextView.text = String.format(Locale.getDefault(), "%.1f%%", ingredient.weight)
            weightLayout.hint = getString(R.string.weight_unit, unit)
            ingredientWeightEditTexts.add(weightEditText)

            weightEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (!isUpdating) {
                        calculateFromIngredientWeight(ingredient.name, s.toString())
                    }
                }
                override fun afterTextChanged(s: Editable) {}
            })

            binding.linearLayoutResults.addView(ingredientView)
        }
    }

    private fun prepopulateAllFields(totalWeight: Double) {
        isUpdating = true // Lock all listeners to prevent loops
        try {
            binding.editTextTotalDoughWeight.setText(String.format(Locale.US, "%.2f", totalWeight))

            var totalPercentage = 0.0
            for (ingredient in recipe!!.ingredients) {
                totalPercentage += ingredient.weight
            }

            if (totalPercentage > 0) {
                var i = 0
                for (ingredient in recipe!!.ingredients) {
                    val ingredientWeight = (totalWeight * ingredient.weight) / totalPercentage
                    ingredientWeightEditTexts[i].setText(String.format(Locale.US, "%.2f", ingredientWeight))
                    i++
                }
            }
        } finally {
            isUpdating = false // Unlock listeners
        }
    }

    private fun calculateFromTotalWeight(totalWeightStr: String) {
        if (totalWeightStr.isEmpty()) return
        isUpdating = true
        try {
            val totalDoughWeight = totalWeightStr.toDouble()
            saveLastWeight(totalDoughWeight)
            var totalPercentage = 0.0
            for (ingredient in recipe!!.ingredients) {
                totalPercentage += ingredient.weight
            }

            if (totalPercentage > 0) {
                var i = 0
                for (ingredient in recipe!!.ingredients) {
                    val ingredientWeight = (totalDoughWeight * ingredient.weight) / totalPercentage
                    ingredientWeightEditTexts[i].setText(String.format(Locale.US, "%.2f", ingredientWeight))
                    i++
                }
            }
        } catch (e: NumberFormatException) {
            // Ignore
        }
        isUpdating = false
    }

    private fun calculateFromIngredientWeight(changedIngredientName: String, newWeightStr: String) {
        if (newWeightStr.isEmpty()) return
        isUpdating = true
        try {
            val newWeight = newWeightStr.toDouble()
            var changedIngredient: Ingredient? = null
            for (ingredient in recipe!!.ingredients) {
                if (ingredient.name == changedIngredientName) {
                    changedIngredient = ingredient
                    break
                }
            }

            if (changedIngredient == null || changedIngredient.weight == 0.0) {
                isUpdating = false
                return
            }

            val baseUnit = newWeight / changedIngredient.weight
            var newTotalDoughWeight = 0.0

            var i = 0
            for (ingredient in recipe!!.ingredients) {
                if (ingredient.name != changedIngredientName) {
                    val newIngredientWeight = baseUnit * ingredient.weight
                    ingredientWeightEditTexts[i].setText(String.format(Locale.US, "%.2f", newIngredientWeight))
                    newTotalDoughWeight += newIngredientWeight
                } else {
                    newTotalDoughWeight += newWeight
                }
                i++
            }

            binding.editTextTotalDoughWeight.setText(String.format(Locale.US, "%.2f", newTotalDoughWeight))
            saveLastWeight(newTotalDoughWeight)
        } catch (e: NumberFormatException) {
            // Ignore
        }
        isUpdating = false
    }
}
