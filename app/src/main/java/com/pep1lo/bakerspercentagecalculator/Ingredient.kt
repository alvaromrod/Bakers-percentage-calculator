package com.pep1lo.bakerspercentagecalculator

import java.io.Serializable
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient(
    @SerializedName("name")
    var name: String,
    @SerializedName("weight")
    var weight: Double
) : Parcelable, Serializable
