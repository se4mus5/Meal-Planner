package mealplanner.helper;

import mealplanner.model.ShoppingListItem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Util {
    public static String initCap(String text) {
        return text.substring(0, 1).toUpperCase() + text.toString().substring(1).toLowerCase();
    }

    //TODO refactor to ShoppingList -> ShoppingListItem dependency, move persist logic to ShoppingList
    public static void persistShoppingListToFile(String fileName, List<ShoppingListItem> shoppingList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))){
            for (ShoppingListItem i : shoppingList) {
                String line = i.getIngredient();
                if (i.getQuantity() > 1) {
                    line += String.format(" x%d", i.getQuantity());
                }
                writer.write(line);
                writer.newLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
