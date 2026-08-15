package com.floatops.shared.exception;

public class ErrorResponse {
    public String title;
    public int status;

    public ErrorResponse(String title, int status) {
        this.title = title;
        this.status = status;
    }
}