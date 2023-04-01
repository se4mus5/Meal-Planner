package mealplanner;

import mealplanner.model.Command;
import mealplanner.model.Meal;
import mealplanner.model.MealCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        List<Meal> meals = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        mainLoop: while (true) {
            System.out.println("What would you like to do (add, show, exit)?");
            Command command;
            try {
                command = Command.valueOf(scanner.nextLine().toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            switch (command) {
                case ADD -> { addLogic(scanner, meals); }
                case SHOW -> { showLogic(scanner, meals); }
                case EXIT -> { System.out.println("Bye!"); break mainLoop;}
            }
        }
    }

    private static void showLogic(Scanner scanner, List<Meal> meals) {
        if (meals.isEmpty()) {
            System.out.println("No meals saved. Add a meal first.");
        } else {
            System.out.println();
            meals.forEach(System.out::println);
        }
    }

    private static void addLogic(Scanner scanner, List<Meal> meals) {
        MealCategory mealCategory = null;
        do {
            System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
            try {
                mealCategory = MealCategory.valueOf(scanner.nextLine().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            }
        } while (mealCategory == null);

        String mealName;
        while (true) {
            System.out.println("Input the meal's name:");
            mealName = scanner.nextLine();
            if (!mealName.matches("[a-zA-Z]+\\s*[a-zA-Z]*")) {
                System.out.println("Wrong format. Use letters only!");
            } else {
                break;
            }
        }

        String mealIngredients;
        while (true) {
            System.out.println("Input the ingredients:");
            mealIngredients = scanner.nextLine();
            if (!mealIngredients.matches("([a-zA-Z][a-zA-Z ]*,\\s*)*[a-zA-Z][a-zA-Z ]+")) {
                System.out.println("Wrong format. Use letters only!");
            } else {
                break;
            }
        }

        meals.add(new Meal(mealCategory, mealName, mealIngredients));
        System.out.println("The meal has been added!");
    }
}