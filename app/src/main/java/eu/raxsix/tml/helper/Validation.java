package eu.raxsix.tml.helper;


public class Validation {

    //private static final String USERNAME_PATTERN = "^[A-z0-9_-|]{2,15}$";

    public static boolean isFormFieldNameValid(String field) {

        return field.length() >= 2 && field.length() <= 8;
    }

    public static boolean isFormGroupNameValid(String field) {

        return field.length() >= 2 && field.length() < 25;
    }
}
