package com.agentcommerce.domain.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=InventoryController.class)
class InventoryApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) ProblemDetail validation(MethodArgumentNotValidException e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Request validation failed");Map<String,String> errors=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->errors.putIfAbsent(x.getField(),x.getDefaultMessage()));p.setProperty("errors",errors);return p;}
 @ExceptionHandler(IllegalArgumentException.class) ProblemDetail bad(IllegalArgumentException e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage());}
 @ExceptionHandler(InventoryAccessDeniedException.class) ProblemDetail forbidden(InventoryAccessDeniedException e){return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,e.getMessage());}
 @ExceptionHandler(InventoryConflictException.class) ProblemDetail conflict(InventoryConflictException e){return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e.getMessage());}
}