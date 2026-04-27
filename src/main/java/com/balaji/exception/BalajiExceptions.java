package com.balaji.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class BalajiExceptions {

    private BalajiExceptions() {}

    // ── Resource not found (404) ──────────────────────────────────────────
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundEx extends RuntimeException {
        private final String errorCode = "RESOURCE_NOT_FOUND";
        public ResourceNotFoundEx(String resource, Object id) {
            super(resource + " not found with id: " + id);
        }
        public ResourceNotFoundEx(String message) { super(message); }
        public String getErrorCode() { return errorCode; }
    }

    // ── Business rule violation (400) ─────────────────────────────────────
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BusinessEx extends RuntimeException {
        private final String errorCode = "BUSINESS_ERROR";
        public BusinessEx(String message) { super(message); }
        public String getErrorCode() { return errorCode; }
    }

    // ── Payment failure (402) ─────────────────────────────────────────────
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public static class PaymentEx extends RuntimeException {
        private final String errorCode = "PAYMENT_ERROR";
        public PaymentEx(String message) { super(message); }
        public String getErrorCode() { return errorCode; }
    }

    // ── File upload error (400) ───────────────────────────────────────────
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class FileUploadEx extends RuntimeException {
        private final String errorCode = "FILE_UPLOAD_ERROR";
        public FileUploadEx(String message) { super(message); }
        public String getErrorCode() { return errorCode; }
    }
}
