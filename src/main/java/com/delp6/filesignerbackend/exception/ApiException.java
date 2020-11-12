package com.delp6.filesignerbackend.exception;

public class ApiException extends RuntimeException {


    private static final long serialVersionUID = 5033131654506960912L;

    public ApiException(String message) {
        super(message);
    }
}
