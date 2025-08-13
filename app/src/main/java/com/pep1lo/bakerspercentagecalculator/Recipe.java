package com.pep1lo.bakerspercentagecalculator;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.ArrayList;

public class Recipe implements Parcelable {
    private long id;

    @SerializedName(value="name", alternate={"recipe_name", "recipeName"})
    private String name;

    @SerializedName(value="ingredients", alternate={"ingredient_list", "ingredientList"})
    private List<Ingredient> ingredients;

    private double lastTotalWeight;

    @SerializedName("unit")
    private String unit;

    @SerializedName("notes")
    private String notes; // New field for notes

    @SerializedName("ovenTempTime")
    private String ovenTempTime; // New field for oven instructions

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.unit = "grams"; // Default unit
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }
    public double getLastTotalWeight() { return lastTotalWeight; }
    public void setLastTotalWeight(double lastTotalWeight) { this.lastTotalWeight = lastTotalWeight; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getNotes() { return notes; } // New getter
    public void setNotes(String notes) { this.notes = notes; } // New setter
    public String getOvenTempTime() { return ovenTempTime; } // New getter
    public void setOvenTempTime(String ovenTempTime) { this.ovenTempTime = ovenTempTime; } // New setter

    @Override
    public String toString() {
        return name;
    }

    // --- Parcelable Implementation (Updated) ---
    protected Recipe(Parcel in) {
        id = in.readLong();
        name = in.readString();
        lastTotalWeight = in.readDouble();
        unit = in.readString();
        notes = in.readString(); // Read new field
        ovenTempTime = in.readString(); // Read new field
        ingredients = new ArrayList<>();
        in.readList(ingredients, Ingredient.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeDouble(lastTotalWeight);
        dest.writeString(unit);
        dest.writeString(notes); // Write new field
        dest.writeString(ovenTempTime); // Write new field
        dest.writeList(ingredients);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Recipe> CREATOR = new Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };
}
