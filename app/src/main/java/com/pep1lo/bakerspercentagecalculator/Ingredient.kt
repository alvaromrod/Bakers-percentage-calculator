package com.pep1lo.bakerspercentagecalculator

import java.io.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ingredient(
    var name: String,
    var weight: Double
) : Parcelable, Serializable
