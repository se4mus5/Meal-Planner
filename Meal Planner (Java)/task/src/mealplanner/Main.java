package mealplanner;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        String mealType = scanner.nextLine();
        System.out.println("Input the meal's name:");
        String mealName = scanner.nextLine();
        System.out.println("Input the ingredients:");
        String mealIngredients = scanner.nextLine();

        Meal meal = new Meal(mealType, mealName, mealIngredients);

        System.out.println(meal);
        System.out.println("The meal has been added!");
    }
}