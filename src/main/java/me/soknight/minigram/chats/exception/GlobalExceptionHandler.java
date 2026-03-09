package me.soknight.minigram.chats.exception;

import jakarta.validation.ConstraintViolationException;
import me.soknight.minigram.chats.model.ErrorModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GenericErrorException.class)
    public ResponseEntity<ErrorModel> handleGenericError(GenericErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.constructModel());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorModel> handleValidationError(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        GenericErrorException error = firstError == null
                ? new GenericErrorException(HttpStatus.BAD_REQUEST, "incorrect_field_value", "Request body validation failed")
                : GenericErrorException.fromFieldError(firstError);
        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorModel> handleConstraintViolation(ConstraintViolationException ex) {
        GenericErrorException error = ex.getConstraintViolations().stream().findFirst()
                .map(GenericErrorException::fromConstraintViolation)
                .orElseGet(() -> new GenericErrorException(HttpStatus.BAD_REQUEST, "incorrect_field_value", "Request validation failed"));
        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorModel> handleUnexpectedError(Exception ex) {
        GenericErrorException error = new GenericErrorException(ex);
        return ResponseEntity.status(error.getStatusCode()).body(error.constructModel());
    }

}
