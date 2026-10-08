package com.taskman.shared.web;

import java.util.List;

import com.taskman.shared.NotFoundException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem details. Spring MVC's own exceptions are
 * handled by {@link ResponseEntityExceptionHandler}; this class adds field errors
 * to validation failures and maps the application's own exceptions.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler
	ProblemDetail handleNotFound(NotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Resource not found");
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		ProblemDetail problem = ex.getBody();
		problem.setDetail("Request validation failed");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	record FieldError(String field, String message) {
	}

}
