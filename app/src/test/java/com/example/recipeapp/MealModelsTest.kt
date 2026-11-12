package com.example.recipeapp

import com.example.recipeapp.data.model.MealDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class MealModelsTest {

    @Test
    fun testGetIngredientsWithMeasures_filtersBlanks() {
        val meal = MealDetail(
            idMeal = "12345",
            strMeal = "Test Meal",
            strCategory = "Test",
            strArea = "World",
            strInstructions = "Cook it well",
            strMealThumb = "https://example.com/thumb.jpg",
            strYoutube = "https://youtube.com/watch?v=123",
            strIngredient1 = "Chicken",
            strMeasure1 = "500g",
            strIngredient2 = "Salt",
            strMeasure2 = "1 tsp",
            strIngredient3 = "",
            strMeasure3 = "",
            strIngredient4 = "  ",
            strMeasure4 = "pinch",
            strIngredient5 = null,
            strMeasure5 = null
        )

        val pairs = meal.getIngredientsWithMeasures()
        assertEquals(2, pairs.size)
        assertEquals("Chicken" to "500g", pairs[0].first to pairs[0].second)
        assertEquals("Salt" to "1 tsp", pairs[1].first to pairs[1].second)
    }

    @Test
    fun testToSummary_createsValidSummary() {
        val meal = MealDetail(
            idMeal = "999",
            strMeal = "Pizza",
            strCategory = "Italian",
            strArea = "Italy",
            strInstructions = "Bake",
            strMealThumb = "https://example.com/pizza.jpg",
            strYoutube = null
        )

        val summary = meal.toSummary()
        assertEquals("999", summary.idMeal)
        assertEquals("Pizza", summary.strMeal)
        assertEquals("https://example.com/pizza.jpg", summary.strMealThumb)
    }
}
