package com.taskman.label;

import com.taskman.shared.ConflictException;

class LabelNameTakenException extends ConflictException {

	LabelNameTakenException(String name) {
		super("A label named '" + name + "' already exists");
	}

}
