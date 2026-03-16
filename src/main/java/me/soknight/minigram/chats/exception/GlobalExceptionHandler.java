package me.soknight.minigram.chats.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import me.soknight.minigram.chats.model.dto.ErrorDto;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericErrorException.class)
    public @NonNull ResponseEntity<ErrorDto> handleGenericError(@NonNull GenericErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.constructModel());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public @NonNull ResponseEntity<ErrorDto> handleValidationError(@NonNull MethodArgumentNotValidException ex) {
        var firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        var error = firstError == null
                ? new GenericErrorException(HttpStatus.BAD_REQUEST, "incorrect_field_value", "Request body validation failed")
                : GenericErrorException.fromFieldError(firstError);

        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public @NonNull ResponseEntity<ErrorDto> handleConstraintViolation(@NonNull ConstraintViolationException ex) {
        var error = ex.getConstraintViolations().stream().findFirst()
                .map(GenericErrorException::fromConstraintViolation)
                .orElseGet(() -> new GenericErrorException(HttpStatus.BAD_REQUEST, "incorrect_field_value", "Request validation failed"));

        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public @NonNull ResponseEntity<ErrorDto> handleNoResourceFoundException(@NonNull NoResourceFoundException ex) {
        var resourcePath = ex.getResourcePath();
        log.debug("Requested unknown resource: '{}'", resourcePath);

        var error = new ErrorDto("resource_not_found", "Resource '%s' not found".formatted(resourcePath));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public @NonNull ResponseEntity<ErrorDto> handleUnexpectedError(@NonNull Exception ex) {
        log.error("Unhandled internal server error!", ex);

        var error = new GenericErrorException(ex);
        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

}
