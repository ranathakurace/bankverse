package com.bank.api.exception;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.UNAUTHORIZED.value(), "AUTHENTICATION_FAILED", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST",
                "Request validation failed.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED", "HTTP method is not supported for this endpoint.", request.getRequestURI());
        return new ResponseEntity<>(error, headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(EmployeeAdministrationException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeAdministration(EmployeeAdministrationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "IDENTITY_ADMINISTRATION_ERROR", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomerPortalException.class)
    public ResponseEntity<ErrorResponse> handleCustomerPortal(CustomerPortalException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "CUSTOMER_PORTAL_ERROR", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "You do not have permission to perform this operation.", request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * ==========================================================
     * Invalid Customer Request
     * HTTP 400 - Bad Request
     * ==========================================================
     */
    @ExceptionHandler(InvalidCustomerException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCustomer(
            InvalidCustomerException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_CUSTOMER");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidKycException.class)
    public ResponseEntity<ErrorResponse> handleInvalidKyc(
            InvalidKycException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_KYC");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccount(
            InvalidAccountException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_ACCOUNT");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccountWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleAccountWorkflow(
            AccountWorkflowException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("ACCOUNT_INVALID_STATE");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(
            InvalidTransactionException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_TRANSACTION");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleTransactionWorkflow(
            TransactionWorkflowException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("TRANSACTION_INVALID_STATE");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidCardException.class, InvalidLoanException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCardOrLoan(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value()); error.setErrorCode("INVALID_CARD_OR_LOAN");
        error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({CardWorkflowException.class, LoanWorkflowException.class})
    public ResponseEntity<ErrorResponse> handleCardOrLoanWorkflow(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value()); error.setErrorCode("CARD_OR_LOAN_INVALID_STATE");
        error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_PAYMENT"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentWorkflowException.class)
    public ResponseEntity<ErrorResponse> handlePaymentWorkflow(PaymentWorkflowException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("PAYMENT_INVALID_STATE"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidControlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidControl(InvalidControlException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_CONTROL"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ControlWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleControlWorkflow(ControlWorkflowException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("CONTROL_INVALID_STATE"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidEventException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEvent(InvalidEventException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_EVENT"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EventWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleEventWorkflow(EventWorkflowException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(); error.setTimestamp(LocalDateTime.now()); error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("EVENT_INVALID_STATE"); error.setMessage(ex.getMessage()); error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(KycWorkflowException.class)
    public ResponseEntity<ErrorResponse> handleKycWorkflow(
            KycWorkflowException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("KYC_INVALID_STATE");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * ==========================================================
     * Duplicate Customer
     * HTTP 409 - Conflict
     * ==========================================================
     */
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCustomerAlreadyExists(
            CustomerAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("CUSTOMER_ALREADY_EXISTS");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * ==========================================================
     * Customer Not Found
     * HTTP 404 - Not Found
     * ==========================================================
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(
            CustomerNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setErrorCode("CUSTOMER_NOT_FOUND");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * ==========================================================
     * Account Not Found
     * HTTP 404 - Not Found
     * ==========================================================
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setErrorCode("ACCOUNT_NOT_FOUND");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * ==========================================================
     * Transfer Failed
     * HTTP 409 - Conflict
     * ==========================================================
     */
    @ExceptionHandler(TransferFailedException.class)
    public ResponseEntity<ErrorResponse> handleTransferFailed(
            TransferFailedException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("TRANSFER_FAILED");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * ==========================================================
     * Unexpected Exception
     * HTTP 500 - Internal Server Error
     * ==========================================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {

        LOGGER.error("Unhandled request failure for {}", request.getRequestURI(), ex);

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setErrorCode("INTERNAL_SERVER_ERROR");
        error.setMessage("Something went wrong. Please contact support.");
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * ==========================================================
     * Malformed JSON Request
     * HTTP 400 - Bad Request
     * ==========================================================
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse();

        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setErrorCode("INVALID_REQUEST");
        error.setMessage("Malformed JSON request.");
        error.setPath(request.getRequestURI());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
