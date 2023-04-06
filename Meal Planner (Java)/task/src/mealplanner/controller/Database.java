package mealplanner.controller;

import mealplanner.Main;
import mealplanner.model.Meal;
import mealplanner.model.MealCategory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class Database {
    private static String DB_CREATE_URL;
    private static String DB_APP_URL;
    private static String USER;
    private static String PASS;
    private final Map<String, Integer> ingredientNameToId;
    private int sequenceMealsId;
    private int sequenceIngredientsId;
    private Connection dbConnection;
    public Database() {

        try {
            Properties properties = new Properties();
            final InputStream inputStream = Main.class.getResourceAsStream("/db.properties");
            properties.load(inputStream);
            inputStream.close();

            DB_CREATE_URL = properties.getProperty("jdbc.createurl");
            DB_APP_URL = properties.getProperty("jdbc.url");
            USER = properties.getProperty("jdbc.username");
            PASS = properties.getProperty("jdbc.password");

            dbConnection = DriverManager.getConnection(DB_CREATE_URL, USER, PASS);
        } catch (SQLException | IOException e) {
            System.out.println("Database connection error, application shutting down.");
            e.printStackTrace();
            System.exit(1);
        }

        ingredientNameToId = new HashMap<>();
    }

    private void createAppDatabase() {
        try {
            Statement statement = dbConnection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT FROM pg_database WHERE datname = 'meals_db'");
            if (!resultSet.isBeforeFirst()) { // check if result set is empty
                statement.executeUpdate("create database meals_db");
                statement.executeUpdate("alter role postgres with password '1111'");
                statement.executeUpdate("grant all privileges on database meals_db to postgres");
            }

            statement.close();
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("Error while creating application database. Application shutting down.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createAppTables() {
        try {
            dbConnection = DriverManager.getConnection(DB_APP_URL, USER, PASS);
            Statement statement = dbConnection.createStatement();
            // meals: category (varchar), meal (varchar), meal_id (integer)
            statement.executeUpdate("create table if not exists meals (" +
                    "category varchar(1024) NOT NULL," +
                    "meal varchar(1024) NOT NULL," +
                    "meal_id integer" +
                    ")");
            statement.close();

            statement = dbConnection.createStatement();
            // ingredients; ingredient (varchar), ingredient_id (integer), meal_id (integer)
            statement.executeUpdate("create table if not exists ingredients (" +
                    "ingredient varchar(1024) NOT NULL," +
                    "ingredient_id integer," +
                    "meal_id integer" +
                    ")");
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error while creating application database tables. Application shutting down.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void initSequences() {
        try {
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("select max(meal_id) from meals");
            if (!resultSet.isBeforeFirst()) { // check if result set is empty
                sequenceMealsId = 0;
            } else {
                resultSet.next();
                sequenceMealsId = resultSet.getInt(1);
            }
            statement.close();

            statement = dbConnection.createStatement();
            resultSet = statement.executeQuery("select max(ingredient_id) from ingredients");
            if (!resultSet.isBeforeFirst()) { // check if result set is empty
                sequenceIngredientsId = 0;
            } else {
                resultSet.next();
                sequenceIngredientsId = resultSet.getInt(1);
            }
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error while initializing in-memory sequences. Application shutting down.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void initDbStructures() {
        createAppDatabase();
        createAppTables();
        initSequences();
        initIngredientCache();
    }

    private void initIngredientCache() {
        try {
            dbConnection = DriverManager.getConnection(DB_APP_URL, USER, PASS);
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("select ingredient, ingredient_id from ingredients");
            if (resultSet.isBeforeFirst()) { // if result set is NOT empty
                while (resultSet.next()) {
                    ingredientNameToId.put(resultSet.getString(1), resultSet.getInt(2));
                }
            }
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error while rebuilding in-memory ingredient cache.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void persistMeal(Meal meal) {
        try {
            dbConnection.setAutoCommit(false);
            String sql = "insert into meals (category, meal, meal_id) values (?, ?, ?)";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setString(1, meal.getMealCategory().toString().toLowerCase());
            preparedStatement.setString(2, meal.getMealName());
            preparedStatement.setInt(3, nextMealId());
            preparedStatement.executeUpdate();

            try {
                persistIngredients(meal.getMealIngredients(), sequenceMealsId);
                dbConnection.commit();
            } catch (SQLException e) {
                dbConnection.rollback();
                System.out.println("TX error:");
                e.printStackTrace();
            }
            preparedStatement.close();
        } catch (SQLException e) {
            System.out.println("Error while writing meal data to database. Application shutting down.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void persistIngredients(String[] ingredients, int mealId) throws SQLException {
        String sql = "insert into ingredients (ingredient, ingredient_id, meal_id) values (?, ?, ?)";
        PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
        for (String ingredient : ingredients) {
            if (!ingredientNameToId.containsKey(ingredient)) {
                ingredientNameToId.put(ingredient, nextIngredientId());
            }
            preparedStatement.setString(1, ingredient);
            preparedStatement.setInt(2, ingredientNameToId.get(ingredient));
            preparedStatement.setInt(3, mealId);
            preparedStatement.executeUpdate();
        }
        preparedStatement.close();
    }

    private int nextMealId() {
        return ++sequenceMealsId;
    }

    private int nextIngredientId() {
        return  ++sequenceIngredientsId;
    }

    public boolean isEmpty() {
        try {
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("select meal from meals");
            if (resultSet.isBeforeFirst()) { // if result set is NOT empty
                statement.close();
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error while checking whether DB has entries.");
            e.printStackTrace();
            System.exit(1);
        }
        return true;
    }

    public List<Meal> getAllMeals() {
        List<Meal> meals = new ArrayList<>();
        try {
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("select category, meal, meal_id from meals");
            while (resultSet.next()) {
                MealCategory mealCategory = MealCategory.valueOf(resultSet.getString(1).toUpperCase());
                String mealName = resultSet.getString(2);
                int mealId = resultSet.getInt(3);
                String[] mealIngredients = getIngredientsForMeal(mealId);
                Meal meal = new Meal(mealCategory, mealName, mealIngredients);
                meals.add(meal);
            }
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error while reading meals from the database.");
            e.printStackTrace();
            System.exit(1);
        }
        return meals;
    }

    private String[] getIngredientsForMeal(int mealId) {
        List<String> ingredients = new ArrayList<>();
        try {
            String sql = "select ingredient from ingredients where meal_id = ?";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setInt(1, mealId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ingredients.add(resultSet.getString(1));
            }
            preparedStatement.close();
        } catch (SQLException e) {
            System.out.println("Error while reading ingredients from the database.");
            e.printStackTrace();
            System.exit(1);
        }
        return ingredients.toArray(new String[] {}); // create array from list // TODO consider refactoring ingredients to List to avoid these conversions
    }

    public void closeConnections() {
        try {
            dbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
