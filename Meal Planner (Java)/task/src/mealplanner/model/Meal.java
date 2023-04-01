package mealplanner.model;

import java.util.Arrays;

public class Meal {
    private final MealCategory mealCategory;
    private final String mealName;
    private final String[] mealIngredients;

    public Meal(MealCategory mealCategory, String mealName, String mealIngredients) {
        this.mealCategory = mealCategory;
        this.mealName = mealName;
        this.mealIngredients = mealIngredients.split(",\\s*");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Category: %s%n", this.mealCategory.toString().toLowerCase()));
        sb.append(String.format("Name: %s%n", this.mealName));
        sb.append("Ingredients:\n");
        Arrays.stream(this.mealIngredients).forEach(i -> sb.append(String.format("%s%n", i)));

        return sb.toString();
    }
}
