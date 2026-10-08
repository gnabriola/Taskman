package com.taskman.shared;

/**
 * Base class for "this resource does not exist" errors thrown by any module.
 * The web layer maps it to 404 without knowing which module threw it.
 */
public abstract class NotFoundException extends RuntimeException {

	protected NotFoundException(String message) {
		super(message);
	}

}
