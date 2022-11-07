package org.psd.server.ServerPSD.exceptions;

public class UserNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String username) {
        super(String.format("User with username %s not found", username));
    }
}
