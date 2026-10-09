package com.taskman.shared;

/**
 * Base class for requests that are well-formed but cannot be carried out, e.g. they
 * refer to something that does not exist by name. The web layer maps it to 422.
 */
public abstract class UnprocessableRequestException extends RuntimeException {

	protected UnprocessableRequestException(String message) {
		super(message);
	}

}
