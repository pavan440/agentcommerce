package com.agentcommerce.domain.identity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
    CustomerProfileController.class,
    CustomerAddressController.class,
    UserDeviceTokenController.class,
    UserEmergencyContactController.class,
    UserConsentController.class
})
class UserApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UserResourceNotFoundException.class)
    ProblemDetail notFound(UserResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({UserConflictException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception) {
        String detail = exception instanceof UserConflictException
            ? exception.getMessage()
            : "The request conflicts with an existing user resource";
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }
}
