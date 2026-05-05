package org.example.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already in use"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),
    CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "Context not found"),
    FREE_PLAN_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "Free plan allows up to 3 projects. Upgrade to Pro for unlimited projects."),
    PAID_PLAN_REQUIRED(HttpStatus.FORBIDDEN, "This feature requires a paid plan"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
