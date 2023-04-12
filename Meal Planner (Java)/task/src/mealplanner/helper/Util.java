package mealplanner.helper;

public class Util {
    public static String initCap(String text) {
        return text.substring(0, 1).toUpperCase() + text.toString().substring(1).toLowerCase();
    }
}
