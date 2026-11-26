package com.bugtracker.exception;

/** Thrown when a username or e-mail is already registered. */
public class DuplicateUserException extends RuntimeException {
    private final String field;

    public DuplicateUserException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The form field that caused the problem ("username" or "email"). */
    public String getField() { return field; }
}
