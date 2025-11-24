package com.pep1lo.bakerspercentagecalculator

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    var id: Long = 0,

    @SerializedName(value = "name", alternate = ["recipe_name", "recipeName"])
    var name: String? = null,

    @SerializedName(value = "ingredients", alternate = ["ingredient_list", "ingredientList"])
    var ingredients: MutableList<Ingredient> = ArrayList(),

    var lastTotalWeight: Double = 0.0,

    @SerializedName("unit")
    var unit: String = "grams",

    @SerializedName("notes")
    var notes: String? = null,

    @SerializedName("ovenTempTime")
    var ovenTempTime: String? = null
) : Parcelable {
    override fun toString(): String {
        return name ?: ""
    }
}
