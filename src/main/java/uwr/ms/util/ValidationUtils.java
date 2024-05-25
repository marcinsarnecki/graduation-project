package uwr.ms.util;
import lombok.experimental.UtilityClass;
import uwr.ms.constant.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public final class ValidationUtils {

    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < 8) {
            errors.add(Message.PASSWORD_MIN_LENGTH.toString());
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add(Message.PASSWORD_UPPERCASE.toString());
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add(Message.PASSWORD_LOWERCASE.toString());
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            errors.add(Message.PASSWORD_DIGIT.toString());
        }
        return errors;
    }

    public List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();
        if (email == null || !Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matcher(email).find()) {
            errors.add(Message.EMAIL_INVALID.toString());
        }
        return errors;
    }
}