package com.melardev.spring.crud.responses;

public class ErrorResponse extends AppResponse {

    public ErrorResponse(String errorMessage) {
        super(false);
        addFullMessage(errorMessage);
    }

}
