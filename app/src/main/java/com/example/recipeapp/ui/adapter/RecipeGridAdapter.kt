package com.example.recipeapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.data.local.FavoritesManager
import com.example.recipeapp.data.model.MealSummary
import com.example.recipeapp.databinding.ItemRecipeCardBinding

class RecipeGridAdapter(
    private val onItemClick: (MealSummary) -> Unit,
    private val onFavoriteClick: (MealSummary, Boolean) -> Unit
) : ListAdapter<MealSummary, RecipeGridAdapter.RecipeViewHolder>(DIFF_CALLBACK) {

    inner class RecipeViewHolder(val binding: ItemRecipeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: MealSummary) {
            binding.tvRecipeTitle.text = meal.strMeal

            Glide.with(binding.ivRecipeThumb.context)
                .load(meal.strMealThumb)
                .placeholder(R.drawable.ic_restaurant)
                .error(R.drawable.ic_restaurant)
                .centerCrop()
                .into(binding.ivRecipeThumb)

            val favManager = FavoritesManager.getInstance(binding.root.context)
            var isFav = favManager.isFavorite(meal.idMeal)
            updateFavIcon(isFav)

            binding.btnCardFavorite.setOnClickListener {
                isFav = favManager.toggleFavorite(meal)
                updateFavIcon(isFav)
                onFavoriteClick(meal, isFav)
            }

            binding.root.setOnClickListener {
                onItemClick(meal)
            }
        }

        private fun updateFavIcon(isFav: Boolean) {
            if (isFav) {
                binding.btnCardFavorite.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                binding.btnCardFavorite.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MealSummary>() {
            override fun areItemsTheSame(oldItem: MealSummary, newItem: MealSummary): Boolean {
                return oldItem.idMeal == newItem.idMeal
            }

            override fun areContentsTheSame(oldItem: MealSummary, newItem: MealSummary): Boolean {
                return oldItem == newItem
            }
        }
    }
}
