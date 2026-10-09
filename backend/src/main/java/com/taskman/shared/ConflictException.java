package com.taskman.shared;

/**
 * Base class for requests that clash with existing data, e.g. a name that is already
 * taken. The web layer maps it to 409.
 */
public abstract class ConflictException extends RuntimeException {

	protected ConflictException(String message) {
		super(message);
	}

}
