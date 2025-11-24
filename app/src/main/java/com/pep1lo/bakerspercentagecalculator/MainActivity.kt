package com.pep1lo.bakerspercentagecalculator

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.pep1lo.bakerspercentagecalculator.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataSource: RecipeDataSource
    private lateinit var adapter: RecipeAdapter
    private var recipes: List<Recipe> = ArrayList()

    private val addRecipeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            loadRecipes()
        }
    }

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.data?.let { uri ->
                writeBackupToFile(uri)
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.data?.let { uri ->
                readBackupFromFile(uri)
            }
        }
    }

    private val qrCodeScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            importRecipeFromJson(result.contents)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.data?.let { imageUri ->
                scanQrCodeFromImage(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        dataSource = RecipeDataSource.getInstance(this)
        try {
            dataSource.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setupRecyclerView()
        loadRecipes()

        binding.fabAddRecipe.setOnClickListener {
            val intent = Intent(this@MainActivity, AddEditRecipeActivity::class.java)
            addRecipeLauncher.launch(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_backup -> {
                initiateBackup()
                true
            }
            R.id.action_restore -> {
                initiateRestore()
                true
            }
            R.id.action_import_recipe -> {
                showImportDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showImportDialog() {
        val options = arrayOf<CharSequence>(
            getString(R.string.scan_with_camera),
            getString(R.string.choose_from_gallery)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.import_recipe)
        builder.setItems(options) { _, which ->
            if (options[which] == getString(R.string.scan_with_camera)) {
                initiateScan()
            } else if (options[which] == getString(R.string.choose_from_gallery)) {
                initiateImagePick()
            }
        }
        builder.show()
    }

    private fun initiateImagePick() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun scanQrCodeFromImage(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) {
                Toast.makeText(this, R.string.qr_code_not_found, Toast.LENGTH_SHORT).show()
                return
            }
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            importRecipeFromJson(result.text)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.qr_code_not_found, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun initiateScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a recipe QR code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        qrCodeScannerLauncher.launch(options)
    }

    private fun initiateBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "bakers_percentage_backup.json")
        backupLauncher.launch(intent)
    }

    private fun writeBackupToFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(recipes)
                contentResolver.openOutputStream(uri)?.write(json.toByteArray())
                Toast.makeText(this@MainActivity, R.string.backup_successful, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateRestore() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.restore_confirmation_title)
            .setMessage(R.string.restore_confirmation_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.restore) { _, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "application/json"
                restoreLauncher.launch(intent)
            }
            .show()
    }

    private fun importRecipeFromJson(jsonString: String) {
        val gson = Gson()
        lifecycleScope.launch {
            try {
                val recipe = gson.fromJson(jsonString, Recipe::class.java)

                if (recipe != null && !recipe.name.isNullOrEmpty()) {
                    dataSource.createRecipe(recipe)
                    loadRecipes()
                    Toast.makeText(this@MainActivity, R.string.recipe_imported_successfully, Toast.LENGTH_SHORT).show()
                } else {
                    throw Exception("Invalid recipe format.")
                }
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.import_failed)
                    .setMessage(R.string.invalid_recipe_format)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun readBackupFromFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                inputStream?.close()

                val gson = Gson()
                val listType = object : TypeToken<ArrayList<Recipe>>() {}.type
                val restoredRecipes: List<Recipe> = gson.fromJson(stringBuilder.toString(), listType)

                if (restoredRecipes != null) {
                    dataSource.deleteAllRecipes()
                    for (recipe in restoredRecipes) {
                        dataSource.createRecipe(recipe)
                    }
                }

                loadRecipes()
                Toast.makeText(this@MainActivity, R.string.restore_successful, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.restore_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = RecipeAdapter(recipes,
            { recipe ->
                val intent = Intent(this@MainActivity, CalculateActivity::class.java)
                intent.putExtra("recipe", recipe)
                startActivity(intent)
            },
            { recipe -> showDeleteConfirmationDialog(recipe) },
            { recipe, view -> showShareMenu(recipe, view) }
        )
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = adapter
    }

    private fun showShareMenu(recipe: Recipe, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.share_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share_text -> {
                    shareRecipeAsText(recipe)
                    true
                }
                R.id.action_share_qr -> {
                    shareRecipeAsQr(recipe)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun shareRecipeAsText(recipe: Recipe) {
        val shareText = StringBuilder()
        shareText.append(getString(R.string.recipe_share_header, recipe.name)).append("\n\n")
        shareText.append(getString(R.string.bakers_percentages_header)).append("\n")

        for (ingredient in recipe.ingredients) {
            shareText.append("- ").append(ingredient.name).append(": ").append(ingredient.weight).append("%\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bakers_recipe_subject, recipe.name))
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString())

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_recipe_via)))
    }

    private fun shareRecipeAsQr(recipe: Recipe) {
        val strategy = object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.name == "id" || f.name == "lastTotalWeight"
            }

            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                return false
            }
        }

        val gson = GsonBuilder()
            .addSerializationExclusionStrategy(strategy)
            .create()

        val json = gson.toJson(recipe)

        val intent = Intent(this, DisplayQrActivity::class.java)
        intent.putExtra("RECIPE_JSON", json)
        startActivity(intent)
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            recipes = dataSource.getAllRecipes()
            adapter.setRecipes(recipes)

            if (recipes.isEmpty()) {
                binding.recyclerViewRecipes.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewRecipes.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(recipe: Recipe) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(getString(R.string.delete_confirmation_message, recipe.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteRecipe(recipe)
            }
            .show()
    }

    private fun deleteRecipe(recipe: Recipe) {
        lifecycleScope.launch {
            dataSource.deleteRecipe(recipe.id)
            loadRecipes()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecipes()
    }

    override fun onDestroy() {
        super.onDestroy()
        dataSource.close()
    }
}
