package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.service.ConflictException;
import io.github.ahmedsh10.shiftwise.service.NotFoundException;
import io.github.ahmedsh10.shiftwise.service.RuleViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/** Turns exceptions into RFC 7807 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    /** 422: the request is well formed, but it breaks a scheduling rule. */
    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail ruleViolation(RuleViolationException e) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Scheduling rule violated", e.getMessage());
        List<Map<String, String>> violations = e.getViolations().stream()
                .map(v -> Map.of("rule", v.rule().name(), "message", v.message()))
                .toList();
        pd.setProperty("violations", violations);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Invalid request", "Some fields are invalid");
        pd.setProperty("fields", e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
                .toList());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail unauthenticated(AuthenticationException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Wrong email or password");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
