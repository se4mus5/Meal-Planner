package mealplanner;

import java.util.Arrays;
import java.util.List;

public class Meal {
    private final String mealCategory; //TODO could be an enum
    private final String mealName;
    private final List<String> mealIngredients; //TODO could be an array, depending on future requirements

    public Meal(String mealCategory, String mealName, List<String> mealIngredients) {
        this.mealCategory = mealCategory;
        this.mealName = mealName;
        this.mealIngredients = mealIngredients;
    }

    public Meal(String mealCategory, String mealName, String mealIngredients) {
        this.mealCategory = mealCategory;
        this.mealName = mealName;
        this.mealIngredients = Arrays.stream(mealIngredients.split(",")).toList();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Category: %s%n", this.mealCategory));
        sb.append(String.format("Name: %s%n", this.mealName));
        sb.append("Ingredients:\n");
        this.mealIngredients.forEach(i -> sb.append(String.format("%s%n", i)));

        return sb.toString();
    }
}
