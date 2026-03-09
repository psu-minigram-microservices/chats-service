package me.soknight.minigram.chats.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import me.soknight.minigram.chats.model.ErrorModel;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.validation.FieldError;

@Getter
public class GenericErrorException extends Exception {

    protected final @NonNull HttpStatusCode statusCode;
    protected final @NonNull String errorCode;
    protected final @Nullable String errorMessage;

    public GenericErrorException(@Nullable Throwable cause) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected_error", "An unexpected error was occured! Try again later.", cause);
    }

    public GenericErrorException(@NonNull String errorCode, @Nullable String errorMessage) {
        this(HttpStatus.BAD_REQUEST, errorCode, errorMessage, null);
    }

    public GenericErrorException(
            @NonNull HttpStatusCode statusCode,
            @NonNull String errorCode,
            @Nullable String errorMessage
    ) {
        this(statusCode, errorCode, errorMessage, null);
    }

    public GenericErrorException(
            @NonNull HttpStatusCode statusCode,
            @NonNull String errorCode,
            @Nullable String errorMessage,
            @Nullable Throwable cause
    ) {
        super(errorMessage, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static @NonNull GenericErrorException fromFieldError(@NonNull FieldError fieldError) {
        return fromConstraintViolation(fieldError.getField(), fieldError.getDefaultMessage());
    }

    public static @NonNull GenericErrorException fromConstraintViolation(
            @Nullable String fieldName,
            @Nullable String message
    ) {
        return new GenericErrorException(
                HttpStatus.BAD_REQUEST,
                "incorrect_field_value",
                "%s: %s".formatted(fieldName, message)
        );
    }

    public static @NonNull GenericErrorException fromConstraintViolation(@NonNull ConstraintViolation<?> violation) {
        return fromConstraintViolation(violation.getPropertyPath().toString(), violation.getMessage());
    }

    public static <T> void throwIfViolationsFound(
            @NonNull Validator validator,
            @NonNull T validationTarget
    ) throws GenericErrorException {
        var violations = validator.validate(validationTarget);
        if (violations.isEmpty()) return;

        var first = violations.stream().limit(1).findFirst().orElse(null);
        if (first == null) return;

        throw GenericErrorException.fromConstraintViolation(first);
    }

    public @NonNull ErrorModel constructModel() {
        return new ErrorModel(errorCode, errorMessage);
    }

}