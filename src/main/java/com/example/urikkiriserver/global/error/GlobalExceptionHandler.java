package com.example.urikkiriserver.global.error;

import com.example.urikkiriserver.global.error.exception.ErrorResponse;
import com.example.urikkiriserver.global.error.exception.UrikkiriException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UrikkiriException.class)
    public ResponseEntity<ErrorResponse> customExceptionHandling(UrikkiriException e) {
        return new ResponseEntity<>(
            ErrorResponse.of(e.getErrorCode()),
            e.getErrorCode().getStatus()
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBindException(BindException e) {
        Map<String, String> errorList = new HashMap<>();
        for (FieldError fieldError : e.getFieldErrors()) {
            errorList.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(errorList, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errorList = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorList.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(errorList, HttpStatus.BAD_REQUEST);
    }
}
