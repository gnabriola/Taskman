package com.taskman.user;

import com.taskman.shared.ConflictException;

class EmailTakenException extends ConflictException {

	EmailTakenException() {
		super("An account with this email already exists");
	}

}
