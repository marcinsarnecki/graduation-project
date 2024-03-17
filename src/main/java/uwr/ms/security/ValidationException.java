package uwr.ms.security;

import lombok.Getter;

import java.util.List;

public class ValidationException extends RuntimeException {
    @Getter
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation error occurred");
        this.errors = errors;
    }
}