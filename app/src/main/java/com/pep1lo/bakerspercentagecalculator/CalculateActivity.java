package com.pep1lo.bakerspercentagecalculator;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculateActivity extends AppCompatActivity {

    private Recipe recipe;
    private TextInputEditText editTextTotalDoughWeight;
    private TextInputLayout textInputLayoutTotalDoughWeight;
    private LinearLayout linearLayoutResults;
    private TextView textViewRecipeName;
    private TextView textViewNotesLabel, textViewNotes, textViewOvenLabel, textViewOven;
    private RecipeDataSource dataSource;

    private List<TextInputEditText> ingredientWeightEditTexts = new ArrayList<>();
    private boolean isUpdating = false;

    private final ActivityResultLauncher<Intent> editRecipeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Refresh the recipe from the database
                    this.recipe = dataSource.getRecipe(recipe.getId());
                    textViewRecipeName.setText(recipe.getName());
                    populateIngredientViews();
                    populateOptionalFields();
                    if (recipe.getLastTotalWeight() > 0) {
                        prepopulateAllFields(recipe.getLastTotalWeight());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate);

        dataSource = RecipeDataSource.getInstance(this);
        textViewRecipeName = findViewById(R.id.textViewRecipeName);
        editTextTotalDoughWeight = findViewById(R.id.editTextTotalDoughWeight);
        textInputLayoutTotalDoughWeight = findViewById(R.id.textInputLayoutTotalDoughWeight);
        linearLayoutResults = findViewById(R.id.linearLayoutResults);
        ImageButton buttonEditRecipe = findViewById(R.id.buttonEditRecipe);
        ImageButton buttonShareCalculated = findViewById(R.id.buttonShareCalculated);
        textViewNotesLabel = findViewById(R.id.textViewNotesLabel);
        textViewNotes = findViewById(R.id.textViewNotes);
        textViewOvenLabel = findViewById(R.id.textViewOvenLabel);
        textViewOven = findViewById(R.id.textViewOven);

        recipe = getIntent().getParcelableExtra("recipe");

        if (recipe != null) {
            textViewRecipeName.setText(recipe.getName());
            String unit = recipe.getUnit() != null ? recipe.getUnit() : getResources().getStringArray(R.array.units)[0];
            textInputLayoutTotalDoughWeight.setHint(getString(R.string.total_dough_weight_unit, unit));
            populateIngredientViews();
            populateOptionalFields();
            if (recipe.getLastTotalWeight() > 0) {
                prepopulateAllFields(recipe.getLastTotalWeight());
            }
        }

        editTextTotalDoughWeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isUpdating) {
                    calculateFromTotalWeight(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonEditRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(CalculateActivity.this, AddEditRecipeActivity.class);
            intent.putExtra("EDIT_RECIPE", recipe);
            editRecipeLauncher.launch(intent);
        });

        buttonShareCalculated.setOnClickListener(v -> shareCalculatedWeights());
    }

    private void shareCalculatedWeights() {
        if (recipe == null || ingredientWeightEditTexts.isEmpty()) {
            return;
        }

        String unit = recipe.getUnit() != null ? recipe.getUnit() : getResources().getStringArray(R.array.units)[0];
        StringBuilder shareText = new StringBuilder();
        shareText.append(getString(R.string.calculated_weights_for, recipe.getName())).append("\n\n");

        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);
            String weight = ingredientWeightEditTexts.get(i).getText().toString();
            if (!weight.isEmpty()) {
                shareText.append("- ")
                        .append(ingredient.getName())
                        .append(": ")
                        .append(weight)
                        .append(" ")
                        .append(unit)
                        .append("\n");
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.calculated_weights_for, recipe.getName()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_recipe_via)));
    }


    private void populateOptionalFields() {
        // Handle Notes
        String notes = recipe.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            textViewNotesLabel.setVisibility(View.VISIBLE);
            textViewNotes.setVisibility(View.VISIBLE);
            textViewNotes.setText(notes);
        } else {
            textViewNotesLabel.setVisibility(View.GONE);
            textViewNotes.setVisibility(View.GONE);
        }

        // Handle Oven Instructions
        String ovenInfo = recipe.getOvenTempTime();
        if (ovenInfo != null && !ovenInfo.trim().isEmpty()) {
            textViewOvenLabel.setVisibility(View.VISIBLE);
            textViewOven.setVisibility(View.VISIBLE);
            textViewOven.setText(ovenInfo);
        } else {
            textViewOvenLabel.setVisibility(View.GONE);
            textViewOven.setVisibility(View.GONE);
        }
    }

    private void saveLastWeight(double weight) {
        recipe.setLastTotalWeight(weight);
        dataSource.updateRecipe(recipe);
    }

    private void populateIngredientViews() {
        linearLayoutResults.removeAllViews();
        ingredientWeightEditTexts.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        String unit = recipe.getUnit() != null ? recipe.getUnit() : getResources().getStringArray(R.array.units)[0];

        for (Ingredient ingredient : recipe.getIngredients()) {
            View ingredientView = inflater.inflate(R.layout.item_ingredient_result, linearLayoutResults, false);
            TextView nameTextView = ingredientView.findViewById(R.id.textViewIngredientName);
            TextView percentageTextView = ingredientView.findViewById(R.id.textViewIngredientPercentage);
            TextInputEditText weightEditText = ingredientView.findViewById(R.id.editTextWeight);
            TextInputLayout weightLayout = ingredientView.findViewById(R.id.textInputLayoutWeight);

            nameTextView.setText(ingredient.getName());
            percentageTextView.setText(String.format(Locale.getDefault(), "%.1f%%", ingredient.getWeight()));
            weightLayout.setHint(getString(R.string.weight_unit, unit));
            ingredientWeightEditTexts.add(weightEditText);

            weightEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!isUpdating) {
                        calculateFromIngredientWeight(ingredient.getName(), s.toString());
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            linearLayoutResults.addView(ingredientView);
        }
    }

    private void prepopulateAllFields(double totalWeight) {
        isUpdating = true; // Lock all listeners to prevent loops
        try {
            editTextTotalDoughWeight.setText(String.format(Locale.US, "%.2f", totalWeight));

            double totalPercentage = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                totalPercentage += ingredient.getWeight();
            }

            if (totalPercentage > 0) {
                int i = 0;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    double ingredientWeight = (totalWeight * ingredient.getWeight()) / totalPercentage;
                    ingredientWeightEditTexts.get(i).setText(String.format(Locale.US, "%.2f", ingredientWeight));
                    i++;
                }
            }
        } finally {
            isUpdating = false; // Unlock listeners
        }
    }

    private void calculateFromTotalWeight(String totalWeightStr) {
        if (totalWeightStr.isEmpty()) return;
        isUpdating = true;
        try {
            double totalDoughWeight = Double.parseDouble(totalWeightStr);
            saveLastWeight(totalDoughWeight);
            double totalPercentage = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                totalPercentage += ingredient.getWeight();
            }

            if (totalPercentage > 0) {
                int i = 0;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    double ingredientWeight = (totalDoughWeight * ingredient.getWeight()) / totalPercentage;
                    ingredientWeightEditTexts.get(i).setText(String.format(Locale.US, "%.2f", ingredientWeight));
                    i++;
                }
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        isUpdating = false;
    }

    private void calculateFromIngredientWeight(String changedIngredientName, String newWeightStr) {
        if (newWeightStr.isEmpty()) return;
        isUpdating = true;
        try {
            double newWeight = Double.parseDouble(newWeightStr);
            Ingredient changedIngredient = null;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName().equals(changedIngredientName)) {
                    changedIngredient = ingredient;
                    break;
                }
            }

            if (changedIngredient == null || changedIngredient.getWeight() == 0) {
                isUpdating = false;
                return;
            }

            double baseUnit = newWeight / changedIngredient.getWeight();
            double newTotalDoughWeight = 0;

            int i = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (!ingredient.getName().equals(changedIngredientName)) {
                    double newIngredientWeight = baseUnit * ingredient.getWeight();
                    ingredientWeightEditTexts.get(i).setText(String.format(Locale.US, "%.2f", newIngredientWeight));
                    newTotalDoughWeight += newIngredientWeight;
                } else {
                    newTotalDoughWeight += newWeight;
                }
                i++;
            }

            editTextTotalDoughWeight.setText(String.format(Locale.US, "%.2f", newTotalDoughWeight));
            saveLastWeight(newTotalDoughWeight);
        } catch (NumberFormatException e) {
            // Ignore
        }
        isUpdating = false;
    }
}
