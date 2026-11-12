package com.example.recipeapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.recipeapp.data.model.MealSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "recipe_favorites_prefs"
        private const val KEY_FAVORITES = "key_favorites_list"

        @Volatile
        private var INSTANCE: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FavoritesManager(context).also { INSTANCE = it }
            }
        }
    }

    fun getAllFavorites(): List<MealSummary> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MealSummary>>() {}.type
            gson.fromJson<List<MealSummary>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isFavorite(mealId: String): Boolean {
        return getAllFavorites().any { it.idMeal == mealId }
    }

    fun addFavorite(meal: MealSummary) {
        val current = getAllFavorites().toMutableList()
        if (current.none { it.idMeal == meal.idMeal }) {
            current.add(0, meal)
            saveFavorites(current)
        }
    }

    fun removeFavorite(mealId: String) {
        val current = getAllFavorites().toMutableList()
        val changed = current.removeAll { it.idMeal == mealId }
        if (changed) {
            saveFavorites(current)
        }
    }

    fun toggleFavorite(meal: MealSummary): Boolean {
        return if (isFavorite(meal.idMeal)) {
            removeFavorite(meal.idMeal)
            false
        } else {
            addFavorite(meal)
            true
        }
    }

    private fun saveFavorites(list: List<MealSummary>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
    }
}
