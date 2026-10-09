package com.taskman.label;

import java.util.UUID;

import com.taskman.shared.NotFoundException;

public class LabelNotFoundException extends NotFoundException {

	public LabelNotFoundException(UUID labelId) {
		super("Label " + labelId + " does not exist");
	}

}
