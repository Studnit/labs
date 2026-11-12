package com.smartlib.web;

/** Expected, user-correctable error (shown as a flash message / 400 JSON). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
