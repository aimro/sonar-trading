package com.sonartrading.challenge.exception;

public class InitializationException extends RuntimeException {

	private static final long serialVersionUID = -8952153129142211694L;

	public InitializationException(String message) {
		super(message);
	}

	public InitializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
