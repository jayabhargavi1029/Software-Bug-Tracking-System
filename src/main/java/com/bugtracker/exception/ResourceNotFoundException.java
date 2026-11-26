package com.bugtracker.exception;

/** Thrown when a ticket or user does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
