package com.knowledge.api.exception;

public class CustomException extends RuntimeException {
    
    private final int statusCode; 
    private final String errorMessage;

    /**
     * Constructs a new NonAuthoritativeInfoException with the specified status code and error message.
     *
     * @param statusCode   the HTTP status code to be sent to the client.
     * @param errorMessage the error message to be sent to the client.
     */
    public CustomException(int statusCode, String errorMessage) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a new NonAuthoritativeInfoException with the specified status code, error message, and cause.
     *
     * @param statusCode   the HTTP status code to be sent to the client.
     * @param errorMessage the error message to be sent to the client.
     * @param cause        the cause of the exception.
     */
    public CustomException(int statusCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the status code associated with this exception.
     *
     * @return the status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the error message associated with this exception.
     *
     * @return the error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

}
