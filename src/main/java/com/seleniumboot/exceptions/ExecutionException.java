package com.seleniumboot.exceptions;

public class ExecutionException extends RuntimeException {
    public ExecutionException(String message) { super(message); }
    public ExecutionException(String message, Throwable cause) { super(message, cause); }
}
