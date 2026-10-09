package com.taskman.task;

import com.taskman.shared.UnprocessableRequestException;

class QuickAddRejectedException extends UnprocessableRequestException {

	private QuickAddRejectedException(String message) {
		super(message);
	}

	static QuickAddRejectedException unknownProject(String name) {
		return new QuickAddRejectedException("No project named '" + name + "'");
	}

	static QuickAddRejectedException emptyTitle() {
		return new QuickAddRejectedException("The task needs a title, not just a date, project or priority");
	}

}
