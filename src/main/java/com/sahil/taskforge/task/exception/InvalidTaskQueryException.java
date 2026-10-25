package com.sahil.taskforge.task.exception;

public class InvalidTaskQueryException extends RuntimeException {
    private final String field;

    public InvalidTaskQueryException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
