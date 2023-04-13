package mealplanner;

import mealplanner.controller.Database;
import mealplanner.model.*;

import java.util.List;
import java.util.Scanner;

import static mealplanner.helper.Util.initCap;
import static mealplanner.helper.Util.persistShoppingListToFile;

public class Main {
    public static void main(String[] args) {
        Database db = new Database();
        db.initDbStructures();
        Scanner scanner = new Scanner(System.in);

        mainLoop: while (true) {
            System.out.println("What would you like to do (add, show, plan, save, exit)?");
            Command command;
            try {
                command = Command.valueOf(scanner.nextLine().toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            switch (command) {
                case ADD -> addLogic(scanner, db);
                case SHOW -> showLogic(scanner, db);
                case PLAN -> planLogic(scanner, db);
                case SAVE -> saveLogic(scanner, db);
                case EXIT -> { db.closeConnections(); System.out.println("Bye!"); break mainLoop;}
            }
        }
    }

    private static void saveLogic(Scanner scanner, Database db) {
        if (db.isPlanTableEmpty()) {
            System.out.println("Unable to save. Plan your meals first.");
        } else {
            System.out.println("Input a filename:");
            String fileName = scanner.nextLine();
            List<ShoppingListItem> shoppingList = db.createShoppingList();
            persistShoppingListToFile(fileName, shoppingList);
            System.out.println("Saved!");
        }
    }

    private static MealCategory getUserInput(Scanner scanner, String subject, String action) {
        MealCategory mealCategory = null;
        do {
            System.out.printf("Which %s do you want to %s (breakfast, lunch, dinner)?%n", subject, action);
            try {
                mealCategory = MealCategory.valueOf(scanner.nextLine().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            }
        } while (mealCategory == null);

        return mealCategory;
    }

    private static void planLogic(Scanner scanner, Database db) {

        db.clearPlan();
        for (Weekday weekday : Weekday.values()) {
            String dayString = initCap(weekday.name());
            System.out.println(dayString);
            for (MealCategory mealCategory : MealCategory.values()) {
                List<Meal> meals = db.getMealsByCategory(mealCategory, true);
                meals.stream().map(Meal::getMealName).forEach(System.out::println);
                System.out.printf("Choose the %s for %s from the list above:%n",
                        mealCategory.toString().toLowerCase(), dayString);

                String mealChoice = scanner.nextLine();
                while (!isMealValid(meals, mealChoice)) {
                    System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                    mealChoice = scanner.nextLine();
                }
                db.persistPlanLineItem(mealCategory, mealChoice, weekday);
            }

            System.out.printf("Yeah! We planned the meals for %s.%n%n", dayString);
        }

        printPlan(db);
    }

    public static boolean isMealValid(List<Meal> meals, String mealChoice) {
        return meals.stream().map(Meal::getMealName).anyMatch(m -> m.equals(mealChoice));
    }

    private static void printPlan(Database db) {
        // print format
        // Monday
        // Breakfast: yogurt
        // Lunch: tomato salad
        // Dinner: ramen
        List<String> mealPlan = db. getMealPlan();
        int foodIndex = 0;
        for (Weekday weekday : Weekday.values()) {
            System.out.println(initCap(weekday.name()));
            for (int i = 0; i < 3; i++){
                System.out.println(mealPlan.get(foodIndex++));
            }
            System.out.println();
        }
    }

    private static void showLogic(Scanner scanner, Database db) {
        MealCategory mealCategory = getUserInput(scanner, "category", "print");

        List<Meal> mealsFound = db.getMealsByCategory(mealCategory, false);
        if (mealsFound.isEmpty()) {
            System.out.println("No meals found.");
        } else {
            System.out.printf("Category: %s%n", mealCategory.toString().toLowerCase());
            mealsFound.forEach(System.out::println);
        }
    }

    private static void addLogic(Scanner scanner, Database db) {
        MealCategory mealCategory = getUserInput(scanner, "meal", "add");

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

        db.persistMeal(new Meal(mealCategory, mealName, mealIngredients));
        System.out.println("The meal has been added!");
    }
}