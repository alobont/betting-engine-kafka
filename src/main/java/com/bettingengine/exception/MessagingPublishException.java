package com.bettingengine.exception;

public class MessagingPublishException extends RuntimeException {

    public MessagingPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessagingPublishException(String message) {
        super(message);
    }
}
