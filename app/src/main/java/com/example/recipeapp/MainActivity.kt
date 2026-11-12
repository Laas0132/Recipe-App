package com.example.recipeapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.recipeapp.data.api.ApiClient
import com.example.recipeapp.data.auth.AuthManager
import com.example.recipeapp.data.model.MealSummary
import com.example.recipeapp.databinding.ActivityMainBinding
import com.example.recipeapp.ui.AuthActivity
import com.example.recipeapp.ui.FavoritesActivity
import com.example.recipeapp.ui.RecipeDetailActivity
import com.example.recipeapp.ui.ScanIngredientActivity
import com.example.recipeapp.ui.adapter.RecipeGridAdapter
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager
    private lateinit var recipeAdapter: RecipeGridAdapter

    private var currentQuery: String = "Chicken"
    private var isIngredientSearch: Boolean = false

    // Scan ingredient launcher
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val query = result.data?.getStringExtra(ScanIngredientActivity.EXTRA_SEARCH_QUERY)
            if (!query.isNullOrBlank()) {
                binding.etSearchQuery.setText(query)
                searchRecipes(query, isIngredient = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        if (!authManager.isLoggedIn) {
            navigateToAuth()
            return
        }

        setupUserHeader()
        setupRecyclerView()
        setupSearch()
        setupChips()
        setupActions()

        // Initial default load
        searchRecipes(currentQuery, isIngredient = false)
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorite heart states on cards
        recipeAdapter.notifyDataSetChanged()
    }

    private fun setupUserHeader() {
        val user = authManager.currentUser
        val email = user?.email
        binding.tvUserEmail.text = when {
            !email.isNullOrBlank() -> email
            user?.isAnonymous == true || authManager.isGuestMode -> "Guest User"
            else -> "Guest User"
        }

        binding.btnNavLogout.setOnClickListener {
            authManager.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            navigateToAuth()
        }

        binding.btnNavFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeGridAdapter(
            onItemClick = { meal ->
                val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, meal.idMeal)
                    putExtra(RecipeDetailActivity.EXTRA_RECIPE_SUMMARY, meal)
                }
                startActivity(intent)
            },
            onFavoriteClick = { meal, isFav ->
                val msg = if (isFav) "Added ${meal.strMeal} to favourites" else "Removed from favourites"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvRecipesGrid.layoutManager = GridLayoutManager(this, 2)
        binding.rvRecipesGrid.adapter = recipeAdapter
    }

    private fun setupSearch() {
        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.etSearchQuery.text.toString().trim()
                if (query.isNotBlank()) {
                    searchRecipes(query, isIngredient = false)
                }
                true
            } else {
                false
            }
        }

        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearch.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.text?.clear()
            searchRecipes("Chicken", isIngredient = false)
        }

        binding.btnRetry.setOnClickListener {
            searchRecipes(currentQuery, isIngredientSearch)
        }
    }

    private fun setupChips() {
        binding.chipChicken.setOnClickListener { onChipClicked("Chicken") }
        binding.chipBeef.setOnClickListener { onChipClicked("Beef") }
        binding.chipPasta.setOnClickListener { onChipClicked("Pasta") }
        binding.chipTomato.setOnClickListener { onChipClicked("Tomato") }
        binding.chipEgg.setOnClickListener { onChipClicked("Egg") }
        binding.chipRice.setOnClickListener { onChipClicked("Rice") }
        binding.chipPotato.setOnClickListener { onChipClicked("Potato") }
    }

    private fun onChipClicked(term: String) {
        binding.etSearchQuery.setText(term)
        searchRecipes(term, isIngredient = false)
    }

    private fun setupActions() {
        binding.cardSnapIngredient.setOnClickListener {
            val intent = Intent(this, ScanIngredientActivity::class.java)
            scanLauncher.launch(intent)
        }
    }

    private fun searchRecipes(query: String, isIngredient: Boolean) {
        currentQuery = query
        isIngredientSearch = isIngredient

        binding.progressBarMain.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE
        binding.rvRecipesGrid.visibility = View.GONE
        binding.tvSectionTitle.text = "Results for \"$query\""
        binding.tvResultsCount.text = ""

        lifecycleScope.launch {
            try {
                var results: List<MealSummary>? = null

                if (isIngredient) {
                    // Try filtering by ingredient first
                    val ingredientRes = ApiClient.api.filterByIngredient(query)
                    results = ingredientRes.meals
                    // If no ingredient results, try searching by name as fallback
                    if (results.isNullOrEmpty()) {
                        val nameRes = ApiClient.api.searchByName(query)
                        results = nameRes.meals?.map { it.toSummary() }
                    }
                } else {
                    // Search by dish name first
                    val nameRes = ApiClient.api.searchByName(query)
                    results = nameRes.meals?.map { it.toSummary() }
                    // If no recipe name found, fallback to ingredient filter
                    if (results.isNullOrEmpty()) {
                        val ingredientRes = ApiClient.api.filterByIngredient(query)
                        results = ingredientRes.meals
                    }
                }

                binding.progressBarMain.visibility = View.GONE

                if (!results.isNullOrEmpty()) {
                    val limitedResults = results.take(2)
                    recipeAdapter.submitList(limitedResults)
                    binding.rvRecipesGrid.visibility = View.VISIBLE
                    binding.tvResultsCount.text = "${limitedResults.size} recipes"
                } else {
                    binding.tvEmptyTitle.text = getString(R.string.no_recipes_found)
                    binding.tvEmptyDesc.text = "No recipes found matching \"$query\". Try a different ingredient or dish."
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.tvResultsCount.text = "0 recipes"
                }
            } catch (e: Exception) {
                binding.progressBarMain.visibility = View.GONE
                binding.layoutErrorState.visibility = View.VISIBLE
                binding.tvResultsCount.text = ""
            }
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
