package com.pep1lo.bakerspercentagecalculator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pep1lo.bakerspercentagecalculator.R;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private final OnItemClickListener clickListener;
    private final OnDeleteClickListener deleteClickListener;
    private final OnShareClickListener shareClickListener;

    public interface OnItemClickListener {
        void onItemClick(Recipe recipe);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Recipe recipe);
    }

    public interface OnShareClickListener {
        void onShareClick(Recipe recipe, View anchor);
    }

    public RecipeAdapter(List<Recipe> recipes, OnItemClickListener clickListener, OnDeleteClickListener deleteClickListener, OnShareClickListener shareClickListener) {
        this.recipes = recipes;
        this.clickListener = clickListener;
        this.deleteClickListener = deleteClickListener;
        this.shareClickListener = shareClickListener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe currentRecipe = recipes.get(position);
        holder.textViewRecipeName.setText(currentRecipe.getName());
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(currentRecipe));
        holder.buttonDeleteRecipe.setOnClickListener(v -> deleteClickListener.onDeleteClick(currentRecipe));
        holder.buttonShareRecipe.setOnClickListener(v -> shareClickListener.onShareClick(currentRecipe, v));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView textViewRecipeName;
        final ImageButton buttonDeleteRecipe;
        final ImageButton buttonShareRecipe;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRecipeName = itemView.findViewById(R.id.textViewRecipeName);
            buttonDeleteRecipe = itemView.findViewById(R.id.buttonDeleteRecipe);
            buttonShareRecipe = itemView.findViewById(R.id.buttonShareRecipe);
        }
    }
}
