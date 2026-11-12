package com.example.recipeapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.recipeapp.data.local.FavoritesManager
import com.example.recipeapp.databinding.ActivityFavoritesBinding
import com.example.recipeapp.ui.adapter.RecipeGridAdapter

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: RecipeGridAdapter
    private lateinit var favoritesManager: FavoritesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.favRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        favoritesManager = FavoritesManager.getInstance(this)

        binding.btnFavBack.setOnClickListener {
            finish()
        }

        adapter = RecipeGridAdapter(
            onItemClick = { meal ->
                val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, meal.idMeal)
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_SUMMARY, meal)
                }
                startActivity(intent)
            },
            onFavoriteClick = { _, _ ->
                // Refresh list when item is unfavorited
                loadFavorites()
            }
        )

        binding.rvFavoritesGrid.layoutManager = GridLayoutManager(this, 2)
        binding.rvFavoritesGrid.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val list = favoritesManager.getAllFavorites()
        adapter.submitList(list)

        if (list.isEmpty()) {
            binding.layoutFavEmpty.visibility = View.VISIBLE
            binding.rvFavoritesGrid.visibility = View.GONE
            binding.tvFavCount.text = "0 saved"
        } else {
            binding.layoutFavEmpty.visibility = View.GONE
            binding.rvFavoritesGrid.visibility = View.VISIBLE
            binding.tvFavCount.text = "${list.size} saved"
        }
    }
}
