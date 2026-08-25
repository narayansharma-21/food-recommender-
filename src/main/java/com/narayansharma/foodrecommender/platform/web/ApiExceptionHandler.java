package com.narayansharma.foodrecommender.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private final Clock clock;

	public ApiExceptionHandler() {
		this(Clock.systemUTC());
	}

	ApiExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
		return response(exception.status(), exception.code(), exception.getMessage(), request, List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidArgument(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<ApiErrorResponse.FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();

		return response(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				"The request contains invalid fields.",
				request,
				violations);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		List<ApiErrorResponse.FieldViolation> violations = exception.getConstraintViolations().stream()
				.map(violation -> new ApiErrorResponse.FieldViolation(
						violation.getPropertyPath().toString(),
						violation.getMessage()))
				.toList();

		return response(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_FAILED",
				"The request contains invalid values.",
				request,
				violations);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
			HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"The request body could not be read.",
				request,
				List.of());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiErrorResponse> handleMissingResource(
			NoResourceFoundException exception,
			HttpServletRequest request) {
		return response(
				HttpStatus.NOT_FOUND,
				"NOT_FOUND",
				"The requested resource was not found.",
				request,
				List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		log.error("Unhandled request failure", exception);
		return response(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"The request could not be completed.",
				request,
				List.of());
	}

	private ResponseEntity<ApiErrorResponse> response(
			HttpStatus status,
			String code,
			String message,
			HttpServletRequest request,
			List<ApiErrorResponse.FieldViolation> violations) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(clock),
				status.value(),
				code,
				message,
				request.getRequestURI(),
				MDC.get(RequestIdFilter.MDC_KEY),
				violations);
		return ResponseEntity.status(status).body(body);
	}
}
