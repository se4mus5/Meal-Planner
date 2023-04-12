package mealplanner.controller;

import mealplanner.Main;
import mealplanner.model.Meal;
import mealplanner.model.MealCategory;
import mealplanner.model.Weekday;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import static mealplanner.helper.Util.initCap;

public class Database {
    private static String DB_CREATE_URL;
    private static String DB_APP_URL;
    private static String USER;
    private static String PASSWORD;
    private final Map<String, Integer> ingredientNameToId;
    private int sequenceMealsId;
    private int sequenceIngredientsId;
    private Connection dbConnection;
    public Database() {

        try (InputStream inputStream = Main.class.getResourceAsStream("/db.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);

            DB_CREATE_URL = properties.getProperty("jdbc.createurl");
            DB_APP_URL = properties.getProperty("jdbc.url");
            USER = properties.getProperty("jdbc.username");
            PASSWORD = properties.getProperty("jdbc.password");

            dbConnection = DriverManager.getConnection(DB_CREATE_URL, USER, PASSWORD);
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

            ResultSet resultSet = statement.executeQuery("select from pg_database where datname = 'meals_db'");
            if (!resultSet.isBeforeFirst()) { // check if result set is empty
                statement.executeUpdate("create database meals_db");
            }

            statement.close();
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("Error while creating application database.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createAppTables() {
        try {
            dbConnection = DriverManager.getConnection(DB_APP_URL, USER, PASSWORD);
            Statement statement = dbConnection.createStatement();
            // meals: category (varchar), meal (varchar), meal_id (integer)
            statement.executeUpdate("create table if not exists meals (" +
                    "category varchar(1024) NOT NULL," +
                    "meal varchar(1024) NOT NULL," +
                    "meal_id integer" +
                    ")");
            statement.close();

            statement = dbConnection.createStatement();
            // ingredients: ingredient (varchar), ingredient_id (integer), meal_id (integer)
            statement.executeUpdate("create table if not exists ingredients (" +
                    "ingredient varchar(1024) NOT NULL," +
                    "ingredient_id integer," +
                    "meal_id integer" +
                    ")");
            statement.close();

            statement = dbConnection.createStatement();
            // plan: category (integer), meal (varchar), meal_id (integer), weekday
            // normalized poorly, however, this is the spec
            statement.executeUpdate("create table if not exists plan (" +
                    "category integer NOT NULL," + // facilitates less # of DB calls + easier sort than varchar-based implementation
                    "meal varchar(1024) NOT NULL," +
                    "meal_id integer," +
                    "weekday integer" +
                    ")");
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
            dbConnection = DriverManager.getConnection(DB_APP_URL, USER, PASSWORD);
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
            dbConnection.setAutoCommit(true);
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

    //TODO enhance: 2 use cases exist, one needs sorting, another one doe not
    // for grading, start the app and exit (to initialize teh DB in a way that works w/ tests)
    public List<Meal> getMealsByCategory(MealCategory category, boolean sort) {
        List<Meal> meals = new ArrayList<>();
        try {
            String sql = "select category, meal, meal_id from meals where category = ?";
            if (sort) {
                sql += " order by meal";
            }
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setString(1, category.toString().toLowerCase());

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                MealCategory mealCategory = MealCategory.valueOf(resultSet.getString(1).toUpperCase());
                String mealName = resultSet.getString(2);
                int mealId = resultSet.getInt(3);
                String[] mealIngredients = getIngredientsForMeal(mealId);
                Meal meal = new Meal(mealCategory, mealName, mealIngredients);
                meals.add(meal);
            }
            preparedStatement.close();
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

    private int getIdForMeal(String meal) {
        int mealId = -1;
        try {
            String sql = "select meal_id from meals where meal = ?";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setString(1, meal);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.isBeforeFirst()) { // if result set is empty
                preparedStatement.close();
                throw new SQLException(String.format("meal_id: %s not in DB", meal));
            }
            resultSet.next();
            mealId = resultSet.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error while checking whether DB has the desired meal.");
            e.printStackTrace();
            System.exit(1);
        }
        return mealId;
    }

    public void persistPlanLineItem(MealCategory category, String mealName, Weekday weekday) {
        try {
            // plan: category (int), meal (varchar), meal_id (integer), weekday
            String sql = "insert into plan (category, meal, meal_id, weekday) values (?, ?, ?, ?)";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setInt(1, category.ordinal());
            preparedStatement.setString(2, mealName);
            preparedStatement.setInt(3, getIdForMeal(mealName));
            preparedStatement.setInt(4, weekday.ordinal());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error while writing meal plan line item to DB.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public List<String> getMealPlan() {
        List<String> mealPlan = new ArrayList<>();
        try {
            Statement statement = dbConnection.createStatement();
            // plan: category (int), meal (varchar), meal_id (integer), weekday
            ResultSet resultSet =
                    statement.executeQuery("select category, meal, meal_id, weekday from plan order by weekday, category");

            while (resultSet.next()) {
                mealPlan.add(String.format("%s: %s", initCap(MealCategory.values()[resultSet.getInt(1)].toString()),
                        resultSet.getString(2)));
            }
            statement.close();
        } catch (SQLException e) {
            System.out.println("Error while reading meal plan from the database.");
            e.printStackTrace();
            System.exit(1);
        }
        return mealPlan;
    }

    public void clearPlan() {
        try {
            Statement statement = dbConnection.createStatement();
            statement.executeUpdate("delete from plan");
        } catch (SQLException e) {
            System.out.println("Error while removing meal plan data from the database.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
