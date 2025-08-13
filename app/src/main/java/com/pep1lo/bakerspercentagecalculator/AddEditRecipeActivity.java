package com.pep1lo.bakerspercentagecalculator;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.pep1lo.bakerspercentagecalculator.R;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddEditRecipeActivity extends AppCompatActivity {

    private TextInputEditText editTextRecipeName;
    private AutoCompleteTextView autoCompleteTextViewUnit;
    private TextInputEditText editTextNotes;
    private TextInputEditText editTextOven;
    private LinearLayout linearLayoutIngredients;
    private ImageButton buttonAddIngredient;
    private Button buttonSaveRecipe;
    private RecipeDataSource dataSource;
    private Recipe existingRecipe; // To hold the recipe being edited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_recipe);

        dataSource = RecipeDataSource.getInstance(this);

        editTextRecipeName = findViewById(R.id.editTextRecipeName);
        autoCompleteTextViewUnit = findViewById(R.id.autoCompleteTextViewUnit);
        editTextNotes = findViewById(R.id.editTextNotes);
        editTextOven = findViewById(R.id.editTextOven);
        linearLayoutIngredients = findViewById(R.id.linearLayoutIngredients);
        buttonAddIngredient = findViewById(R.id.buttonAddIngredient);
        buttonSaveRecipe = findViewById(R.id.buttonSaveRecipe);

        // Setup the unit dropdown
        String[] units = getResources().getStringArray(R.array.units);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, units);
        autoCompleteTextViewUnit.setAdapter(adapter);

        if (getIntent().hasExtra("EDIT_RECIPE")) {
            existingRecipe = getIntent().getParcelableExtra("EDIT_RECIPE");
            populateFields(existingRecipe);
        } else {
            // Pre-populate with Flour/Harina
            addIngredientView(getString(R.string.flour), 100.0, false);
            autoCompleteTextViewUnit.setText(units[0], false); // Default to first unit
        }

        buttonAddIngredient.setOnClickListener(v -> addIngredientView(null, null, true));
        buttonSaveRecipe.setOnClickListener(v -> saveRecipe());
    }

    private void populateFields(Recipe recipe) {
        editTextRecipeName.setText(recipe.getName());
        autoCompleteTextViewUnit.setText(recipe.getUnit(), false);
        editTextNotes.setText(recipe.getNotes());
        editTextOven.setText(recipe.getOvenTempTime());
        for (Ingredient ingredient : recipe.getIngredients()) {
            addIngredientView(ingredient.getName(), ingredient.getWeight(), false);
        }
    }

    private void addIngredientView(String name, Double weight, boolean animate) {
        View ingredientView = getLayoutInflater().inflate(R.layout.item_ingredient, null, false);
        TextInputEditText editTextIngredientName = ingredientView.findViewById(R.id.editTextIngredientName);
        TextInputEditText editTextPercentage = ingredientView.findViewById(R.id.editTextPercentage);
        ImageButton buttonRemoveIngredient = ingredientView.findViewById(R.id.buttonRemoveIngredient);

        if (name != null) {
            editTextIngredientName.setText(name);
        }
        if (weight != null) {
            editTextPercentage.setText(String.valueOf(weight));
        }

        buttonRemoveIngredient.setOnClickListener(v -> {
            String flourString = getString(R.string.flour);
            if (linearLayoutIngredients.getChildCount() > 1 || !editTextIngredientName.getText().toString().equals(flourString)) {
                removeIngredientView(ingredientView);
            } else {
                Toast.makeText(this, R.string.at_least_one_ingredient, Toast.LENGTH_SHORT).show();
            }
        });

        linearLayoutIngredients.addView(ingredientView);
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down);
            ingredientView.startAnimation(animation);
        }
    }

    private void removeIngredientView(final View view) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                linearLayoutIngredients.removeView(view);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(animation);
    }

    private void saveRecipe() {
        String recipeName = editTextRecipeName.getText().toString().trim();
        if (recipeName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_recipe_name, Toast.LENGTH_SHORT).show();
            return;
        }

        Recipe recipe = new Recipe();
        recipe.setName(recipeName);
        recipe.setUnit(autoCompleteTextViewUnit.getText().toString());
        recipe.setNotes(editTextNotes.getText().toString().trim());
        recipe.setOvenTempTime(editTextOven.getText().toString().trim());

        List<Ingredient> ingredients = new ArrayList<>();

        for (int i = 0; i < linearLayoutIngredients.getChildCount(); i++) {
            View ingredientView = linearLayoutIngredients.getChildAt(i);
            EditText editTextIngredientName = ingredientView.findViewById(R.id.editTextIngredientName);
            EditText editTextPercentage = ingredientView.findViewById(R.id.editTextPercentage);

            String ingredientName = editTextIngredientName.getText().toString().trim();
            String percentageStr = editTextPercentage.getText().toString().trim();

            if (!ingredientName.isEmpty() && !percentageStr.isEmpty()) {
                try {
                    double weight = Double.parseDouble(percentageStr);
                    ingredients.add(new Ingredient(ingredientName, weight));
                } catch (NumberFormatException e) {
                    Toast.makeText(this, getString(R.string.invalid_weight_for, ingredientName), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, R.string.add_at_least_one_ingredient, Toast.LENGTH_SHORT).show();
            return;
        }

        final String flourString = getString(R.string.flour);
        Collections.sort(ingredients, (o1, o2) -> {
            if (o1.getName().equalsIgnoreCase(flourString)) {
                return -1;
            }
            if (o2.getName().equalsIgnoreCase(flourString)) {
                return 1;
            }
            return 0;
        });

        recipe.setIngredients(ingredients);

        if (existingRecipe != null) {
            recipe.setId(existingRecipe.getId());
            dataSource.updateRecipe(recipe);
        } else {
            dataSource.createRecipe(recipe);
        }

        setResult(RESULT_OK);
        finish();
    }
}
