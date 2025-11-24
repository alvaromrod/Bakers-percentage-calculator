# Testing Guide

Since the app has been migrated to Kotlin and uses a local database, the best way to verify everything is working is to run the app in Android Studio and perform a few manual tests.

## 1. Build & Launch
1.  Open the project in **Android Studio**.
2.  Sync Gradle (File -> Sync Project with Gradle Files).
3.  Run the app (`Shift + F10`) on an Emulator or Physical Device.
4.  **Success Criteria:** App launches without crashing.

## 2. Verify Data Persistence (Database)
1.  **Create a Recipe:**
    -   Tap the FAB (+) button.
    -   Enter Name: "Test Bread".
    -   Add Ingredient: "Flour" (100%).
    -   Add Ingredient: "Water" (70%).
    -   Add Ingredient: "Salt" (2%).
    -   Tap "Save".
    -   **Success:** Recipe appears in the main list.
2.  **Edit a Recipe:**
    -   Tap the "Test Bread" recipe.
    -   Tap the "Edit" (Pencil) button.
    -   Change Water to 75%.
    -   Tap "Save".
    -   **Success:** The calculation screen updates with the new percentage.
3.  **Delete a Recipe:**
    -   Go back to the main list.
    -   Tap the "Delete" (Trash) icon on "Test Bread".
    -   Confirm deletion.
    -   **Success:** Recipe is removed from the list.

## 3. Verify Calculation Logic
1.  Create a recipe with: Flour (100%), Water (50%).
2.  Open the recipe.
3.  Enter "1000" in "Total Dough Weight".
4.  **Success:**
    -   Flour should be ~666.67g
    -   Water should be ~333.33g
    -   (Math: 1000 * (100/150) = 666.66...)

## 4. Verify QR Code & Sharing
1.  Open a recipe.
2.  Tap the "Share" button (top right or in menu).
3.  Select "Share as QR Code".
4.  **Success:** A QR code is generated and displayed.

## 5. Verify Backup/Restore (Optional)
1.  Create a few recipes.
2.  Menu -> Backup Recipes. Save the file.
3.  Delete all recipes manually.
4.  Menu -> Restore Recipes. Select the file.
5.  **Success:** All recipes reappear.
