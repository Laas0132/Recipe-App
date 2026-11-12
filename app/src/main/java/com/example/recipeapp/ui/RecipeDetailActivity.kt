package com.example.recipeapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.data.api.ApiClient
import com.example.recipeapp.data.local.FavoritesManager
import com.example.recipeapp.data.model.MealDetail
import com.example.recipeapp.data.model.MealSummary
import com.example.recipeapp.databinding.ActivityRecipeDetailBinding
import com.example.recipeapp.databinding.ItemIngredientMeasureBinding
import kotlinx.coroutines.launch

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipeId: String = ""
    private var currentMeal: MealDetail? = null
    private lateinit var favoritesManager: FavoritesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        favoritesManager = FavoritesManager.getInstance(this)

        recipeId = intent.getStringExtra(EXTRA_RECIPE_ID).orEmpty()
        val initialSummary = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_RECIPE_SUMMARY, MealSummary::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_RECIPE_SUMMARY) as? MealSummary
        }

        if (initialSummary != null) {
            binding.tvDetailTitle.text = initialSummary.strMeal
            Glide.with(this)
                .load(initialSummary.strMealThumb)
                .placeholder(R.drawable.ic_restaurant)
                .into(binding.ivDetailThumb)
            updateFavoriteButton(favoritesManager.isFavorite(initialSummary.idMeal))
        }

        binding.btnDetailBack.setOnClickListener {
            finish()
        }

        binding.btnRetryDetail.setOnClickListener {
            fetchRecipeDetails()
        }

        binding.btnDetailFavorite.setOnClickListener {
            val meal = currentMeal?.toSummary() ?: initialSummary
            if (meal != null) {
                val newState = favoritesManager.toggleFavorite(meal)
                updateFavoriteButton(newState)
                val msg = if (newState) "Added to favourites" else "Removed from favourites"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        if (recipeId.isNotBlank()) {
            fetchRecipeDetails()
        } else {
            showError("Invalid recipe ID")
        }
    }

    private fun fetchRecipeDetails() {
        binding.progressBarDetail.visibility = View.VISIBLE
        binding.layoutDetailError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.lookupById(recipeId)
                val meal = response.meals?.firstOrNull()
                binding.progressBarDetail.visibility = View.GONE

                if (meal != null) {
                    currentMeal = meal
                    bindMealDetail(meal)
                } else {
                    showError("Recipe not found.")
                }
            } catch (e: Exception) {
                binding.progressBarDetail.visibility = View.GONE
                showError("Network error: Could not load recipe. Please check your connection.")
            }
        }
    }

    private fun bindMealDetail(meal: MealDetail) {
        binding.scrollDetailContent.visibility = View.VISIBLE
        binding.tvDetailTitle.text = meal.strMeal

        Glide.with(this)
            .load(meal.strMealThumb)
            .placeholder(R.drawable.ic_restaurant)
            .into(binding.ivDetailThumb)

        // Category & Area
        if (!meal.strCategory.isNullOrBlank()) {
            binding.tvDetailCategory.text = meal.strCategory
            binding.tvDetailCategory.visibility = View.VISIBLE
        } else {
            binding.tvDetailCategory.visibility = View.GONE
        }

        if (!meal.strArea.isNullOrBlank()) {
            binding.tvDetailArea.text = meal.strArea
            binding.tvDetailArea.visibility = View.VISIBLE
        } else {
            binding.tvDetailArea.visibility = View.GONE
        }

        // YouTube Button
        val ytUrl = meal.strYoutube?.trim().orEmpty()
        if (ytUrl.isNotBlank()) {
            binding.btnWatchYoutube.visibility = View.VISIBLE
            binding.btnWatchYoutube.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open YouTube link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnWatchYoutube.visibility = View.GONE
        }

        // Favorite Button state
        updateFavoriteButton(favoritesManager.isFavorite(meal.idMeal))

        // Ingredients
        val ingredients = meal.getIngredientsWithMeasures()
        binding.tvIngredientsHeader.text = "Ingredients (${ingredients.size})"
        binding.layoutIngredientsList.removeAllViews()

        for (item in ingredients) {
            val itemBinding = ItemIngredientMeasureBinding.inflate(
                LayoutInflater.from(this),
                binding.layoutIngredientsList,
                false
            )
            itemBinding.tvIngredientName.text = item.first
            itemBinding.tvMeasure.text = item.second
            binding.layoutIngredientsList.addView(itemBinding.root)
        }

        // Instructions
        val instructions = meal.strInstructions?.trim().orEmpty()
        binding.tvDetailInstructions.text = if (instructions.isNotBlank()) {
            instructions
        } else {
            "No instructions provided for this recipe."
        }
    }

    private fun updateFavoriteButton(isFav: Boolean) {
        if (isFav) {
            binding.btnDetailFavorite.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnDetailFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    private fun showError(msg: String) {
        binding.layoutDetailError.visibility = View.VISIBLE
        binding.layoutDetailError.findViewById<android.widget.TextView>(R.id.tvErrorDesc)?.text = msg
    }

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
        const val EXTRA_RECIPE_SUMMARY = "extra_recipe_summary"
    }
}
