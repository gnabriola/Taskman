package com.taskman.shared;

/**
 * Base class for requests that are malformed in a way Bean Validation cannot check,
 * e.g. a syntax error in a query string. The web layer maps it to 400.
 */
public abstract class BadRequestException extends RuntimeException {

	protected BadRequestException(String message) {
		super(message);
	}

}
