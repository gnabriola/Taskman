package com.taskman.task;

import java.util.UUID;

import com.taskman.shared.NotFoundException;

class TaskNotFoundException extends NotFoundException {

	TaskNotFoundException(UUID taskId) {
		super("Task " + taskId + " does not exist");
	}

}
