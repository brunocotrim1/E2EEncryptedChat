package org.psd.server.ServerPSD.exceptions;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Wrong username or password");
    }
}
