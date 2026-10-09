package com.taskman.task;

import com.taskman.shared.UnprocessableRequestException;

/** A syntactically valid filter that names a project or label that does not exist. */
class FilterRejectedException extends UnprocessableRequestException {

	private FilterRejectedException(String message) {
		super(message);
	}

	static FilterRejectedException unknownProject(String name) {
		return new FilterRejectedException("No project named '" + name + "'");
	}

	static FilterRejectedException unknownLabel(String name) {
		return new FilterRejectedException("No label named '" + name + "'");
	}

}
