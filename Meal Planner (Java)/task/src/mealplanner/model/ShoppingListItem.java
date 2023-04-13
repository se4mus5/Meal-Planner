package mealplanner.model;

public class ShoppingListItem {
    String ingredient;
    int quantity;

    public ShoppingListItem(String ingredient, int quantity) {
        this.ingredient = ingredient;
        this.quantity = quantity;
    }

    public String getIngredient() {
        return ingredient;
    }

    public int getQuantity() {
        return quantity;
    }
}
