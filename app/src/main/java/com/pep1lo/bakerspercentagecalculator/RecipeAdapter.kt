package com.pep1lo.bakerspercentagecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pep1lo.bakerspercentagecalculator.databinding.ItemRecipeBinding

class RecipeAdapter(
    private var recipes: List<Recipe>,
    private val clickListener: (Recipe) -> Unit,
    private val deleteClickListener: (Recipe) -> Unit,
    private val shareClickListener: (Recipe, View) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val currentRecipe = recipes[position]
        holder.bind(currentRecipe)
    }

    override fun getItemCount(): Int = recipes.size

    fun setRecipes(recipes: List<Recipe>) {
        this.recipes = recipes
        notifyDataSetChanged()
    }

    inner class RecipeViewHolder(private val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.textViewRecipeName.text = recipe.name
            binding.root.setOnClickListener { clickListener(recipe) }
            binding.buttonDeleteRecipe.setOnClickListener { deleteClickListener(recipe) }
            binding.buttonShareRecipe.setOnClickListener { shareClickListener(recipe, it) }
        }
    }
}
