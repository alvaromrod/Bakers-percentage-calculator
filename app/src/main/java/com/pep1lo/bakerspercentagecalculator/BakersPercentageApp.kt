package com.pep1lo.bakerspercentagecalculator

import android.app.Application
import com.google.android.material.color.DynamicColors

class BakersPercentageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
