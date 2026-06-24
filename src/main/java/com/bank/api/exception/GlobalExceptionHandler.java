package com.bank.api.exception;

import com.bank.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ACCOUNT NOT FOUND → 404
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex) {

        ErrorResponse error = new ErrorResponse(
                "ACCOUNT_NOT_FOUND",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * VALIDATION ERROR (Path / Query / Header) → 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        ErrorResponse error = new ErrorResponse(
                "INVALID_REQUEST",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * REQUEST BODY VALIDATION ERROR → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        ErrorResponse error = new ErrorResponse(
                "INVALID_REQUEST",
                message
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * TRANSFER FAILURE / ROLLBACK → 409
     */
    @ExceptionHandler(TransferFailedException.class)
    public ResponseEntity<ErrorResponse> handleTransferFailed(
            TransferFailedException ex) {

        ErrorResponse error = new ErrorResponse(
                "TRANSFER_FAILED",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * AUTH / SECURITY / BUSINESS ERRORS → 400
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(
            RuntimeException ex) {

        ErrorResponse error = new ErrorResponse(
                "BUSINESS_ERROR",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * FALLBACK → 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex) {

        ex.printStackTrace();

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
