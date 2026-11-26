package com.bugtracker.exception;

/** Thrown when a ticket is moved to a status that is not allowed from its current status. */
public class InvalidStatusChangeException extends IllegalStateException {
    public InvalidStatusChangeException(String message) { super(message); }
}
