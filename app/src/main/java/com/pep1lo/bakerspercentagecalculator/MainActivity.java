package com.pep1lo.bakerspercentagecalculator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pep1lo.bakerspercentagecalculator.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecipeDataSource dataSource;
    private RecyclerView recyclerViewRecipes;
    private FloatingActionButton fabAddRecipe;
    private RecipeAdapter adapter;
    private List<Recipe> recipes = new ArrayList<>();

    private final ActivityResultLauncher<Intent> addRecipeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadRecipes();
                }
            });

    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    writeBackupToFile(uri);
                }
            });

    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    readBackupFromFile(uri);
                }
            });

    // Launcher for the QR Code Camera Scanner
    private final ActivityResultLauncher<ScanOptions> qrCodeScannerLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    importRecipeFromJson(result.getContents());
                }
            });

    // New launcher for picking an image from the gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    scanQrCodeFromImage(imageUri);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataSource = RecipeDataSource.getInstance(this);
        dataSource.open();

        recyclerViewRecipes = findViewById(R.id.recyclerViewRecipes);
        fabAddRecipe = findViewById(R.id.fabAddRecipe);

        setupRecyclerView();
        loadRecipes();

        fabAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            addRecipeLauncher.launch(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_backup) {
            initiateBackup();
            return true;
        } else if (id == R.id.action_restore) {
            initiateRestore();
            return true;
        } else if (id == R.id.action_import_recipe) {
            showImportDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImportDialog() {
        final CharSequence[] options = {
                getString(R.string.scan_with_camera),
                getString(R.string.choose_from_gallery)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.import_recipe);
        builder.setItems(options, (dialog, which) -> {
            if (options[which].equals(getString(R.string.scan_with_camera))) {
                initiateScan();
            } else if (options[which].equals(getString(R.string.choose_from_gallery))) {
                initiateImagePick();
            }
        });
        builder.show();
    }

    private void initiateImagePick() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void scanQrCodeFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, R.string.qr_code_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);
            importRecipeFromJson(result.getText());
        } catch (Exception e) {
            Toast.makeText(this, R.string.qr_code_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initiateScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a recipe QR code");
        options.setCameraId(0);  // Use a specific camera of the device
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        qrCodeScannerLauncher.launch(options);
    }

    private void initiateBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "bakers_percentage_backup.json");
        backupLauncher.launch(intent);
    }

    private void writeBackupToFile(Uri uri) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(recipes);
            getContentResolver().openOutputStream(uri).write(json.getBytes());
            Toast.makeText(this, R.string.backup_successful, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateRestore() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_confirmation_title)
                .setMessage(R.string.restore_confirmation_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    restoreLauncher.launch(intent);
                })
                .show();
    }

    private void importRecipeFromJson(String jsonString) {
        Gson gson = new Gson();
        try {
            Recipe recipe = gson.fromJson(jsonString, Recipe.class);

            if (recipe != null && recipe.getName() != null && !recipe.getName().isEmpty()) {
                dataSource.createRecipe(recipe);
                loadRecipes();
                Toast.makeText(this, R.string.recipe_imported_successfully, Toast.LENGTH_SHORT).show();
            } else {
                throw new Exception("Invalid recipe format.");
            }
        } catch (Exception e) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.import_failed)
                    .setMessage(R.string.invalid_recipe_format)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void readBackupFromFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Recipe>>(){}.getType();
            List<Recipe> restoredRecipes = gson.fromJson(stringBuilder.toString(), listType);

            if (restoredRecipes != null) {
                dataSource.deleteAllRecipes();
                for (Recipe recipe : restoredRecipes) {
                    dataSource.createRecipe(recipe);
                }
            }

            loadRecipes();
            Toast.makeText(this, R.string.restore_successful, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        adapter = new RecipeAdapter(recipes,
                recipe -> {
                    Intent intent = new Intent(MainActivity.this, CalculateActivity.class);
                    intent.putExtra("recipe", recipe);
                    startActivity(intent);
                },
                this::showDeleteConfirmationDialog,
                this::showShareMenu
        );
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecipes.setAdapter(adapter);
    }

    private void showShareMenu(Recipe recipe, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.share_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_share_text) {
                shareRecipeAsText(recipe);
                return true;
            } else if (itemId == R.id.action_share_qr) {
                shareRecipeAsQr(recipe);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void shareRecipeAsText(Recipe recipe) {
        StringBuilder shareText = new StringBuilder();
        shareText.append(getString(R.string.recipe_share_header, recipe.getName())).append("\n\n");
        shareText.append(getString(R.string.bakers_percentages_header)).append("\n");

        for (Ingredient ingredient : recipe.getIngredients()) {
            shareText.append("- ").append(ingredient.getName()).append(": ").append(ingredient.getWeight()).append("%\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bakers_recipe_subject, recipe.getName()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_recipe_via)));
    }

    private void shareRecipeAsQr(Recipe recipe) {
        // This strategy tells Gson to skip the 'id' and 'lastTotalWeight' fields during serialization
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("id") || f.getName().equals("lastTotalWeight");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(strategy)
                .create();

        String json = gson.toJson(recipe);

        Intent intent = new Intent(this, DisplayQrActivity.class);
        intent.putExtra("RECIPE_JSON", json);
        startActivity(intent);
    }

    private void loadRecipes() {
        recipes = dataSource.getAllRecipes();
        adapter.setRecipes(recipes);
    }

    private void showDeleteConfirmationDialog(Recipe recipe) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(getString(R.string.delete_confirmation_message, recipe.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteRecipe(recipe);
                })
                .show();
    }

    private void deleteRecipe(Recipe recipe) {
        dataSource.deleteRecipe(recipe.getId());
        loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataSource.close();
    }
}
