package com.example.pocketexpense.util

import android.content.Context
import android.content.SharedPreferences

object OnboardingPreferences {
    private const val PREFS_NAME = "pocket_expense_onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "has_completed_permission_onboarding"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun resetOnboarding(context: Context) {
        getPrefs(context).edit().remove(KEY_ONBOARDING_COMPLETED).apply()
    }
}
